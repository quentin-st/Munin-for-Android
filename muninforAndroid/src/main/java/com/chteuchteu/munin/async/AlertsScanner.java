package com.chteuchteu.munin.async;

import android.os.AsyncTask;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.ui.IAlertsActivity;

public class AlertsScanner extends AsyncTask<Void, Integer, Void> {
	private int fromIndex;
	private int toIndex;
	private IAlertsActivity activity;

	public AlertsScanner(int fromIndex, int toIndex, IAlertsActivity activity) {
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
		this.activity = activity;
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		MuninFoo muninFoo = MuninFoo.getInstance();

		for (int i=fromIndex; i<=toIndex; i++) {
			muninFoo.getServer(i).fetchPluginsStates(muninFoo.getUserAgent());
			activity.onScanProgress(i);
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		activity.onGroupScanFinished(this.fromIndex, this.toIndex);
	}
}
