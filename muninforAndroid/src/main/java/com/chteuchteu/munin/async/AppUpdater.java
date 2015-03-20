package com.chteuchteu.munin.async;

import android.os.AsyncTask;

import com.chteuchteu.munin.ui.Activity_Main;

public class AppUpdater extends AsyncTask<Void, Integer, Void> {
    private Activity_Main activity;

    public AppUpdater(Activity_Main activity) {
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        activity.updateActions();
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        // When rotating the device while updating : may crash
        if (activity.progressDialog != null && activity.progressDialog.isShowing()) {
            try {
                activity.progressDialog.dismiss();
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        activity.onLoadFinished();
    }
}
