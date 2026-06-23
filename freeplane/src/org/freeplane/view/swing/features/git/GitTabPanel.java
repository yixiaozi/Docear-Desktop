package org.freeplane.view.swing.features.git;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.freeplane.core.util.LogUtils;

public class GitTabPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private final DefaultListModel<GitFileChange> fileListModel = new DefaultListModel<GitFileChange>();
	private final JList<GitFileChange> fileList = new JList<GitFileChange>(fileListModel);
	private final JTextField summaryField = new JTextField();
	private final JTextArea descriptionArea = new JTextArea();
	private final JButton refreshButton = new JButton("刷新");
	private final JButton commitButton = new JButton("提交");
	private final JLabel statusLabel = new JLabel("就绪");

	private static final String ADD_ICON = "+";
	private static final String MODIFY_ICON = "~";
	private static final String DELETE_ICON = "-";

	public GitTabPanel() {
		super(new BorderLayout(4, 4));

		JPanel topPanel = new JPanel(new BorderLayout(4, 0));
		topPanel.add(statusLabel, BorderLayout.CENTER);
		topPanel.add(refreshButton, BorderLayout.EAST);
		add(topPanel, BorderLayout.NORTH);

		JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainSplit.setDividerLocation(300);

		JScrollPane fileScroll = new JScrollPane(fileList);
		fileScroll.setBorder(BorderFactory.createTitledBorder("修改的文件"));
		fileList.setCellRenderer(new GitFileCellRenderer());
		fileList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		mainSplit.setTopComponent(fileScroll);

		JPanel commitPanel = new JPanel(new BorderLayout(4, 4));

		JPanel summaryPanel = new JPanel(new BorderLayout(4, 0));
		summaryPanel.add(new JLabel("Summary (required)"), BorderLayout.WEST);
		summaryField.setPreferredSize(new Dimension(200, 25));
		summaryPanel.add(summaryField, BorderLayout.CENTER);
		commitPanel.add(summaryPanel, BorderLayout.NORTH);

		JPanel descriptionPanel = new JPanel(new BorderLayout(4, 0));
		descriptionPanel.add(new JLabel("Description"), BorderLayout.NORTH);
		descriptionArea.setRows(4);
		descriptionArea.setBorder(BorderFactory.createEtchedBorder());
		JScrollPane descScroll = new JScrollPane(descriptionArea);
		descriptionPanel.add(descScroll, BorderLayout.CENTER);
		commitPanel.add(descriptionPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(commitButton);
		commitPanel.add(buttonPanel, BorderLayout.SOUTH);

		mainSplit.setBottomComponent(commitPanel);

		add(mainSplit, BorderLayout.CENTER);

		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshChanges();
			}
		});

		commitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				commitChanges();
			}
		});

		refreshChanges();
	}

	private void refreshChanges() {
		statusLabel.setText("正在扫描...");
		commitButton.setEnabled(false);

		new Thread(new Runnable() {
			@Override
			public void run() {
				final List<GitFileChange> changes = getGitChanges();
				final File repoDir = findGitRepository();

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						fileListModel.clear();
						for (GitFileChange change : changes) {
							fileListModel.addElement(change);
						}
						if (repoDir != null) {
							statusLabel.setText("仓库: " + repoDir.getName() + " - 发现 " + changes.size() + " 个修改");
						} else {
							statusLabel.setText("未找到 Git 仓库");
						}
						commitButton.setEnabled(changes.size() > 0);
					}
				});
			}
		}).start();
	}

	private List<GitFileChange> getGitChanges() {
		List<GitFileChange> changes = new ArrayList<GitFileChange>();
		
		File workspaceDir = findGitRepository();
		if (workspaceDir == null) {
			return changes;
		}

		try {
			Process process = Runtime.getRuntime().exec(new String[] { "git", "status", "--porcelain" }, null, workspaceDir);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.length() >= 3) {
					String status = line.substring(0, 2).trim();
					String fileName = line.substring(3);
					GitFileChange.Status changeStatus;
					if (status.startsWith("A")) {
						changeStatus = GitFileChange.Status.ADDED;
					} else if (status.startsWith("D")) {
						changeStatus = GitFileChange.Status.DELETED;
					} else {
						changeStatus = GitFileChange.Status.MODIFIED;
					}
					changes.add(new GitFileChange(fileName, changeStatus));
				}
			}
			process.waitFor();
		} catch (Exception e) {
			LogUtils.warn("Git command failed: " + e.getMessage());
		}

		return changes;
	}
	
	private File findGitRepository() {
		File docearDir = new File(System.getProperty("user.dir"));
		File gitDir = new File(docearDir, ".git");
		if (gitDir.exists() && gitDir.isDirectory()) {
			return docearDir;
		}
		
		File parentDir = docearDir.getParentFile();
		if (parentDir != null) {
			gitDir = new File(parentDir, ".git");
			if (gitDir.exists() && gitDir.isDirectory()) {
				return parentDir;
			}
		}
		
		File workspaceDir = new File(System.getProperty("user.home"), "Docear-Desktop");
		if (workspaceDir.exists()) {
			gitDir = new File(workspaceDir, ".git");
			if (gitDir.exists() && gitDir.isDirectory()) {
				return workspaceDir;
			}
		}
		
		return docearDir;
	}

	private void commitChanges() {
		String summary = summaryField.getText().trim();
		if (summary.isEmpty()) {
			statusLabel.setText("请输入提交摘要");
			return;
		}

		String description = descriptionArea.getText().trim();
		final String commitMessage = summary + (description.isEmpty() ? "" : "\n\n" + description);

		statusLabel.setText("正在提交...");
		commitButton.setEnabled(false);

		new Thread(new Runnable() {
			@Override
			public void run() {
				File workspaceDir = new File(System.getProperty("user.dir"));
				boolean success = false;

				try {
					Process addProcess = Runtime.getRuntime().exec(new String[] { "git", "add", "." }, null, workspaceDir);
					addProcess.waitFor();

					Process commitProcess = Runtime.getRuntime().exec(
							new String[] { "git", "commit", "-m", commitMessage }, null, workspaceDir);
					commitProcess.waitFor();
					success = commitProcess.exitValue() == 0;
				} catch (Exception e) {
					LogUtils.warn("Git commit failed: " + e.getMessage());
				}

				final boolean finalSuccess = success;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (finalSuccess) {
							statusLabel.setText("提交成功");
							summaryField.setText("");
							descriptionArea.setText("");
							refreshChanges();
						} else {
							statusLabel.setText("提交失败");
							commitButton.setEnabled(true);
						}
					}
				});
			}
		}).start();
	}

	public static class GitFileChange {
		public enum Status {
			ADDED, MODIFIED, DELETED
		}

		private final String fileName;
		private final Status status;
		private boolean selected = true;

		public GitFileChange(String fileName, Status status) {
			this.fileName = fileName;
			this.status = status;
		}

		public String getFileName() {
			return fileName;
		}

		public Status getStatus() {
			return status;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}

	private class GitFileCellRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;
		private final Border noFocusBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value instanceof GitFileChange) {
				GitFileChange change = (GitFileChange) value;
				String iconText = "";

				switch (change.getStatus()) {
				case ADDED:
					iconText = ADD_ICON;
					setForeground(new Color(0, 128, 0));
					break;
				case MODIFIED:
					iconText = MODIFY_ICON;
					setForeground(new Color(192, 128, 0));
					break;
				case DELETED:
					iconText = DELETE_ICON;
					setForeground(Color.RED);
					break;
				}

				setText(iconText + " " + change.getFileName());
				setBorder(noFocusBorder);
			}

			return this;
		}
	}
}