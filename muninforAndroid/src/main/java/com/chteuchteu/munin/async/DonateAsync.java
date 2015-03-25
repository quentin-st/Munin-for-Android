package com.chteuchteu.munin.async;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.BillingService;

public class DonateAsync extends AsyncTask<Void, Integer, Void> {
    private ProgressDialog dialog;
    private Context context;
    private String product;

    public DonateAsync(Context context, String product) {
        this.product = product;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        dialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
        dialog.setCancelable(true);
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        BillingService.getInstanceAndPurchase(context, product, dialog);
        // Dialog will be dismissed in the BillingService.

        return null;
    }
}
