package com.chteuchteu.munin.async;

import android.os.AsyncTask;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_GraphView;

public class DynazoomDetector extends AsyncTask<Void, Integer, Void> {
    private Activity_GraphView activity;
    private MuninServer server;
    private boolean dynazoomAvailable;

    public DynazoomDetector (Activity_GraphView activity, MuninServer server) {
        this.activity = activity;
        this.server = server;
        this.dynazoomAvailable = false;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        dynazoomAvailable = server.getParent().isDynazoomAvailable(MuninFoo.getInstance().getUserAgent());
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        server.getParent().setDynazoomAvailable(MuninMaster.DynazoomAvailability.get(dynazoomAvailable));
        MuninFoo.getInstance().sqlite.dbHlpr.updateMuninMaster(server.getParent());
        activity.loadGraphs = true;
        activity.actionRefresh();
    }
}
