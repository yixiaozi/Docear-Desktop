package org.freeplane.view.swing.features.git;

final class GitSyncStatus {
	final int ahead;
	final int behind;
	final boolean hasUpstream;
	final boolean fetchOk;
	final String branch;
	final String upstream;
	final String error;

	GitSyncStatus(final int ahead, final int behind, final boolean hasUpstream, final boolean fetchOk,
	    final String branch, final String upstream, final String error) {
		this.ahead = ahead;
		this.behind = behind;
		this.hasUpstream = hasUpstream;
		this.fetchOk = fetchOk;
		this.branch = branch;
		this.upstream = upstream;
		this.error = error;
	}

	boolean needsPull() {
		return behind > 0;
	}

	boolean needsPush() {
		return ahead > 0;
	}

	boolean inSync() {
		return hasUpstream && fetchOk && ahead == 0 && behind == 0;
	}

	boolean diverged() {
		return ahead > 0 && behind > 0;
	}

	String syncSummary() {
		if (error != null && error.length() > 0) {
			return error;
		}
		if (!hasUpstream) {
			return "未配置 upstream";
		}
		if (!fetchOk) {
			return "无法连接远端";
		}
		if (inSync()) {
			return "与远端一致";
		}
		if (diverged()) {
			return "本地与远端已分叉 (需先拉取再推送)";
		}
		if (needsPull()) {
			return "远端有 " + behind + " 个新提交";
		}
		if (needsPush()) {
			return "本地有 " + ahead + " 个未推送提交";
		}
		return "";
	}
}
