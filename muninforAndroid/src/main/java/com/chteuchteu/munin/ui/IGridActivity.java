package com.chteuchteu.munin.ui;

import com.chteuchteu.munin.obj.Grid;

public interface IGridActivity {
	void onPreviewHide();
	void onEditModeChange(boolean editing);
	void onPreview();
	void onGridLoaded(Grid grid);
	void onGridSaved(Grid grid);

	/**
	 * Triggered when the user hits the "Load" button when autoLoad=false
	 */
	void onManualLoad();
}
