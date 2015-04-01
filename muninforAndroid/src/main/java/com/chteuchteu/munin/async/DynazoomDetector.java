package com.chteuchteu.munin.async;

import android.os.AsyncTask;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.ui.Activity_GraphView;

public class DynazoomDetector extends AsyncTask<Void, Integer, Void> {
    private Activity_GraphView activity;
    private MuninNode node;
    private boolean dynazoomAvailable;

    public DynazoomDetector (Activity_GraphView activity, MuninNode node) {
        this.activity = activity;
        this.node = node;
        this.dynazoomAvailable = false;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        dynazoomAvailable = node.getParent().isDynazoomAvailable(MuninFoo.getInstance().getUserAgent());
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        node.getParent().setDynazoomAvailable(MuninMaster.DynazoomAvailability.get(dynazoomAvailable));
        MuninFoo.getInstance().sqlite.dbHlpr.updateMuninMaster(node.getParent());
        activity.loadGraphs = true;
        activity.actionRefresh();
    }
}
