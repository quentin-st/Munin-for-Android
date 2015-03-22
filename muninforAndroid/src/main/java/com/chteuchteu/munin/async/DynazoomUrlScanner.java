package com.chteuchteu.munin.async;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.ui.Activity_GraphView;

public class DynazoomUrlScanner extends AsyncTask<Void, Integer, Void> {
	private MuninFoo muninFoo;
	private Activity_GraphView activity;
	private ProgressDialog dialog;
	private Context context;
	private MuninMaster master;

	public DynazoomUrlScanner(Activity_GraphView activity, MuninMaster master, Context context) {
		this.muninFoo = MuninFoo.getInstance();
		this.activity = activity;
		this.master = master;
		this.context = context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		dialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		master.rescan(context, muninFoo);

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (dialog != null && dialog.isShowing()) {
			try {
				dialog.dismiss();
			} catch (Exception ex) { ex.printStackTrace(); }
		}

		if (master.isDynazoomAvailable() == MuninMaster.DynazoomAvailability.FALSE) {
			activity.fab.hide(true);
			activity.isFabShown = false;
		}

		activity.actionRefresh();
	}
}
