package com.chteuchteu.munin.async;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.ui.Activity_Servers;

public class MasterScanner extends AsyncTask<Void, Integer, Void> {
	private MuninFoo muninFoo;
	private Activity_Servers activity;
	private Context context;

	private ProgressDialog dialog;
	private MuninMaster original;
	private String report;

	public MasterScanner(Activity_Servers activity, MuninMaster master) {
		this.original = master;
		this.context = activity;
		this.activity = activity;
		this.muninFoo = MuninFoo.getInstance(context);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		dialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		report = original.rescan(context, muninFoo);

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (dialog != null && dialog.isShowing()) {
			try {
				dialog.dismiss();
			} catch (Exception ex) { ex.printStackTrace(); }

			new AlertDialog.Builder(activity)
					.setTitle(R.string.sync_reporttitle)
					.setMessage(report)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// if "No change" => don't reload nodes list
							if (!report.equals(context.getString(R.string.sync_nochange))) {
								activity.refreshList();
								activity.updateDrawerIfNeeded();
							}
						}
					})
					.show();
		}
	}
}
