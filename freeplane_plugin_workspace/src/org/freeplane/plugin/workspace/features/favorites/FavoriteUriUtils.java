package org.freeplane.plugin.workspace.features.favorites;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.features.ProjectURLHandler;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;
import org.freeplane.plugin.workspace.model.WorkspaceModel;

public final class FavoriteUriUtils {

	private FavoriteUriUtils() {
	}

	public static String toStoredUri(final File file, final AWorkspaceProject project) {
		if (file == null) {
			return null;
		}
		if (project != null) {
			final URI relative = project.getRelativeURI(file.toURI());
			if (relative != null) {
				return relative.toString();
			}
		}
		return file.toURI().toString();
	}

	public static String toStoredUri(final AWorkspaceTreeNode node) {
		final File file = WorkspaceMindMapUtils.getWorkspaceFile(node);
		if (file == null) {
			return null;
		}
		return toStoredUri(file, WorkspaceController.getSelectedProject(node));
	}

	public static File resolveToFile(final String storedUri) {
		if (storedUri == null || storedUri.length() == 0) {
			return null;
		}
		try {
			final URI uri = new URI(storedUri);
			if (WorkspaceController.PROJECT_RESOURCE_URL_PROTOCOL.equals(uri.getScheme())) {
				final String projectId = uri.getHost();
				final AWorkspaceProject project = WorkspaceController.getCachedProjectByID(projectId);
				if (project != null) {
					final URL absoluteUrl = ProjectURLHandler.resolve(project, uri.toURL());
					return URIUtils.getAbsoluteFile(absoluteUrl.toURI());
				}
			}
			return URIUtils.getAbsoluteFile(uri);
		}
		catch (final Exception e) {
			return null;
		}
	}

	public static AWorkspaceProject resolveProject(final String storedUri) {
		if (storedUri == null) {
			return null;
		}
		try {
			final URI uri = new URI(storedUri);
			if (WorkspaceController.PROJECT_RESOURCE_URL_PROTOCOL.equals(uri.getScheme())) {
				return WorkspaceController.getCachedProjectByID(uri.getHost());
			}
		}
		catch (final Exception e) {
			// ignore
		}
		try {
			final File file = URIUtils.getAbsoluteFile(new URI(storedUri));
			if (file == null) {
				return null;
			}
			return findProjectForFile(file);
		}
		catch (final Exception e) {
			return null;
		}
	}

	public static AWorkspaceProject findProjectForFile(final File file) {
		if (file == null) {
			return null;
		}
		final WorkspaceModel model = WorkspaceController.getCurrentModel();
		if (model == null) {
			return null;
		}
		final List projects = model.getProjects();
		synchronized (projects) {
			for (final Iterator it = projects.iterator(); it.hasNext();) {
				final AWorkspaceProject project = (AWorkspaceProject) it.next();
				final File projectHome = URIUtils.getAbsoluteFile(project.getProjectHome());
				if (projectHome != null && isFileInsideProject(file, projectHome)) {
					return project;
				}
			}
		}
		return null;
	}

	public static String normalizeToStoredUri(final String uri) {
		if (uri == null) {
			return null;
		}
		try {
			final URI parsed = new URI(uri);
			if (WorkspaceController.PROJECT_RESOURCE_URL_PROTOCOL.equals(parsed.getScheme())) {
				return uri;
			}
			final File file = URIUtils.getAbsoluteFile(parsed);
			if (file != null) {
				return toStoredUri(file, findProjectForFile(file));
			}
		}
		catch (final Exception e) {
			// ignore
		}
		return uri;
	}

	public static String toRelativePath(final String storedUri, final AWorkspaceProject project) {
		if (storedUri == null || project == null) {
			return null;
		}
		try {
			final URI uri = new URI(storedUri);
			if (WorkspaceController.PROJECT_RESOURCE_URL_PROTOCOL.equals(uri.getScheme())) {
				if (!project.getProjectID().equals(uri.getHost())) {
					return null;
				}
				String path = uri.getRawPath();
				if (path != null && path.startsWith("/")) {
					path = path.substring(1);
				}
				return path;
			}
			final File file = URIUtils.getAbsoluteFile(uri);
			final File projectHome = URIUtils.getAbsoluteFile(project.getProjectHome());
			if (file != null && projectHome != null && isFileInsideProject(file, projectHome)) {
				final URI relative = project.getRelativeURI(file.toURI());
				if (relative != null) {
					return toRelativePath(relative.toString(), project);
				}
			}
		}
		catch (final Exception e) {
			// ignore
		}
		return null;
	}

	public static String fromRelativePath(final AWorkspaceProject project, final String relativePath) {
		if (project == null || relativePath == null || relativePath.length() == 0) {
			return null;
		}
		try {
			return new URI(WorkspaceController.PROJECT_RESOURCE_URL_PROTOCOL + "://" + project.getProjectID() + "/" + relativePath).toString();
		}
		catch (final Exception e) {
			return null;
		}
	}

	private static boolean isFileInsideProject(final File file, final File projectHome) {
		try {
			final String filePath = file.getCanonicalPath();
			final String homePath = projectHome.getCanonicalPath();
			return filePath.startsWith(homePath + File.separator) || filePath.equals(homePath);
		}
		catch (final Exception e) {
			return false;
		}
	}
}
