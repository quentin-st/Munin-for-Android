package com.chteuchteu.munin;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Widget_Configure extends Activity {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private MuninFoo muninFoo;
	private MuninServer selectedServer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		muninFoo = MuninFoo.getInstance(this);
		
		// If the user closes window, don't create the widget
		setResult(RESULT_CANCELED);
		
		// Find widget id from launching intent
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null)
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		
		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
			finish();
		
		
		if (muninFoo.sqlite.dbHlpr.getWidget(mAppWidgetId) == null) {
			final Widget widget = new Widget();
			widget.setWidgetId(mAppWidgetId);
			
			setContentView(R.layout.widget_configuration);
			
			if (muninFoo != null) {
				final ListView lv1 = (ListView) findViewById(R.id.listview1); // servers
				final ListView lv2 = (ListView) findViewById(R.id.listview2); // plugins
				final ListView lv3 = (ListView) findViewById(R.id.listview3); // period
				final TextView tv = (TextView) findViewById(R.id.widgetConfiguration_textview);
				
				if (muninFoo.getHowManyServers() == 0) {
					tv.setText(R.string.text37);
					tv.setVisibility(View.VISIBLE);
				}
				if (!muninFoo.premium) {
					tv.setText(R.string.text58);
					tv.setVisibility(View.VISIBLE);
				}
				
				ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
				list.clear();
				HashMap<String,String> item;
				for(int i=0; i<muninFoo.getOrderedServers().size(); i++){
					item = new HashMap<String,String>();
					item.put("line1", muninFoo.getOrderedServers().get(i).getName());
					item.put("line2", muninFoo.getOrderedServers().get(i).getServerUrl());
					list.add(item);
				}
				SimpleAdapter sa = new SimpleAdapter(this, list, R.layout.servers_list_dark, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
				lv1.setAdapter(sa);
				lv1.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
						String adresse = ((TextView) view.findViewById(R.id.line_b)).getText().toString();
						
						for (MuninServer s : muninFoo.getServers()) {
							if (s.getServerUrl().equals(adresse)) {
								selectedServer = s; break;
							}
						}
						
						// Populate lv2
						List<MuninPlugin> plugins = selectedServer.getPlugins();
						
						ArrayList<HashMap<String,String>> list2 = new ArrayList<HashMap<String,String>>();
						list2.clear();
						HashMap<String,String> item2;
						for(int i=0; i<plugins.size(); i++){
							item2 = new HashMap<String,String>();
							item2.put("line1", plugins.get(i).getFancyName());
							item2.put("line2", plugins.get(i).getName());
							list2.add(item2);
						}
						SimpleAdapter sa2 = new SimpleAdapter(Widget_Configure.this, list2, R.layout.plugins_list_dark, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
						lv2.setAdapter(sa2);
						
						lv2.setOnItemClickListener(new OnItemClickListener() {
							public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
								String pluginName = ((TextView) view.findViewById(R.id.line_b)).getText().toString();
								
								for (MuninPlugin p : selectedServer.getPlugins()) {
									if (p.getName().equals(pluginName))
										widget.setPlugin(p);
								}
								
								lv2.setVisibility(View.GONE);
								
								ArrayList<HashMap<String,String>> list3 = new ArrayList<HashMap<String,String>>();
								list3.clear();
								HashMap<String,String> item3;
								String[] periods = {"day", "week", "month", "year" };
								for (int i=0; i<4; i++) {
									item3 = new HashMap<String,String>();
									item3.put("line1", periods[i]);
									list3.add(item3);
								}
								SimpleAdapter sa3 = new SimpleAdapter(Widget_Configure.this, list3, R.layout.widget_periodselection_dark, new String[] { "line1" }, new int[] {R.id.line_a});
								lv3.setAdapter(sa3);
								lv3.setOnItemClickListener(new OnItemClickListener() {
									public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
										String[] periods = {"day", "week", "month", "year" };
										widget.setPeriod(periods[position]);
										
										LinearLayout ll = (LinearLayout) findViewById(R.id.final_instructions);
										lv3.setVisibility(View.GONE);
										ll.setVisibility(View.VISIBLE);
										ll.requestFocus();
										final CheckBox cb = (CheckBox) findViewById(R.id.checkbox_wifi);
										final CheckBox cb2 = (CheckBox) findViewById(R.id.checkbox_hidetitle);
										Button btn = (Button) findViewById(R.id.save);
										
										btn.setOnClickListener(new View.OnClickListener() {
											public void onClick(View v) {
												// Save & close
												widget.setWifiOnly(cb.isChecked());
												widget.setHideServerName(cb2.isChecked());
												
												muninFoo.sqlite.dbHlpr.insertWidget(widget);
												//muninFoo.sqlite.logWidgets();
												configureWidget(getApplicationContext());
												
												// Make sure we pass back the original appWidgetId before closing the activity
												Intent resultValue = new Intent();
												resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
												setResult(RESULT_OK, resultValue);
												finish();
											}
										});
									}
								});
								
								lv3.setVisibility(View.VISIBLE);
								lv3.requestFocus();
							}
						});
						
						lv1.setVisibility(View.GONE);
						
						lv2.setVisibility(View.VISIBLE);
						lv2.requestFocus();
					}
				});
				
				lv1.setVisibility(View.VISIBLE);
				lv1.requestFocus();
			}
		}
	}
	
	/**
	 * Configures the created widget
	 * @param context
	 */
	public void configureWidget(Context context) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		Widget_GraphWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId, true);
	}
}