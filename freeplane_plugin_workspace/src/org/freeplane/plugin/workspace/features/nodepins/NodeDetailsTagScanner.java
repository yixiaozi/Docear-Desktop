package org.freeplane.plugin.workspace.features.nodepins;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.features.favorites.FavoriteUriUtils;
import org.freeplane.plugin.workspace.model.WorkspaceModel;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public final class NodeDetailsTagScanner {

	private NodeDetailsTagScanner() {
	}

	public static List scanAllProjects() {
		final List entries = new ArrayList();
		final WorkspaceModel model = WorkspaceController.getCurrentModel();
		if (model == null) {
			return entries;
		}
		final List projects = model.getProjects();
		synchronized (projects) {
			for (final Iterator it = projects.iterator(); it.hasNext();) {
				final AWorkspaceProject project = (AWorkspaceProject) it.next();
				final File projectHome = URIUtils.getAbsoluteFile(project.getProjectHome());
				if (projectHome != null && projectHome.exists()) {
					scanDirectory(project, projectHome, entries);
				}
			}
		}
		return entries;
	}

	private static void scanDirectory(final AWorkspaceProject project, final File directory, final List entries) {
		if (directory == null || !directory.isDirectory()) {
			return;
		}
		final File[] children = directory.listFiles();
		if (children == null) {
			return;
		}
		for (int i = 0; i < children.length; i++) {
			final File child = children[i];
			if (child.getName().startsWith(".") || child.getName().equalsIgnoreCase("bin")) {
				continue;
			}
			if (child.isDirectory()) {
				scanDirectory(project, child, entries);
			}
			else if (child.getName().toLowerCase().endsWith(".mm")) {
				entries.addAll(scanFile(project, child));
			}
		}
	}

	public static List scanFile(final AWorkspaceProject project, final File file) {
		final List entries = new ArrayList();
		if (file == null || !file.exists()) {
			return entries;
		}
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(false);
			final SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(file, new DefaultHandler() {
				private String currentId;
				private String currentText;
				private final StringBuilder detailsBuilder = new StringBuilder();
				private boolean inDetails;

				public void startElement(final String uri, final String localName, final String qName,
						final Attributes attributes) {
					if ("node".equals(qName)) {
						currentId = attributes.getValue("ID");
						currentText = attributes.getValue("TEXT");
						detailsBuilder.setLength(0);
						inDetails = false;
					}
					else if ("richcontent".equals(qName)) {
						final String type = attributes.getValue("TYPE");
						if ("DETAILS".equals(type)) {
							inDetails = true;
							detailsBuilder.setLength(0);
						}
					}
				}

				public void characters(final char[] ch, final int start, final int length) {
					if (inDetails) {
						detailsBuilder.append(ch, start, length);
					}
				}

				public void endElement(final String uri, final String localName, final String qName) {
					if ("richcontent".equals(qName) && inDetails) {
						inDetails = false;
					}
					else if ("node".equals(qName)) {
						addEntryIfTagged();
						currentId = null;
						currentText = null;
						detailsBuilder.setLength(0);
					}
				}

				private void addEntryIfTagged() {
					if (currentId == null || detailsBuilder.length() == 0) {
						return;
					}
					final String detailsHtml = detailsBuilder.toString();
					if (!NodeDetailsTagUtils.hasAnyManagedTag(detailsHtml)) {
						return;
					}
					final Set allTags = NodeDetailsTagUtils.parseAllTags(detailsHtml);
					final boolean pinned = allTags.contains(NodeDetailsTagUtils.PIN_TAG);
					final LinkedHashSet userTags = new LinkedHashSet();
					for (final Iterator it = allTags.iterator(); it.hasNext();) {
						final String tag = (String) it.next();
						if (!NodeDetailsTagUtils.PIN_TAG.equals(tag) && NodeDetailsTagUtils.isValidTagName(tag)) {
							userTags.add(tag);
						}
					}
					final String mapUri = FavoriteUriUtils.toStoredUri(file, project);
					if (mapUri == null) {
						return;
					}
					final String key = mapUri + "#" + currentId;
					final String label = currentText == null ? "" : HtmlUtils.unescapeHTMLUnicodeEntity(currentText.trim());
					entries.add(new NodePinEntry(key, userTags, pinned, label));
				}
			});
		}
		catch (final Exception e) {
			LogUtils.warn("could not scan tags in " + file.getAbsolutePath(), e);
		}
		return entries;
	}

}
