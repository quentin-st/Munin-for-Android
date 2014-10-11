package com.chteuchteu.munin;

import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.ui.Activity_Servers;

public class Adapter_ExpandableListView extends BaseExpandableListAdapter {
	private Activity_Servers activity;
	private Context context;
	private Map<String, List<String>> serversCollection;
	private List<String> servers;
	
	public Adapter_ExpandableListView(Activity_Servers activity, Context context, List<String> servers,
			Map<String, List<String>> serversCollection) {
		this.activity = activity;
		this.context = context;
		this.serversCollection = serversCollection;
		this.servers = servers;
	}
	
	public Object getChild(int groupPosition, int childPosition) {
		return serversCollection.get(servers.get(groupPosition)).get(childPosition);
	}
	
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}
	
	@SuppressLint("InflateParams")
	public View getChildView(final int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		final String server = (String) getChild(groupPosition, childPosition);
		LayoutInflater inflater = activity.getLayoutInflater();
		
		if (convertView == null)
			convertView = inflater.inflate(R.layout.expandable_server, null);
		
		TextView item = (TextView) convertView.findViewById(R.id.server);
		item.setText(server);
		
		// Click action
		convertView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.onChildClick(groupPosition, childPosition);
			}
		});
		
		// Long click actions
		convertView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return activity.onChildLongClick(groupPosition, childPosition);
			}
		});
		
		return convertView;
	}
	
	public int getChildrenCount(int groupPosition) {
		return serversCollection.get(servers.get(groupPosition)).size();
	}
	
	public Object getGroup(int groupPosition) {
		return servers.get(groupPosition);
	}
	
	public int getGroupCount() {
		return servers.size();
	}
	
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}
	
	@SuppressLint("InflateParams")
	public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		String masterName = (String) getGroup(groupPosition);
		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.expandable_master, null);
		}
		TextView item = (TextView) convertView.findViewById(R.id.master);
		Util.Fonts.setFont((Context) context, item, CustomFont.RobotoCondensed_Regular);
		item.setText(masterName);
		
		ImageView edit = (ImageView) convertView.findViewById(R.id.edit);
		edit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				activity.onGroupItemOptionsClick(groupPosition);
			}
		});
		return convertView;
	}
	
	public boolean hasStableIds() {
		return true;
	}
	
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}