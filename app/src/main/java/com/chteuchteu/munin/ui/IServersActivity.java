package com.chteuchteu.munin.ui;

import android.view.View;

public interface IServersActivity {
	void onChildClick();
	boolean onChildLongClick(int groupPosition, int childPosition);
	void onParentOptionsClick(View overflowIcon, int position);
	void onParentCredentialsClick(int position);
}
