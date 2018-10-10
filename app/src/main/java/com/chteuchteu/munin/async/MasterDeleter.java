package com.chteuchteu.munin.async;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.ui.Activity_Servers;

public class MasterDeleter extends AsyncTask<Void, Integer, Void> {
	private Context context;
	private Activity_Servers activity;
	private MuninFoo muninFoo;

	private ProgressDialog dialog;
	private MuninMaster toBeDeleted;
	private Util.ProgressNotifier progressNotifier;

	public MasterDeleter(MuninMaster master, Activity_Servers activity) {
		this.toBeDeleted = master;
		this.context = activity;
		this.activity = activity;
		this.muninFoo = MuninFoo.getInstance();
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		dialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
		this.progressNotifier = new Util.ProgressNotifier() {
			@Override
			public void notify(final int progress, final int total) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dialog.setMessage(context.getString(R.string.loading) + " " + progress + "/" + total);
					}
				});
			}
		};
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		muninFoo.deleteMuninMaster(toBeDeleted, progressNotifier);

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		dialog.dismiss();

		activity.refreshList();
		activity.updateDrawerIfNeeded();
	}
}
