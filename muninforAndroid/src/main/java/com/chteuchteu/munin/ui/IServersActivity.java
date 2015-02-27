package com.chteuchteu.munin.ui;

import android.view.View;

public interface IServersActivity {
	public void onChildClick();
	public boolean onChildLongClick(int groupPosition, int childPosition);
	public void onParentOptionsClick(View overflowIcon, int position);
	public void onParentCredentialsClick(int position);
}
