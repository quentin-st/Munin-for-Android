package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_Alerts;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.Calendar;

/**
 * Since using a listView for alert parts would be too tricky,
 *  we're copying the way adapter works (using a getView method)
 */
public class Activity_Alerts extends MuninActivity {
	private MenuItem		menu_flatList;
	private View			everythingsOk;

	private TextView        tv_hideNoAlerts;
	private Adapter_Alerts  adapter;
	
	private Handler		    mHandler;
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
		dh.setDrawerActivity(this);
		actionBar.setTitle(getString(R.string.alertsTitle));
		progressBar = Util.UI.prepareGmailStyleProgressBar(this, actionBar);
		everythingsOk = findViewById(R.id.alerts_ok);
		tv_hideNoAlerts = (TextView) findViewById(R.id.hideNoAlerts);

		adapter = new Adapter_Alerts(this, muninFoo.getServers(),
				Adapter_Alerts.ListItemSize.EXPANDED, Adapter_Alerts.ListItemPolicy.HIDE_NORMAL);

		// Build layout
		ViewGroup insertPoint = (ViewGroup) findViewById(R.id.alerts_insertPoint);

		for (MuninServer server : muninFoo.getServers()) {
			View view = adapter.getView(muninFoo.getServers().indexOf(server), insertPoint);
			insertPoint.addView(view);
		}
		
		// If coming from PluginSelection : don't check again
		// TODO : list isn't inflated yet...
		/*Intent thisIntent = getIntent();
		if (thisIntent.hasExtra("dontCheckAgain") && thisIntent.getExtras().getBoolean("dontCheckAgain"))
			updateStates(false);
		else {
			if (muninFoo.shouldUpdateAlerts())
				updateStates(true);
			else
				updateStates(false);
		}*/

		tv_hideNoAlerts.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (adapter.getListItemPolicy() == Adapter_Alerts.ListItemPolicy.HIDE_NORMAL) {
					// => show all servers
					adapter.setListItemPolicy(Adapter_Alerts.ListItemPolicy.SHOW_ALL);
					tv_hideNoAlerts.setText(getString(R.string.text49_1));
					adapter.updateViewsPartial();
					// TODO if shouldDisplayEverythingsOk
				} else {
					// => hide normal servers
					adapter.setListItemPolicy(Adapter_Alerts.ListItemPolicy.HIDE_NORMAL);
					tv_hideNoAlerts.setText(getString(R.string.text49_2));
					adapter.updateViewsPartial();
					// TODO if shouldDisplayEverythingsOk
				}
			}
		});
		
		// Launch periodical check
		if (Util.getPref(this, Util.PrefKeys.AutoRefresh).equals("true")) {
			mHandler = new Handler();
			final int INTERVAL = 1000 * 60 * 5;
			mHandlerTask = new Runnable() {
				@Override 
				public void run() {
					refresh(true);
					mHandler.postDelayed(mHandlerTask, INTERVAL);
				}
			};
			mHandlerTask.run();
		}

		if (Util.getPref(this, Util.PrefKeys.ScreenAlwaysOn).equals("true"))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	/**
	 * Update UI
	 * @param fetch Use cached data or not
	 */
	private void refresh(boolean fetch) {
		if (fetch && !Util.isOnline(this)) {
			Toast.makeText(this, getString(R.string.text30), Toast.LENGTH_LONG).show();
			return;
		}

		tv_hideNoAlerts.setEnabled(false);
		tv_hideNoAlerts.setBackgroundColor(Color.GRAY);
		adapter.setAllGray();

		int nbServers = muninFoo.getServers().size();
		if (fetch) {
			currentLoadingProgress = 0;
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setProgress(0);
			everythingsOk.setVisibility(View.GONE);

			for (int i=0; i<nbServers; i++) {
				if (i%SERVERS_BY_THREAD == 0) {
					int to = i + 2;
					if (to >= nbServers)
						to = nbServers-1;

					//                          Avoid serial execution
					new AlertsFetcher(i, to).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			}
			muninFoo.alerts_lastUpdated = Calendar.getInstance();
		} else
			adapter.updateViews();
	}
	
	private class AlertsFetcher extends AsyncTask<Void, Integer, Void> {
		private int fromIndex;
		private int toIndex;
		
		private AlertsFetcher(int fromIndex, int toIndex) {
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			for (int i=fromIndex; i<=toIndex; i++) {
				muninFoo.getServer(i).fetchPluginsStates(muninFoo.getUserAgent());
				currentLoadingProgress++;
				progressBar.setProgress(currentLoadingProgress*100/muninFoo.getServers().size());
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			adapter.updateViews(fromIndex, toIndex);

			// TODO this is bad
			if (this.toIndex == muninFoo.getServers().size()-1)
				progressBar.setVisibility(View.GONE);
		}
	}
	
	/**
	 * Switchs from flat to expanded list mode
	 */
	private void switchListMode() {
		adapter.setListItemSize(
				adapter.getListItemSize() == Adapter_Alerts.ListItemSize.REDUCED
						? Adapter_Alerts.ListItemSize.EXPANDED
						: Adapter_Alerts.ListItemSize.REDUCED);
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
				refresh(true);
				return true;
		}

		return true;
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Alerts; }

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		if (Util.getPref(this, Util.PrefKeys.ScreenAlwaysOn).equals("true"))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
