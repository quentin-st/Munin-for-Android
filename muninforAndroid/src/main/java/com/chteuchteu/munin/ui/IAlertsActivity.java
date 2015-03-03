package com.chteuchteu.munin.ui;

public interface IAlertsActivity {
	public void onScanProgress(int finishedIndex);
	public void onGroupScanFinished(int fromIndex, int toIndex);
}
