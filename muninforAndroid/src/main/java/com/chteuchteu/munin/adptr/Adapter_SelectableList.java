package com.chteuchteu.munin.adptr;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.HashMap;
import java.util.List;

public class Adapter_SelectableList extends ArrayAdapter {
	private int selectedIndex;
	private HashMap<Integer, View> viewsList;

	public Adapter_SelectableList(Context context, int resource, int textViewResourceId, List objects) {
		super(context, resource, textViewResourceId, objects);
		this.selectedIndex = -1;
		this.viewsList = new HashMap<>();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);

		viewsList.put(position, view);

		return view;
	}

	public void setSelectedItem(int position) {
		if (this.selectedIndex != -1 && viewsList.containsKey(this.selectedIndex))
			viewsList.get(this.selectedIndex).setBackgroundColor(Color.TRANSPARENT);

		if (viewsList.containsKey(position))
			viewsList.get(position).setBackgroundColor(0x11000000);

		this.selectedIndex = position;
	}
}
