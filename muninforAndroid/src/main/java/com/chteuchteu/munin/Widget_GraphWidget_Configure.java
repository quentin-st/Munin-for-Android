package com.chteuchteu.munin;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.obj.GraphWidget;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Widget_GraphWidget_Configure extends Activity {
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
		Bundle extras = getIntent().getExtras();
		if (extras != null)
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		
		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
			finish();
		
		
		if (muninFoo.sqlite.dbHlpr.getGraphWidget(mAppWidgetId) == null) {
			final GraphWidget graphWidget = new GraphWidget();
			graphWidget.setWidgetId(mAppWidgetId);
			
			if (muninFoo != null) {
				LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				final View dialogView = mInflater.inflate(R.layout.widget_configuration, null);

				final Context context = new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light);

				final AlertDialog dialog = new AlertDialog.Builder(context)
					.setView(dialogView)
					.show();

				final ListView lv1 = (ListView) dialogView.findViewById(R.id.listview1); // servers
				final ListView lv2 = (ListView) dialogView.findViewById(R.id.listview2); // plugins
				final ListView lv3 = (ListView) dialogView.findViewById(R.id.listview3); // period
				
				if (muninFoo.getServers().size() == 0) {
					Toast.makeText(this, R.string.text37, Toast.LENGTH_SHORT).show();
					dialog.dismiss();
					finish();
				}
				if (!muninFoo.premium) {
					Toast.makeText(this, "Munin for Android features pack needed", Toast.LENGTH_SHORT).show();
					dialog.dismiss();
					finish();
				}
				
				ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
				list.clear();
				HashMap<String,String> item;
				for(MuninServer server : muninFoo.getOrderedServers()) {
					item = new HashMap<String,String>();
					item.put("line1", server.getName());
					item.put("line2", server.getServerUrl());
					list.add(item);
				}
				SimpleAdapter sa = new SimpleAdapter(context, list, R.layout.servers_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
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
						for (MuninPlugin plugin : plugins) {
							item2 = new HashMap<String,String>();
							item2.put("line1", plugin.getFancyName());
							item2.put("line2", plugin.getName());
							list2.add(item2);
						}
						SimpleAdapter sa2 = new SimpleAdapter(context, list2, R.layout.plugins_list,
								new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
						lv2.setAdapter(sa2);
						
						lv2.setOnItemClickListener(new OnItemClickListener() {
							public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
								String pluginName = ((TextView) view.findViewById(R.id.line_b)).getText().toString();

								for (MuninPlugin p : selectedServer.getPlugins()) {
									if (p.getName().equals(pluginName))
										graphWidget.setPlugin(p);
								}
								
								lv2.setVisibility(View.GONE);
								
								ArrayList<HashMap<String,String>> list3 = new ArrayList<HashMap<String,String>>();

								String[] periods = { getString(R.string.text47_1), getString(R.string.text47_2),
										getString(R.string.text47_3), getString(R.string.text47_4) };
								for (String str : periods) {
									HashMap<String,String> item3 = new HashMap<String,String>();
									item3.put("line1", str);
									list3.add(item3);
								}
								SimpleAdapter sa3 = new SimpleAdapter(context, list3, R.layout.widget_periodselection,
										new String[] { "line1" }, new int[] {R.id.line_a});
								lv3.setAdapter(sa3);
								lv3.setOnItemClickListener(new OnItemClickListener() {
									public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
										String[] periods = {"day", "week", "month", "year" };
										graphWidget.setPeriod(periods[position]);
										
										LinearLayout ll = (LinearLayout) dialogView.findViewById(R.id.final_instructions);
										lv3.setVisibility(View.GONE);
										ll.setVisibility(View.VISIBLE);
										ll.requestFocus();
										final CheckBox cb = (CheckBox) dialogView.findViewById(R.id.checkbox_wifi);
										final CheckBox cb2 = (CheckBox) dialogView.findViewById(R.id.checkbox_hidetitle);
										Button btn = (Button) dialogView.findViewById(R.id.save);
										
										btn.setOnClickListener(new View.OnClickListener() {
											public void onClick(View v) {
												// Save & close
												graphWidget.setWifiOnly(cb.isChecked());
												graphWidget.setHideServerName(cb2.isChecked());
												
												muninFoo.sqlite.dbHlpr.insertGraphWidget(graphWidget);

												configureWidget(getApplicationContext());
												
												// Make sure we pass back the original appWidgetId before closing the activity
												Intent resultValue = new Intent();
												resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
												setResult(RESULT_OK, resultValue);
												dialog.dismiss();
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
		Widget_GraphWidget_WidgetProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId, true);
	}
}