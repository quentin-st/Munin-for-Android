package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Label;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


public class Activity_Labels extends ListActivity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private Context			context;
	
	private SimpleAdapter 	sa;
	private ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	private Menu 			menu;
	private String			activityName;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		context = this;
		
		setContentView(R.layout.labelselection);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(getString(R.string.button_labels));
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(dh.Activity_Labels);
		
		Util.UI.applySwag(this);
		
		updateListView();
	}
	
	public void updateListView() {
		list.clear();
		getListView().setAdapter(null);
		
		if (muninFoo.labels.size() > 0) {
			HashMap<String,String> item;
			for(int i=0; i<muninFoo.labels.size(); i++){
				item = new HashMap<String,String>();
				item.put("line1", muninFoo.labels.get(i).getName());
				item.put("line2", muninFoo.labels.get(i).plugins.size() + "");
				list.add(item);
			}
			sa = new SimpleAdapter(Activity_Labels.this, list, R.layout.labelselection_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
			setListAdapter(sa);
			
			getListView().setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
					TextView label = (TextView) view.findViewById(R.id.line_a);
					Intent intent = new Intent(Activity_Labels.this, Activity_Label.class);
					intent.putExtra("label", label.getText().toString());
					startActivity(intent);
					Util.setTransition(context, TransitionStyle.DEEPER);
				}
			});
			
			getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long arg) {
					final TextView labelNameTextView = (TextView) view.findViewById(R.id.line_a);
					final String labelName = labelNameTextView.getText().toString();
					
					// Display actions list
					AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
					final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
							context, android.R.layout.simple_list_item_1);
					arrayAdapter.add(context.getString(R.string.rename_label));
					arrayAdapter.add(context.getString(R.string.delete));
					
					builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0: // Rename label
								final EditText input = new EditText(context);
								input.setText(labelName);
								
								new AlertDialog.Builder(context)
								.setTitle(R.string.rename_label)
								.setView(input)
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										String value = input.getText().toString();
										if (!value.equals(labelName)) {
											Label label = muninFoo.getLabel(labelName);
											label.setName(value);
											MuninFoo.getInstance(context).sqlite.dbHlpr.updateLabel(label);
											labelNameTextView.setText(value);
										}
										dialog.dismiss();
									}
								}).setNegativeButton(R.string.text64, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) { }
								}).show();
								break;
							case 1: // Delete label
								new AlertDialog.Builder(context)
								.setTitle(R.string.delete)
								.setMessage(R.string.text82)
								.setPositiveButton(R.string.text33, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Label label = muninFoo.getLabel(labelName);
										
										muninFoo.removeLabel(label);
										updateListView();
									}
								})
								.setNegativeButton(R.string.text34, null)
								.show();
								
								break;
							}
						}
					});
					builderSingle.show();
					
					return true;
				}
			});
		}
		else
			findViewById(R.id.no_label).setVisibility(View.VISIBLE);
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		
		dh.getDrawer().setOnOpenListener(new OnOpenListener() {
			@Override
			public void onOpen() {
				activityName = getActionBar().getTitle().toString();
				getActionBar().setTitle("Munin for Android");
				menu.clear();
				getMenuInflater().inflate(R.menu.main, menu);
			}
		});
		dh.getDrawer().setOnCloseListener(new OnCloseListener() {
			@Override
			public void onClose() {
				getActionBar().setTitle(activityName);
				createOptionsMenu();
			}
		});
		
		createOptionsMenu();
		return true;
	}
	private void createOptionsMenu() {
		menu.clear();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Labels.this, Activity_Settings.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Labels.this, Activity_About.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStop(this);
	}
}