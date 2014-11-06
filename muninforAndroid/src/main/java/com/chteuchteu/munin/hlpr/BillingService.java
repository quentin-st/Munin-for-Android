package com.chteuchteu.munin.hlpr;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.chteuchteu.munin.R;

public class BillingService {
	private static BillingService instance;
	
	private IInAppBillingService mService;
	private ServiceConnection mServiceConn;
	private Context activityContext;
	
	private boolean isBound = false;
	
	private static final int REQUEST_CODE = 1664;
	
	public static final String DONATE_1 = "donate_1";
	public static final String DONATE_2 = "donate_2";
	public static final String DONATE_5 = "donate_5";
	public static final String DONATE_20 = "donate_20";
	
	private ProgressDialog progressDialog;
	private String productToBuy;
	
	private BillingService(Context activityContext) {
		loadInstance(activityContext);
	}
	
	private BillingService(Context activityContext, String product, ProgressDialog progressDialog) {
		this.productToBuy = product;
		this.progressDialog = progressDialog;
		loadInstance(activityContext);
	}
	
	private void loadInstance(final Context activityContext) {
		if (activityContext != null && this.activityContext == null)
			this.activityContext = activityContext;
		
		mServiceConn = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				mService = null;
			}
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mService = IInAppBillingService.Stub.asInterface(service);
				
				// Service connected : we can now check if the user has purchased smth for example
				launchPurchase(productToBuy);
				
				unbind();
			}
		};
		isBound = activityContext.bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), mServiceConn,
				Context.BIND_AUTO_CREATE);
	}
	
	private void launchPurchase(String product) {
		progressDialog.dismiss();
		
		try {
			Bundle buyIntentBundle = mService.getBuyIntent(3, activityContext.getPackageName(), product, "inapp", "");
			PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
			((Activity) activityContext).startIntentSenderForResult(pendingIntent.getIntentSender(),
					REQUEST_CODE, new Intent(), 0, 0, 0);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void displayErrorToast() {
		Toast.makeText(activityContext, R.string.text09, Toast.LENGTH_SHORT).show();
	}
	
	private void unbind() {
		if (mService != null && isBound) {
			try {
				isBound = false;
				activityContext.unbindService(mServiceConn);
			} catch (Exception ex) { ex.printStackTrace(); }
		}
	}
	
	public boolean isBound() { return this.isBound; }
	public static boolean isLoaded() { return instance != null; }
	public static BillingService getInstance() { return instance; }
	private void setProductToBuy(String val) { this.productToBuy = val; }
	private void setProgressDialog(ProgressDialog val) { this.progressDialog = val; }
	
	public static synchronized BillingService getInstance(Context activityContext) {
		if (instance == null)
			instance = new BillingService(activityContext);
		return instance;
	}
	
	public static synchronized BillingService getInstanceAndPurchase(Context activityContext, String product,
			ProgressDialog progressDialog) {
		if (instance == null)
			instance = new BillingService(activityContext, product, progressDialog);
		else {
			// Instance already defined : just have to loadInstance again
			instance.setProductToBuy(product);
			instance.setProgressDialog(progressDialog);
			instance.loadInstance(activityContext);
		}
		return instance;
	}
}