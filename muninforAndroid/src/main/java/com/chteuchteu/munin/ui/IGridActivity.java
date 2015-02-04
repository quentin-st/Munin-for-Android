package com.chteuchteu.munin.ui;

import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.MuninPlugin;

public interface IGridActivity {
	public void updatePeriodMenuItem(MuninPlugin.Period newPeriod);
	public void onPreviewHide();
	public void onEditModeChange(boolean editing);
	public void onPreview();
	public void onGridLoaded(Grid grid);
}
