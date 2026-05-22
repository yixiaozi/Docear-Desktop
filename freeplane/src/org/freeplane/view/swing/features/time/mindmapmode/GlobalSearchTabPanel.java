package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

public class GlobalSearchTabPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String SCAN_ROOT = "E:\\yixiaozi";
	private static final int MAX_RESULTS = 100;
	private static final int PREVIEW_LENGTH = 100;
	
	private final JTextField searchField = new JTextField();
	private final DefaultListModel<SearchResult> listModel = new DefaultListModel<SearchResult>();
	private final JList<SearchResult> resultList = new JList<SearchResult>(listModel);
	private final JLabel statusLabel = new JLabel("加载中...");
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private volatile boolean isSearching = false;
	private volatile String lastQuery = "";
	
	private final Map<String, Long> fileLastModifiedCache = new HashMap<String, Long>();
	private final Map<String, List<NodeInfo>> fileNodesCache = new HashMap<String, List<NodeInfo>>();
	
	private static class NodeInfo {
		String text;
		String note;
		String id;
		int depth;
		
		NodeInfo(String text, String note, String id, int depth) {
			this.text = text;
			this.note = note;
			this.id = id;
			this.depth = depth;
		}
	}
	
	public static class SearchResult {
		final File file;
		final String nodeText;
		final String preview;
		final String nodeId;
		final int depth;
		
		SearchResult(File file, String nodeText, String preview, String nodeId, int depth) {
			this.file = file;
			this.nodeText = nodeText;
			this.preview = preview;
			this.nodeId = nodeId;
			this.depth = depth;
		}
	}
	
	public GlobalSearchTabPanel() {
		super(new BorderLayout(4, 4));
		
		searchField.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.GRAY),
			BorderFactory.createEmptyBorder(4, 8, 4, 8)
		));
		searchField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
		searchField.setToolTipText("输入关键词后点击搜索按钮");
		
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					searchField.setText("");
					listModel.clear();
					searchField.requestFocus();
				}
			}
		});
		
		JButton searchButton = new JButton("搜索");
		searchButton.setPreferredSize(new Dimension(60, 30));
		searchButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				performSearch();
			}
		});
		
		statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		statusLabel.setForeground(Color.GRAY);
		statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		
		resultList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			private final Border lineBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230));
			private final Border padding = BorderFactory.createEmptyBorder(4, 8, 4, 8);
			
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof SearchResult) {
					SearchResult result = (SearchResult) value;
					String fileName = result.file.getName();
					if (fileName.toLowerCase().endsWith(".mm")) {
						fileName = fileName.substring(0, fileName.length() - 3);
					}
					String text = highlightKeywords(result.nodeText, lastQuery);
					
					if (text.length() > 150) {
						text = text.substring(0, 150) + "...";
					}
					
					String html = "<html><span style='color:#3366cc;font-weight:bold'>[" + escapeHtml(fileName) + "]</span> " + text + "</html>";
					
					setText(html);
					setBorder(BorderFactory.createCompoundBorder(lineBorder, padding));
					
					if (isSelected) {
						setBackground(new Color(66, 133, 244));
						setForeground(Color.WHITE);
					} else {
						setBackground(Color.WHITE);
						setForeground(Color.BLACK);
					}
				}
				return this;
			}
		});
		resultList.setSelectionBackground(new Color(66, 133, 244));
		resultList.setSelectionForeground(Color.WHITE);
		resultList.setBackground(Color.WHITE);
		resultList.setBorder(null);
		resultList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		resultList.setFixedCellHeight(50);
		
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
		searchPanel.add(searchButton, BorderLayout.EAST);
		searchPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		
		JScrollPane scrollPane = new JScrollPane(resultList);
		scrollPane.setBorder(null);
		
		add(searchPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		add(statusLabel, BorderLayout.SOUTH);
		
		loadCacheIfNeeded();
	}
	
	private String highlightKeywords(String text, String query) {
		if (text == null) {
			return "";
		}
		if (query == null || query.isEmpty()) {
			return escapeHtml(text);
		}
		
		String result = escapeHtml(text);
		
		for (String keyword : query.split("\\s+")) {
			if (!keyword.isEmpty()) {
				String escapedKeyword = escapeHtml(keyword);
				try {
					result = result.replaceAll("(?i)" + Pattern.quote(escapedKeyword),
						"<font color='red'><b>$0</b></font>");
				} catch (Exception e) {
					LogUtils.warn("Regex error in highlightKeywords: " + e.getMessage());
				}
			}
		}
		return result;
	}
	
	private String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
			.replace("\"", "&quot;").replace("'", "&#39;");
	}
	
	private void performSearch() {
		lastQuery = searchField.getText().trim();
		
		if (lastQuery.isEmpty()) {
			listModel.clear();
			return;
		}
		
		if (isSearching) {
			return;
		}
		
		isSearching = true;
		statusLabel.setText("搜索中...");
		
		executor.submit(new Runnable() {
			public void run() {
				try {
					List<String> keywords = new ArrayList<String>();
					for (String word : lastQuery.split("\\s+")) {
						if (!word.isEmpty()) {
							keywords.add(word.toLowerCase());
						}
					}
					
					if (keywords.isEmpty()) {
						return;
					}
					
					List<SearchResult> results = new ArrayList<SearchResult>();
					Set<String> checkedFiles = new HashSet<String>();
					Set<String> seenResults = new HashSet<String>();
					
					scanDirectory(new File(SCAN_ROOT), keywords, results, checkedFiles, seenResults);
					
					final List<SearchResult> finalResults = results;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							listModel.clear();
							for (SearchResult result : finalResults) {
								listModel.addElement(result);
							}
							statusLabel.setText(getStatusText());
						}
					});
				} finally {
					isSearching = false;
				}
			}
		});
	}
	
	private String getStatusText() {
		int fileCount = fileNodesCache.size();
		int nodeCount = 0;
		for (List<NodeInfo> nodes : fileNodesCache.values()) {
			nodeCount += nodes.size();
		}
		return "已加载 " + fileCount + " 个导图，共 " + nodeCount + " 个节点";
	}
	
	private void scanDirectory(File dir, List<String> keywords, List<SearchResult> results, 
			Set<String> checkedFiles, Set<String> seenResults) {
		if (results.size() >= MAX_RESULTS) {
			return;
		}
		
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		
		for (File file : files) {
			if (results.size() >= MAX_RESULTS) {
				return;
			}
			
			if (file.isDirectory()) {
				if (!file.getName().startsWith(".")) {
					scanDirectory(file, keywords, results, checkedFiles, seenResults);
				}
			} else if (file.getName().toLowerCase().endsWith(".mm")) {
				String filePath = file.getAbsolutePath();
				if (checkedFiles.contains(filePath)) {
					continue;
				}
				checkedFiles.add(filePath);
				
				List<NodeInfo> nodes = getNodesFromCache(file);
				if (nodes == null) {
					nodes = parseMindMapFile(file);
					if (nodes != null) {
						fileNodesCache.put(filePath, nodes);
						fileLastModifiedCache.put(filePath, file.lastModified());
					}
				}
				
				if (nodes != null) {
					for (NodeInfo node : nodes) {
						if (matchesKeywords(node, keywords)) {
							String preview = generatePreview(node, keywords);
							String nodeId = node.id != null ? node.id : String.valueOf(node.text.hashCode());
							String resultKey = filePath + "|" + nodeId + "|" + node.text;
							if (!seenResults.contains(resultKey)) {
								seenResults.add(resultKey);
								results.add(new SearchResult(file, node.text, preview, node.id, node.depth));
								if (results.size() >= MAX_RESULTS) {
									return;
								}
							}
						}
					}
				}
			}
		}
	}
	
	private List<NodeInfo> getNodesFromCache(File file) {
		String filePath = file.getAbsolutePath();
		Long cachedTime = fileLastModifiedCache.get(filePath);
		if (cachedTime != null && cachedTime == file.lastModified()) {
			return fileNodesCache.get(filePath);
		}
		fileNodesCache.remove(filePath);
		fileLastModifiedCache.remove(filePath);
		return null;
	}
	
	private List<NodeInfo> parseMindMapFile(File file) {
		List<NodeInfo> nodes = new ArrayList<NodeInfo>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
			reader.close();
			
			String xmlContent = content.toString();
			
			Pattern nodePattern = Pattern.compile("<node[^>]*>", Pattern.DOTALL);
			Matcher nodeMatcher = nodePattern.matcher(xmlContent);
			
			Stack<Integer> depthStack = new Stack<Integer>();
			depthStack.push(0);
			
			while (nodeMatcher.find()) {
				String nodeTag = nodeMatcher.group();
				String text = extractTextFromTag(nodeTag);
				String id = extractIdFromTag(nodeTag);
				String note = extractNote(xmlContent, nodeMatcher.end());
				
				String beforeText = xmlContent.substring(0, nodeMatcher.start());
				int startTagCount = beforeText.split("<node", -1).length - 1;
				int endTagCount = beforeText.split("</node>", -1).length - 1;
				int currentDepth = startTagCount - endTagCount;
				
				if (!text.isEmpty()) {
					nodes.add(new NodeInfo(text, note, id, currentDepth));
				}
			}
		} catch (Exception e) {
			LogUtils.warn("Error parsing " + file.getName() + ": " + e.getMessage());
			return null;
		}
		
		return nodes;
	}
	
	private String extractIdFromTag(String nodeTag) {
		Pattern idPattern = Pattern.compile("ID=\"([^\"]*)\"");
		Matcher matcher = idPattern.matcher(nodeTag);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}
	
	private String extractTextFromTag(String nodeTag) {
		Pattern textPattern = Pattern.compile("TEXT=\"([^\"]*)\"");
		Matcher matcher = textPattern.matcher(nodeTag);
		if (matcher.find()) {
			String text = matcher.group(1);
			text = decodeHtmlEntities(text);
			return text.replaceAll("\\s+", " ").trim();
		}
		return "";
	}
	
	private String decodeHtmlEntities(String text) {
		if (text == null) {
			return "";
		}
		Pattern entityPattern = Pattern.compile("&#x([0-9a-fA-F]+);");
		Matcher matcher = entityPattern.matcher(text);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			try {
				int code = Integer.parseInt(matcher.group(1), 16);
				matcher.appendReplacement(result, String.valueOf((char) code));
			} catch (Exception e) {
				matcher.appendReplacement(result, matcher.group());
			}
		}
		matcher.appendTail(result);
		return result.toString();
	}
	
	private String extractNote(String xmlContent, int startPos) {
		int noteStart = xmlContent.indexOf("<richcontent", startPos);
		if (noteStart == -1) {
			return "";
		}
		int noteEnd = xmlContent.indexOf("</richcontent>", noteStart);
		if (noteEnd == -1) {
			return "";
		}
		
		String noteContent = xmlContent.substring(noteStart, noteEnd + 15);
		if (!noteContent.contains("TYPE=\"NOTE\"")) {
			return "";
		}
		
		Pattern contentPattern = Pattern.compile("<richcontent[^>]*>(.*?)</richcontent>", Pattern.DOTALL);
		Matcher matcher = contentPattern.matcher(noteContent);
		if (matcher.find()) {
			String content = matcher.group(1);
			content = HtmlUtils.removeHtmlTagsFromString(content);
			content = decodeHtmlEntities(content);
			return content.replaceAll("\\s+", " ").trim();
		}
		return "";
	}
	
	private boolean matchesKeywords(NodeInfo node, List<String> keywords) {
		String text = node.text.toLowerCase();
		for (String keyword : keywords) {
			if (!text.contains(keyword)) {
				return false;
			}
		}
		return true;
	}
	
	private String generatePreview(NodeInfo node, List<String> keywords) {
		String text = node.text + " " + node.note;
		String lowerText = text.toLowerCase();
		
		int minPos = text.length();
		for (String keyword : keywords) {
			int pos = lowerText.indexOf(keyword.toLowerCase());
			if (pos >= 0 && pos < minPos) {
				minPos = pos;
			}
		}
		
		int start = Math.max(0, minPos - 30);
		int end = Math.min(text.length(), minPos + PREVIEW_LENGTH);
		
		String preview = text.substring(start, end).replaceAll("\\s+", " ").trim();
		if (start > 0) {
			preview = "..." + preview;
		}
		if (end < text.length()) {
			preview = preview + "...";
		}
		
		for (String keyword : keywords) {
			preview = preview.replaceAll("(?i)" + Pattern.quote(keyword), 
				"<span style='color:red;font-weight:bold'>$0</span>");
		}
		
		return preview;
	}
	
	private String createPadding(int count) {
		if (count <= 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < count; i++) {
			sb.append("&nbsp;");
		}
		return sb.toString();
	}
	
	private void openSelectedResult() {
		SearchResult result = resultList.getSelectedValue();
		if (result == null) {
			return;
		}
		
		try {
			final URL mapUrl = result.file.toURI().toURL();
			final String targetNodeId = result.nodeId;
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						Controller.getCurrentModeController().getMapController().newMap(mapUrl);
						
						if (targetNodeId != null && !targetNodeId.isEmpty()) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									try {
										MapModel map = Controller.getCurrentController().getMap();
										if (map != null) {
											NodeModel node = map.getNodeForID(targetNodeId);
											if (node != null) {
												Controller.getCurrentModeController().getMapController().select(node);
											}
										}
									} catch (Exception e) {
										LogUtils.warn("Failed to select node: " + e.getMessage());
									}
								}
							});
						}
					} catch (Exception e) {
						JOptionPane.showMessageDialog(GlobalSearchTabPanel.this, "无法打开文件: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "无法打开文件: " + e.getMessage());
		}
	}
	
	private void loadCacheIfNeeded() {
		executor.submit(new Runnable() {
			public void run() {
				File root = new File(SCAN_ROOT);
				if (root.exists()) {
					preloadCache(root);
				}
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						statusLabel.setText(getStatusText());
					}
				});
			}
		});
	}
	
	private void preloadCache(File dir) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		
		for (File file : files) {
			if (file.isDirectory() && !file.getName().startsWith(".")) {
				preloadCache(file);
			} else if (file.getName().toLowerCase().endsWith(".mm")) {
				String filePath = file.getAbsolutePath();
				if (!fileLastModifiedCache.containsKey(filePath) || 
					fileLastModifiedCache.get(filePath) != file.lastModified()) {
					List<NodeInfo> nodes = parseMindMapFile(file);
					if (nodes != null) {
						fileNodesCache.put(filePath, nodes);
						fileLastModifiedCache.put(filePath, file.lastModified());
					}
				}
			}
		}
	}
}