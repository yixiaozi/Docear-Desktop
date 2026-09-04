package org.docear.plugin.mermaid;

/**
 * Public top-level object exposed to Mermaid shell JavaScript via {@code javaBridge}.
 * Must not be a nested class — JavaFX JSObject interop rejects inner classes.
 */
public final class MermaidJsBridge {

	public interface Callback {
		void onSuccess(String source, float zoom, boolean gantt, int width, int height);

		void onError(String source, String message);
	}

	private final String source;
	private final float zoom;
	private final boolean gantt;
	private final Callback callback;

	public MermaidJsBridge(final String source, final float zoom, final boolean gantt,
			final Callback callback) {
		this.source = source != null ? source : "";
		this.zoom = zoom;
		this.gantt = gantt;
		this.callback = callback;
	}

	public String getSource() {
		return source;
	}

	public void onSuccess(final Object w, final Object h) {
		if (callback != null) {
			callback.onSuccess(source, zoom, gantt, toInt(w, 400), toInt(h, 300));
		}
	}

	public void onError(final Object message) {
		if (callback != null) {
			callback.onError(source, message != null ? String.valueOf(message) : "render error");
		}
	}

	private static int toInt(final Object o, final int fallback) {
		if (o instanceof Number) {
			return ((Number) o).intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o));
		}
		catch (Exception e) {
			return fallback;
		}
	}
}
