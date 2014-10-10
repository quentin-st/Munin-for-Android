package com.chteuchteu.munin;

import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.ui.Activity_Servers;
import com.chteuchteu.munin.ui.Activity_ServersEdit;

public class Adapter_ExpandableListView extends BaseExpandableListAdapter {
	private Activity context;
	private Map<String, List<String>> serversCollection;
	private List<String> servers;
	private MuninFoo muninFoo;
	
	public Adapter_ExpandableListView(Activity context, List<String> servers,
			Map<String, List<String>> serversCollection, MuninFoo f) {
		this.context = context;
		this.serversCollection = serversCollection;
		this.servers = servers;
		this.muninFoo = f;
	}
	
	public Object getChild(int groupPosition, int childPosition) {
		return serversCollection.get(servers.get(groupPosition)).get(childPosition);
	}
	
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}
	
	@SuppressLint("InflateParams")
	public View getChildView(int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		final String server = (String) getChild(groupPosition, childPosition);
		LayoutInflater inflater = context.getLayoutInflater();
		
		if (convertView == null)
			convertView = inflater.inflate(R.layout.expandable_server, null);
		
		TextView item = (TextView) convertView.findViewById(R.id.server);
		item.setText(server);
		
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
				// Display actions list
				AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
						context, android.R.layout.simple_list_item_1);
				arrayAdapter.add(context.getString(R.string.editServersTitle));
				arrayAdapter.add(context.getString(R.string.renameMaster));
				
				builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							MuninMaster m = muninFoo.masters.get(groupPosition);
							Intent i = new Intent(context, Activity_ServersEdit.class);
							i.putExtra("masterId", m.getId());
							context.startActivity(i);
							Util.setTransition(context, TransitionStyle.DEEPER);
							break;
						case 1:
							final MuninMaster master = muninFoo.masters.get(groupPosition);
							final EditText input = new EditText(context);
							input.setText(master.getName());
							
							new AlertDialog.Builder(context)
							.setTitle(R.string.renameMaster)
							.setView(input)
							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									String value = input.getText().toString();
									if (!value.equals(master.getName())) {
										master.setName(value);
										MuninFoo.getInstance(context).sqlite.dbHlpr.updateMuninMaster(master);
										context.startActivity(new Intent(context, Activity_Servers.class));
									}
									dialog.dismiss();
								}
							}).setNegativeButton(R.string.text64, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) { }
							}).show();
							break;
						}
					}
				});
				builderSingle.show();
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