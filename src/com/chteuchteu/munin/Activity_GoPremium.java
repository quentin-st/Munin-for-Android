package com.chteuchteu.munin;

import java.util.Currency;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


@SuppressLint("NewApi")
public class Activity_GoPremium extends Activity {
	private MuninFoo 		muninFoo;
	private DrawerHelper 	dh;
	private Menu			menu;
	private String			activityName;
	
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		setContentView(R.layout.go_premium);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getString(R.string.goPremiumTitle));
			
			if (muninFoo.drawer) {
				dh = new DrawerHelper(this, muninFoo);
				dh.setDrawerActivity(dh.Activity_GoPremium);
			}
		}
		
		TextView price1 = (TextView)findViewById(R.id.price1);
		TextView price2 = (TextView)findViewById(R.id.price2);
		TextView price3 = (TextView)findViewById(R.id.price3);
		Button buyNow = (Button)findViewById(R.id.buyNow);
		WebView benefits = (WebView)findViewById(R.id.benefits);
		
		Currency currency = Currency.getInstance(Locale.getDefault());
		if (currency.toString().equals("EUR")) {
			price1.setText("1");
			price2.setText("49");
			price3.setText("€");
		} else {
			price1.setText("$1");
			price2.setText("49");
		}
		
		benefits.setVerticalScrollBarEnabled(true);
		benefits.setBackgroundColor(0x00000000);
		benefits.getSettings().setDefaultTextEncodingName("utf-8");
		benefits.loadDataWithBaseURL(null, getString(R.string.goPremiumBenefits), "text/html", "utf-8", null);
		// Eviter le clignotement pendant le scroll
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			benefits.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		benefits.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		benefits.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		
		OnClickListener buy = new OnClickListener() {
			@Override
			public void onClick(View actualView) {
				try {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("market://details?id=com.chteuchteu.muninforandroidfeaturespack"));
					startActivity(intent);
				} catch (Exception ex) {
					final AlertDialog ad = new AlertDialog.Builder(Activity_GoPremium.this).create();
					// Error!
					ad.setTitle(getString(R.string.text09));
					ad.setMessage(getString(R.string.text11));
					ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { ad.dismiss(); }
					});
					ad.setIcon(R.drawable.alerts_and_states_error);
					ad.show();
				}
			}
		};
		buyNow.setOnClickListener(buy);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				if (muninFoo.drawer)
					dh.getDrawer().toggle(true);
				else {
					startActivity(new Intent(this, Activity_Main.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					setTransition("shallower");
				}
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_GoPremium.this, Activity_Settings.class));
				setTransition("deeper");
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_GoPremium.this, Activity_About.class));
				setTransition("deeper");
				return true;
			default:	return super.onOptionsItemSelected(item);
		}
	}
	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		if (muninFoo.drawer) {
			dh.getDrawer().setOnOpenListener(new OnOpenListener() {
				@Override
				public void onOpen() {
					activityName = getActionBar().getTitle().toString();
					getActionBar().setTitle("Munin for Android");
					menu.clear();
					getMenuInflater().inflate(R.menu.main, menu);
				}
			});
			dh.getDrawer().setOnCloseListener(new OnCloseListener() {
				@Override
				public void onClose() {
					getActionBar().setTitle(activityName);
					createOptionsMenu();
				}
			});
		}
		createOptionsMenu();
		return true;
	}
	
	private void createOptionsMenu() {
		menu.clear();
		getMenuInflater().inflate(R.menu.alerts, menu);
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		setTransition("shallower");
	}
	
	public String getPref(String key) {
		return this.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	public void setPref(String key, String value) {
		if (value.equals(""))
			removePref(key);
		else {
			SharedPreferences prefs = this.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(key, value);
			editor.commit();
		}
	}
	
	public void removePref(String key) {
		SharedPreferences prefs = this.getSharedPreferences("user_pref", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(key);
		editor.commit();
	}
	
	public void setTransition(String level) {
		if (getPref("transitions").equals("true")) {
			if (level.equals("deeper"))
				overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
			else if (level.equals("shallower"))
				overridePendingTransition(R.anim.shallower_in, R.anim.shallower_out);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!muninFoo.debug)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!muninFoo.debug)
			EasyTracker.getInstance(this).activityStop(this);
	}
}