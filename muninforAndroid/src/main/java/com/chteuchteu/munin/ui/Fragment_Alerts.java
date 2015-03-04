package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_Alerts;
import com.chteuchteu.munin.async.AlertsScanner;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.Calendar;

public class Fragment_Alerts extends Fragment {
	private Context         context;
	private IAlertsActivity activity;
	private MuninFoo        muninFoo;

	private View            everythingsOk;
	private TextView        tv_hideNoAlerts;
	private Adapter_Alerts  adapter;

	private Handler         mHandler;
	private Runnable		 mHandlerTask;

	private int 			 currentLoadingProgress;
	private boolean        loading;

	private static final int SERVERS_BY_THREAD = 3;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.context = activity;
		this.activity = (IAlertsActivity) activity;
		this.muninFoo = MuninFoo.getInstance();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_alerts, container, false);

		everythingsOk = view.findViewById(R.id.alerts_ok);
		tv_hideNoAlerts = (TextView) view.findViewById(R.id.hideNoAlerts);
		loading = false;

		adapter = new Adapter_Alerts(context, muninFoo.getServers(),
				Adapter_Alerts.ListItemSize.EXPANDED, Adapter_Alerts.ListItemPolicy.HIDE_NORMAL);

		// Build layout
		ViewGroup insertPoint = (ViewGroup) view.findViewById(R.id.alerts_insertPoint);

		for (MuninServer server : muninFoo.getServers())
			insertPoint.addView(adapter.getView(muninFoo.getServers().indexOf(server), insertPoint));

		refresh(muninFoo.shouldUpdateAlerts());

		tv_hideNoAlerts.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				switchListItemPolicy();
				switch (adapter.getListItemPolicy()) {
					case HIDE_NORMAL:
						adapter.setListItemPolicy(Adapter_Alerts.ListItemPolicy.HIDE_NORMAL);
						tv_hideNoAlerts.setText(getString(R.string.text49_2));
						adapter.updateViewsPartial();
						everythingsOk.setVisibility(adapter.isEverythingOk() ? View.VISIBLE : View.GONE);
						break;
					case SHOW_ALL:
						adapter.setListItemPolicy(Adapter_Alerts.ListItemPolicy.SHOW_ALL);
						tv_hideNoAlerts.setText(getString(R.string.text49_1));
						adapter.updateViewsPartial();
						everythingsOk.setVisibility(View.GONE);
						break;
				}
			}
		});

		// Launch periodical check
		if (Util.getPref(context, Util.PrefKeys.AutoRefresh).equals("true")) {
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

		return view;
	}

	public void onLoadingFinished() {
		loading = false;
		activity.setLoading(false);

		everythingsOk.setVisibility(
				adapter.shouldDisplayEverythingsOkMessage() ? View.VISIBLE : View.GONE);
	}

	public void onScanProgress() {
		currentLoadingProgress++;
		activity.setLoadingProgress(currentLoadingProgress * 100 / muninFoo.getServers().size());
	}

	public void onGroupScanFinished(int fromIndex, int toIndex) {
		adapter.updateViews(fromIndex, toIndex);

		if (currentLoadingProgress == muninFoo.getServers().size())
			onLoadingFinished();
	}

	/**
	 * Update UI
	 * @param fetch Use cached data or not
	 */
	public void refresh(boolean fetch) {
		if (loading) {
			MuninFoo.logW("Fragment_Alerts.refresh(" + fetch + ")", "Alerts is currently loading, return");
			return;
		}
		loading = true;

		if (fetch && !Util.isOnline(context)) {
			Toast.makeText(context, getString(R.string.text30), Toast.LENGTH_LONG).show();
			loading = false;
			return;
		}

		adapter.setAllGray();

		int nbServers = muninFoo.getServers().size();
		if (fetch) {
			activity.setLoading(true);
			activity.setLoadingProgress(0);
			everythingsOk.setVisibility(View.GONE);
			currentLoadingProgress = 0;

			for (int i=0; i<nbServers; i++) {
				if (i%SERVERS_BY_THREAD == 0) {
					int to = i + 2;
					if (to >= nbServers)
						to = nbServers-1;

					AlertsScanner scanner = new AlertsScanner(i, to, this);
					//      Avoid serial execution
					scanner.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			}
			muninFoo.alerts_lastUpdated = Calendar.getInstance();
		} else {
			adapter.updateViews();
			loading = false;
		}
	}

	/**
	 * Switch from flat to expanded list mode
	 */
	public void switchListMode() {
		adapter.setListItemSize(
				adapter.getListItemSize() == Adapter_Alerts.ListItemSize.REDUCED
						? Adapter_Alerts.ListItemSize.EXPANDED
						: Adapter_Alerts.ListItemSize.REDUCED);
	}

	private void switchListItemPolicy() {
		adapter.setListItemPolicy(
				adapter.getListItemPolicy() == Adapter_Alerts.ListItemPolicy.HIDE_NORMAL
						? Adapter_Alerts.ListItemPolicy.SHOW_ALL
						: Adapter_Alerts.ListItemPolicy.HIDE_NORMAL);
	}
}
