package com.chteuchteu.munin.async;

import android.os.AsyncTask;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.ui.Fragment_Alerts;

public class AlertsScanner extends AsyncTask<Void, Integer, Void> {
	private int fromIndex;
	private int toIndex;
	private Fragment_Alerts fragment;

	public AlertsScanner(int fromIndex, int toIndex, Fragment_Alerts fragment) {
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
		this.fragment = fragment;
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		MuninFoo muninFoo = MuninFoo.getInstance();

		for (int i=fromIndex; i<=toIndex; i++) {
			muninFoo.getNode(i).fetchPluginsStates(muninFoo.getUserAgent());
			fragment.onScanProgress();
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		fragment.onGroupScanFinished(this.fromIndex, this.toIndex);
	}
}
