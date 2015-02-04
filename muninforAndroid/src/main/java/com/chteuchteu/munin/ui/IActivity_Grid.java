package com.chteuchteu.munin.ui;

import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.MuninPlugin;

public interface IActivity_Grid {
	public void updatePeriodMenuItem(MuninPlugin.Period newPeriod);
	public void onPreviewHide();
	public void onEditModeChange(boolean editing);
	public void onPreview();
	public void onGridLoaded(Grid grid);
}
