package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninPlugin.AlertState;


@SuppressLint("InflateParams")
public class Activity_AlertsPlugins extends MuninActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_alerts_pluginselection);
		super.onContentViewSet();

        actionBar.setTitle(getString(R.string.alertsTitle));
		actionBar.setSubtitle(muninFoo.getCurrentNode().getName());
		
		for (MuninPlugin plugin : muninFoo.getCurrentNode().getPlugins()) {
			if (plugin.getState() == AlertState.WARNING || plugin.getState() == AlertState.CRITICAL) {
				LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View v = vi.inflate(R.layout.plugins_list_dark, null);
				
				LinearLayout part = (LinearLayout)v.findViewById(R.id.pluginselection_part_ll);
				TextView line_a = (TextView)v.findViewById(R.id.line_a);
				TextView line_b = (TextView)v.findViewById(R.id.line_b);
				
				line_a.setText(plugin.getFancyName());
				line_b.setText(plugin.getName());
				
				if (plugin.getState() == AlertState.WARNING)
					part.setBackgroundColor(context.getResources().getColor(R.color.alerts_bg_color_warning));
				else if (plugin.getState() == AlertState.CRITICAL)
					part.setBackgroundColor(context.getResources().getColor(R.color.alerts_bg_color_critical));
				
				final int indexOfPlugin = muninFoo.getCurrentNode().getPlugins().indexOf(plugin);
				
				part.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						String pluginName = ((TextView)v.findViewById(R.id.line_b)).getText().toString();
						
						Intent i = new Intent(Activity_AlertsPlugins.this, Activity_GraphView.class);
						i.putExtra("plugin", pluginName);
						i.putExtra("position", indexOfPlugin);
						i.putExtra("node", muninFoo.getCurrentNode().getUrl());
						i.putExtra("from", "alerts");
						startActivity(i);
						Util.setTransition(activity, TransitionStyle.DEEPER);
					}
				});
				
				ViewGroup insertPoint = (ViewGroup) findViewById(R.id.alerts_pluginselection_inserthere);
				insertPoint.addView(v);
			}
		}
	}
	
	@Override
	public void onBackPressed() {
        if (drawerHelper.closeDrawerIfOpen())
            return;

        Intent intent = new Intent(this, Activity_Alerts.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
	}
}
