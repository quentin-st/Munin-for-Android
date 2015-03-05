package com.chteuchteu.munin.ui;

import android.graphics.Bitmap;

import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.MuninPlugin;

public interface IGridActivity {
	public void updatePeriodMenuItem(MuninPlugin.Period newPeriod);
	public void onPreviewHide();
	public void onEditModeChange(boolean editing);
	public void onPreview();
	public void onGridLoaded(Grid grid);
	public void onGridItemGraphLoaded(GridItem item, Bitmap bitmap);

	/**
	 * Triggered when the user hits the "Load" button when autoLoad=false
	 */
	public void onManualLoad();
}
