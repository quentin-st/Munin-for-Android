package com.chteuchteu.munin.async;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.NetHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.ui.Activity_Notifications;
import com.chteuchteu.munin.ui.Activity_Servers;

import java.util.ArrayList;
import java.util.List;

public class Notifications_SendInstructionsByMail extends AsyncTask<Void, Integer, Void> {
	private Context context;
	private String email;
	private String regId;
	private String userAgent;
	private ProgressDialog dialog;

	public Notifications_SendInstructionsByMail(Context context, String email, String regId, String userAgent) {
		this.context = context;
		this.email = email;
		this.regId = regId;
		this.userAgent = userAgent;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		dialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		// Build params list
		List<Pair<String, String>> params = new ArrayList<>();
		params.add(new Pair<>("mailAddress", this.email));
		params.add(new Pair<>("regId", this.regId));

		NetHelper.simplePost(Activity_Notifications.INSTRUCTIONS_EMAIL_TARGET, params, this.userAgent);

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		dialog.dismiss();
		Toast.makeText(context, R.string.notifications_mailSent, Toast.LENGTH_LONG).show();
	}
}
