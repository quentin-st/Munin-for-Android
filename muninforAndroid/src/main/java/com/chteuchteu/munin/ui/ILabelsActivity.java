package com.chteuchteu.munin.ui;

import com.chteuchteu.munin.obj.Label;

public interface ILabelsActivity {
	public void onLabelClick(Label label);
	public void onLabelItemClick(int pos, String labelName, long labelId);
	public void onLabelsItemsListFragmentLoaded();
}
