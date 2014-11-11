package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.SpecialBool;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.Calendar;
import java.util.List;

public class Activity_Alerts extends MuninActivity {
	private boolean		hideNormalStateServers;
	private MenuItem		menu_flatList;
	/* If the menu items are flat / expanded */
	private boolean		listMode_flat;
	private boolean		shouldDisplayEverythingsOk;
	private View			everythingsOk;
	
	private LinearLayout[]		part_part;
	private TextView[] 		part_serverName;
	private LinearLayout[] 	part_criticals;
	private TextView[] 		part_criticalsNumber;
	private TextView[] 		part_criticalsLabel;
	private TextView[]			part_criticalsPluginsList;
	private LinearLayout[] 	part_warnings;
	private TextView[] 		part_warningsNumber;
	private TextView[] 		part_warningsLabel;
	private TextView[]			part_warningsPluginsList;
	
	private static final String	BG_COLOR_UNDEFINED = "#B2B2B2";
	private static final String	BG_COLOR_OK = "#8EC842";
	public static final String	BG_COLOR_WARNING = "#FFAE5B";
	public static final String	BG_COLOR_CRITICAL = "#FF7B68";
	private static final String	TEXT_COLOR = "#333333";
	
	private Handler		mHandler;
	private Runnable		mHandlerTask;
	
	private ProgressBar 	progressBar;
	private int 			currentLoadingProgress;
	
	private static final int SERVERS_BY_THREAD = 3;

