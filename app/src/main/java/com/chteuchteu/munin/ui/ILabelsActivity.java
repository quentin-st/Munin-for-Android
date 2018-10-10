package com.chteuchteu.munin.ui;

import com.chteuchteu.munin.obj.Label;

public interface ILabelsActivity {
	void onLabelClick(Label label);
	void onLabelItemClick(int pos, String labelName, long labelId);
	void onLabelsItemsListFragmentLoaded();
	void onLabelsFragmentLoaded();
	void unselectLabel();
}
