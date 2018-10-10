package com.chteuchteu.munin.adptr;

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

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.ui.IServersActivity;

import java.util.List;
import java.util.Map;

public class Adapter_ExpandableListView extends BaseExpandableListAdapter {
	private IServersActivity activity;
	private Context context;
	private Map<MuninMaster, List<String>> nodesCollection;
	private List<MuninMaster> nodes;
	
	public Adapter_ExpandableListView(IServersActivity activity, Context context, List<MuninMaster> nodes,
			Map<MuninMaster, List<String>> nodesCollection) {
		this.activity = activity;
		this.context = context;
		this.nodesCollection = nodesCollection;
		this.nodes = nodes;
	}
	
	public Object getChild(int groupPosition, int childPosition) {
		return nodesCollection.get(nodes.get(groupPosition)).get(childPosition);
	}
	
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}
	
	@SuppressLint("InflateParams")
	public View getChildView(final int groupPosition, final int childPosition,
	                         boolean isLastChild, View convertView, ViewGroup parent) {
		final String node = (String) getChild(groupPosition, childPosition);
		LayoutInflater inflater = LayoutInflater.from(context);
		
		if (convertView == null)
			convertView = inflater.inflate(R.layout.expandable_server, null);
		
		TextView item = (TextView) convertView.findViewById(R.id.server);
		item.setText(node);
		
		// Click action
		convertView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.onChildClick();
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
	
	@SuppressLint("InflateParams")
	public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		MuninMaster master = (MuninMaster) getGroup(groupPosition);
		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.expandable_master, null);
		}
		TextView item = (TextView) convertView.findViewById(R.id.master);
		Util.Fonts.setFont(context, item, CustomFont.Roboto_Medium);
		item.setText(master.getName());
		
		if (master.isAuthNeeded()) {
            convertView.findViewById(R.id.credentials).setVisibility(View.VISIBLE);
            convertView.findViewById(R.id.credentials).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.onParentCredentialsClick(groupPosition);
                }
            });
        }
		
		final ImageView edit = (ImageView) convertView.findViewById(R.id.edit);
		edit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				activity.onParentOptionsClick(edit, groupPosition);
			}
		});
		return convertView;
	}
	
	public int getChildrenCount(int groupPosition) {
		return nodesCollection.get(nodes.get(groupPosition)).size();
	}
	
	public Object getGroup(int groupPosition) {
		return nodes.get(groupPosition);
	}
	
	public int getGroupCount() {
		return nodes.size();
	}
	
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}
	
	public boolean hasStableIds() {
		return true;
	}
	
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}
