package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;

public class MindMapFileSearchPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String SCAN_ROOT = "E:\\yixiaozi";
	
	private final JTextField searchField = new JTextField();
	private final DefaultListModel<FileResult> listModel = new DefaultListModel<FileResult>();
	private final JList<FileResult> resultList = new JList<FileResult>(listModel);
	private final JLabel statusLabel = new JLabel("加载中...");
	
	private final List<File> allMindMapFiles = new ArrayList<File>();
	
	public static class FileResult {
		final File file;
		final long lastModified;
		
		FileResult(File file, long lastModified) {
			this.file = file;
			this.lastModified = lastModified;
		}
	}
	
	public MindMapFileSearchPanel() {
		super(new BorderLayout(4, 4));
		
		searchField.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.GRAY),
			BorderFactory.createEmptyBorder(4, 8, 4, 8)
		));
		searchField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		searchField.setToolTipText("输入关键词搜索文件名，清空后显示最近修改的文件");
		
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					searchField.setText("");
					searchField.requestFocus();
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					performSearch();
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				performSearch();
			}
		});
		
		JButton clearButton = new JButton("清空");
		clearButton.setPreferredSize(new Dimension(60, 30));
		clearButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				searchField.setText("");
				searchField.requestFocus();
				performSearch();
			}
		});
		
		statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		statusLabel.setForeground(Color.GRAY);
		statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		
		resultList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			private final Border lineBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230));
			private final Border padding = BorderFactory.createEmptyBorder(2, 4, 2, 4);
			private final SimpleDateFormat thisYearFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
			private final SimpleDateFormat otherYearFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
			private final int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
			
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof FileResult) {
					FileResult result = (FileResult) value;
					String fileName = result.file.getName();
					if (fileName.toLowerCase().endsWith(".mm")) {
						fileName = fileName.substring(0, fileName.length() - 3);
					}
					
					String modifiedTime;
					java.util.Calendar cal = java.util.Calendar.getInstance();
					cal.setTime(new Date(result.lastModified));
					if (cal.get(java.util.Calendar.YEAR) == currentYear) {
						modifiedTime = thisYearFormat.format(new Date(result.lastModified));
					} else {
						modifiedTime = otherYearFormat.format(new Date(result.lastModified));
					}
					
					String html = "<html><table width='100%' style='table-layout:fixed'><tr>" +
						"<td align='left' style='white-space:nowrap;overflow:hidden;text-overflow:ellipsis'>" +
						"<span style='color:#3366cc;font-weight:bold'>" + escapeHtml(fileName) + "</span>" +
						"</td>" +
						"<td align='right' style='white-space:nowrap;font-size:10px;color:#999999;padding-left:8px'>" + modifiedTime + "</td>" +
						"</tr></table></html>";
					
					setText(html);
					setBorder(BorderFactory.createCompoundBorder(lineBorder, padding));
					
					if (isSelected) {
						setBackground(new Color(100, 149, 237));
						setForeground(Color.WHITE);
					} else {
						setBackground(Color.WHITE);
						setForeground(Color.BLACK);
					}
				}
				return this;
			}
		});
		resultList.setSelectionBackground(new Color(100, 149, 237));
		resultList.setSelectionForeground(Color.WHITE);
		resultList.setBackground(Color.WHITE);
		resultList.setBorder(null);
		resultList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		resultList.setFixedCellHeight(28);
		
		resultList.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					openSelectedResult();
				}
			}
		});
		
		resultList.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getClickCount() >= 1) {
					openSelectedResult();
				}
			}
		});
		
		JPanel searchPanel = new JPanel(new BorderLayout(4, 4));
		searchPanel.add(searchField, BorderLayout.CENTER);
		searchPanel.add(clearButton, BorderLayout.EAST);
		searchPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		
		JScrollPane scrollPane = new JScrollPane(resultList);
		scrollPane.setBorder(null);
		
		add(searchPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		add(statusLabel, BorderLayout.SOUTH);
		
		loadMindMapFiles();
	}
	
	private String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
			.replace("\"", "&quot;").replace("'", "&#39;");
	}
	
	private void loadMindMapFiles() {
		new Thread(new Runnable() {
			public void run() {
				allMindMapFiles.clear();
				File root = new File(SCAN_ROOT);
				if (root.exists()) {
					collectMindMapFiles(root);
				}
				
				Collections.sort(allMindMapFiles, new Comparator<File>() {
					public int compare(File a, File b) {
						return Long.compare(b.lastModified(), a.lastModified());
					}
				});
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						performSearch();
						statusLabel.setText("就绪 (共 " + allMindMapFiles.size() + " 个文件)");
					}
				});
			}
		}).start();
	}
	
	private void collectMindMapFiles(File dir) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory() && !file.getName().startsWith(".")) {
				collectMindMapFiles(file);
			} else if (file.getName().toLowerCase().endsWith(".mm")) {
				allMindMapFiles.add(file);
			}
		}
	}
	
	private void performSearch() {
		final String query = searchField.getText().trim().toLowerCase();
		
		listModel.clear();
		
		List<FileResult> results = new ArrayList<FileResult>();
		
		if (query.isEmpty()) {
			// 显示所有文件，按修改时间排序
			for (File file : allMindMapFiles) {
				results.add(new FileResult(file, file.lastModified()));
			}
		} else {
			// 搜索匹配文件名的文件
			for (File file : allMindMapFiles) {
				String fileName = file.getName().toLowerCase();
				if (fileName.contains(query)) {
					results.add(new FileResult(file, file.lastModified()));
				}
			}
			
			// 搜索结果按修改时间排序
			Collections.sort(results, new Comparator<FileResult>() {
				public int compare(FileResult a, FileResult b) {
					return Long.compare(b.lastModified, a.lastModified);
				}
			});
		}
		
		for (FileResult result : results) {
			listModel.addElement(result);
		}
		
		// 更新状态标签
		if (query.isEmpty()) {
			statusLabel.setText("就绪 (共 " + allMindMapFiles.size() + " 个文件)");
		} else {
			statusLabel.setText("找到 " + results.size() + " 个文件");
		}
	}
	
	private void openSelectedResult() {
		FileResult result = resultList.getSelectedValue();
		if (result == null) {
			return;
		}
		
		try {
			final URL mapUrl = result.file.toURI().toURL();
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						Controller.getCurrentModeController().getMapController().newMap(mapUrl);
					} catch (Exception e) {
						LogUtils.warn("Failed to open file: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			LogUtils.warn("Failed to open file: " + e.getMessage());
		}
	}
}