	@SuppressLint("InflateParams")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_alerts);
		super.onContentViewSet();
		dh.setDrawerActivity(DrawerHelper.Activity_Alerts);

		listMode_flat = false;

		actionBar.setTitle(getString(R.string.alertsTitle));

		progressBar = Util.UI.prepareGmailStyleProgressBar(this, actionBar);
		
		if (Util.getPref(this, "screenAlwaysOn").equals("true"))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		everythingsOk = findViewById(R.id.alerts_ok);
		
		int nbS = muninFoo.getServers().size();
		part_part 					= new LinearLayout[nbS];
		part_serverName 			= new TextView[nbS];
		part_criticals 				= new LinearLayout[nbS];
		part_criticalsNumber 		= new TextView[nbS];
		part_criticalsLabel 		= new TextView[nbS];
		part_criticalsPluginsList 	= new TextView[nbS];
		part_warnings 				= new LinearLayout[nbS];
		part_warningsNumber 		= new TextView[nbS];
		part_warningsLabel 			= new TextView[nbS];
		part_warningsPluginsList 	= new TextView[nbS];
		
		hideNormalStateServers = true;

		LinearLayout wholeContainer = new LinearLayout(this);
		wholeContainer.setOrientation(LinearLayout.VERTICAL);
		
		for (int i=0; i<muninFoo.getServers().size(); i++) {
			MuninServer server = muninFoo.getServer(i);

			LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			View v = vi.inflate(R.layout.alerts_part, null);
			
			part_part[i] 					= (LinearLayout) v.findViewById(R.id.alerts_part);
			part_serverName[i] 				= (TextView) v.findViewById(R.id.alerts_part_serverName);
			part_criticals[i] 				= (LinearLayout) v.findViewById(R.id.alerts_part_criticals);
			part_criticalsNumber[i] 		= (TextView) v.findViewById(R.id.alerts_part_criticalsNumber);
			part_criticalsLabel[i] 			= (TextView) v.findViewById(R.id.alerts_part_criticalsLabel);
			part_criticalsPluginsList[i] 	= (TextView) v.findViewById(R.id.alerts_part_criticalsPluginsList);
			part_warnings[i] 				= (LinearLayout) v.findViewById(R.id.alerts_part_warnings);
			part_warningsNumber[i]			= (TextView) v.findViewById(R.id.alerts_part_warningsNumber);
			part_warningsLabel[i] 			= (TextView) v.findViewById(R.id.alerts_part_warningsLabel);
			part_warningsPluginsList[i] 	= (TextView) v.findViewById(R.id.alerts_part_warningsPluginsList);
			
			part_part[i].setVisibility(View.GONE);
			part_serverName[i].setText(server.getName());

			final int index = i;
			part_serverName[i].setOnClickListener(new OnClickListener() {
				public void onClick (View v) {
					muninFoo.setCurrentServer(muninFoo.getServer(index));
					startActivity(new Intent(Activity_Alerts.this, Activity_AlertsPluginSelection.class));
					Util.setTransition(context, TransitionStyle.DEEPER);
				}
			});
			
			Util.Fonts.setFont(this, (ViewGroup) v, CustomFont.RobotoCondensed_Regular);
			
			wholeContainer.addView(v);
		}
		
		View insertPoint = findViewById(R.id.alerts_root_container);
		((ViewGroup) insertPoint).addView(wholeContainer);
		
		// If coming from PluginSelection : don't check again
		Intent thisIntent = getIntent();
		if (thisIntent.hasExtra("dontCheckAgain") && thisIntent.getExtras().getBoolean("dontCheckAgain"))
			updateStates(false);
		else {
			if (muninFoo.shouldUpdateAlerts())
				updateStates(true);
			else
				updateStates(false);
		}

		findViewById(R.id.hideNoAlerts).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (hideNormalStateServers) { // Show all servers
					hideNormalStateServers = false;
					((TextView) findViewById(R.id.hideNoAlerts)).setText(getString(R.string.text49_1));
					updateView(false);
					if (shouldDisplayEverythingsOk)
						everythingsOk.setVisibility(View.GONE);
				} else {
					hideNormalStateServers = true;
					((TextView) findViewById(R.id.hideNoAlerts)).setText(getString(R.string.text49_2));
					updateView(true);
					if (shouldDisplayEverythingsOk)
						everythingsOk.setVisibility(View.VISIBLE);
				}
			}
		});
		
		// Launch periodical check
		if (Util.getPref(this, "autoRefresh").equals("true")) {
			mHandler = new Handler();
			final int INTERVAL = 1000 * 60 * 5;
			mHandlerTask = new Runnable() {
				@Override 
				public void run() {
					updateStates(true);
					mHandler.postDelayed(mHandlerTask, INTERVAL);
				}
			};
			mHandlerTask.run();
		}
	}
	
	private void updateView(boolean hideNormal, int i) {
		boolean hide = false;
		
		int nbErrors = muninFoo.getServer(i).getErroredPlugins().size();
		int nbWarnings = muninFoo.getServer(i).getWarnedPlugins().size();
		
		if (nbErrors == 0 && nbWarnings == 0) {
			if (hideNormal)
				hide = true;
			part_serverName[i].setClickable(false);
			enableArrow(false, i);
		} else {
			if (hideNormal)
				hide = false;
			
			part_serverName[i].setClickable(true);
			enableArrow(true, i);
		}
		
		if (hide)
			part_part[i].setVisibility(View.GONE);
		else
			part_part[i].setVisibility(View.VISIBLE);
	}
	
	private void updateView(boolean hideNormal) {
		for (int i=0; i<part_part.length; i++)
			updateView(hideNormal, i);
	}
	
	/**
	 * Update UI
	 * @param fetch Use cached data or not
	 */
	private void updateStates(boolean fetch) {
		if (fetch && !Util.isOnline(this)) {
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
			return;
		}
		
		for (int i=0; i<muninFoo.getServers().size(); i++) {
			part_criticals[i].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
			part_warnings[i].setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
		}
		
		int nbServers = muninFoo.getServers().size();
		if (fetch) {
			if (nbServers > 0) {
				currentLoadingProgress = 0;
				progressBar.setVisibility(View.VISIBLE);
				progressBar.setProgress(0);
				shouldDisplayEverythingsOk = true;
				everythingsOk.setVisibility(View.GONE);
			}
			for (int i=0; i<nbServers; i++) {
				if (i%SERVERS_BY_THREAD == 0) {
					int from = i;
					int to = i + 2;
					if (to >= nbServers)
						to = nbServers-1;

					// Avoid serial execution
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
						new AlertsFetcher(from, to).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					else
						new AlertsFetcher(from, to).execute();
				}
			}
			muninFoo.alerts_lastUpdated = Calendar.getInstance();
		} else {
			for (int i=0; i<nbServers; i++)
				updatePart(i);
		}
	}
	
	private class AlertsFetcher extends AsyncTask<Void, Integer, Void> {
		private int fromIndex;
		private int toIndex;
		
		private AlertsFetcher(int fromIndex, int toIndex) {
			log(fromIndex + " AlertsFetcher()");
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			log(fromIndex + " onPreExecute()");
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			log(fromIndex + " doInBackground()");
			
			for (int i=fromIndex; i<=toIndex; i++) {
				muninFoo.getServer(i).fetchPluginsStates(muninFoo.getUserAgent());
				currentLoadingProgress++;
				progressBar.setProgress(currentLoadingProgress*100/muninFoo.getServers().size());
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			log(fromIndex + " onPostExecute()");
			for (int i=fromIndex; i<=toIndex; i++)
				updatePart(i);
			
			if (this.toIndex == muninFoo.getServers().size()-1)
				progressBar.setVisibility(View.GONE);
		}
	}
	
	private void updatePart(int i) {
		MuninServer server = muninFoo.getServer(i);
		int index = muninFoo.getServers().indexOf(server);
		
		LinearLayout part_critical = part_criticals[index];
		LinearLayout part_warning = part_warnings[index];
		TextView part_criticalPluginsList = part_criticalsPluginsList[index];
		TextView part_warningPluginsList = part_warningsPluginsList[index];
		TextView part_criticalNumber = part_criticalsNumber[index];
		TextView part_warningNumber = part_warningsNumber[index];
		TextView part_criticalLabel = part_criticalsLabel[index];
		TextView part_warningLabel = part_warningsLabel[index];
		
		int nbErrors = server.getErroredPlugins().size();
		int nbWarnings = server.getWarnedPlugins().size();
		
		if (server.reachable == SpecialBool.TRUE) {
			if (nbErrors > 0) {
				part_critical.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_CRITICAL));
				// Plugins list
				String toBeShown1 = "";
				List<MuninPlugin> erroredPlugins = server.getErroredPlugins();
				for (MuninPlugin plugin : erroredPlugins) {
					if (erroredPlugins.indexOf(plugin) != erroredPlugins.size()-1)
						toBeShown1 = toBeShown1 + plugin.getFancyName() + ", ";
					else
						toBeShown1 += plugin.getFancyName();
				}
				part_criticalPluginsList.setText(toBeShown1);
			} else
				part_critical.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_OK));
			
			if (nbWarnings > 0) {
				part_warning.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_WARNING));
				// Plugins list
				String toBeShown2 = "";
				List<MuninPlugin> warningPlugins = server.getWarnedPlugins();
				for (MuninPlugin plugin : warningPlugins) {
					if (warningPlugins.indexOf(plugin) != warningPlugins.size()-1)
						toBeShown2 = toBeShown2 + plugin.getFancyName() + ", ";
					else
						toBeShown2 += plugin.getFancyName();
				}
				part_warningPluginsList.setText(toBeShown2);
			} else
				part_warning.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_OK));
			
			part_criticalNumber.setText(nbErrors + "");
			if (nbErrors == 1) // critical
				part_criticalLabel.setText(getString(R.string.text50_1));
			else // criticals
				part_criticalLabel.setText(getString(R.string.text50_2));
			
			part_warningNumber.setText(nbWarnings + "");
			if (nbWarnings == 1) // warning
				part_warningLabel.setText(getString(R.string.text51_1));
			else // warnings
				part_warningLabel.setText(getString(R.string.text51_2));
		}
		else if (server.reachable == SpecialBool.FALSE) {
			part_criticalPluginsList.setText("");
			part_warningPluginsList.setText("");
			part_criticalNumber.setText("?");
			part_warningNumber.setText("?");
			part_criticalLabel.setText(getString(R.string.text50_2));
			part_warningLabel.setText(getString(R.string.text51_2));
			part_critical.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
			part_warning.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
		}
		
		updateView(hideNormalStateServers, i);
		
		if (nbErrors > 0 || nbWarnings > 0)
			shouldDisplayEverythingsOk = false;
		
		if (!hideNormalStateServers)
			shouldDisplayEverythingsOk = false;
		
		// Can't flat the list before the first loading is finished
		if (index == muninFoo.getServers().size()-1) {
			// menu_flatList can be null if onCreateMenu hasn't been called yet
			if (menu_flatList != null)
				menu_flatList.setVisible(true);
			
			if (shouldDisplayEverythingsOk)
				everythingsOk.setVisibility(View.VISIBLE);
			else
				everythingsOk.setVisibility(View.GONE);
		}
	}
	
	/**
	 * Switchs from flat to expanded list mode
	 */
	private void switchListMode() {
		if (this.listMode_flat) {
			// Expand
			for (LinearLayout ll : this.part_criticals)
				ll.setVisibility(View.VISIBLE);
			for (LinearLayout ll : this.part_warnings)
				ll.setVisibility(View.VISIBLE);
			
			for (int i=0; i<this.part_part.length; i++) {
				this.part_serverName[i].setTextColor(Color.parseColor(TEXT_COLOR));
				this.part_serverName[i].setBackgroundColor(Color.WHITE);
			}
			
			this.listMode_flat = false;
		} else {
			// Reduce
			for (LinearLayout ll : this.part_criticals)
				ll.setVisibility(View.GONE);
			for (LinearLayout ll : this.part_warnings)
				ll.setVisibility(View.GONE);
			
			// Set the background color so we can see the server state
			for (int i=0; i<this.part_part.length; i++) {
				String s_CriticalsNumber = this.part_criticalsNumber[i].getText().toString();
				int criticalsNumber = s_CriticalsNumber.equals("?") ? -1 : Integer.parseInt(s_CriticalsNumber);
				String s_WarningsNumber = this.part_warningsNumber[i].getText().toString();
				int warningsNumber = s_WarningsNumber.equals("?") ? -1 : Integer.parseInt(s_WarningsNumber);
				if (criticalsNumber > 0 || warningsNumber > 0) {
					if (criticalsNumber > 0)
						this.part_serverName[i].setBackgroundColor(Color.parseColor(BG_COLOR_CRITICAL));
					else if (warningsNumber > 0)
						this.part_serverName[i].setBackgroundColor(Color.parseColor(BG_COLOR_WARNING));
					
					this.part_serverName[i].setTextColor(Color.WHITE);
				}
			}
			
			this.listMode_flat = true;
		}
	}

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.alerts, menu);
		this.menu_flatList = menu.findItem(R.id.menu_flatlist);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_flatlist:
				switchListMode();
				return true;
			case R.id.menu_refresh:
				updateStates(true);
				return true;
		}

		return true;
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
	}
	
	private void enableArrow(boolean b, int p) {
		if (p >= 0 && p < part_serverName.length) {
			if (b)
				part_serverName[p].setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.arrow, 0);
			else
				part_serverName[p].setCompoundDrawables(null, null, null, null);
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		if (Util.getPref(this, "screenAlwaysOn").equals("true"))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}