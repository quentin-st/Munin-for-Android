package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.TagFormat;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;

import java.util.Currency;
import java.util.Locale;


public class Activity_GoPremium extends MuninActivity {
	
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_go_premium);
		super.onContentViewSet();
		dh.setDrawerActivity(this);

		actionBar.setTitle(getString(R.string.goPremiumTitle));
		
		TextView price1 = (TextView)findViewById(R.id.price1);
		TextView price2 = (TextView)findViewById(R.id.price2);
		TextView price3 = (TextView)findViewById(R.id.price3);
		Button buyNow = (Button)findViewById(R.id.buyNow);
		WebView benefits = (WebView)findViewById(R.id.benefits);

		String cur;
		try {
			Currency currency = Currency.getInstance(Locale.getDefault());
			cur = currency.toString();
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			try {
				Currency currency = Currency.getInstance(Locale.ENGLISH);
				cur = currency.toString();
			} catch (IllegalArgumentException ex2) {
				ex2.printStackTrace();
				cur = "";
			}
		}

		if (cur.equals("EUR")) {
			price1.setText("1");
			price2.setText("49");
			price3.setText("€");
		} else {
			price1.setText("$1");
			price2.setText("49");
		}

		// Generate HTML content
		String html = TagFormat.from(this, R.string.goPremiumHTMLStructure)
				.with("title", R.string.goPremium_title)
				.with("features", R.string.goPremium_features)
				.format();
		
		benefits.setVerticalScrollBarEnabled(true);
		benefits.setBackgroundColor(0x00000000);
		benefits.getSettings().setDefaultTextEncodingName("utf-8");
		benefits.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
		benefits.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		benefits.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		benefits.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		
		buyNow.setOnClickListener(new OnClickListener() {
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
					ad.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { ad.dismiss(); }
					});
					ad.setIcon(R.drawable.alerts_and_states_error);
					ad.show();
				}
			}
		});
		
		Util.Fonts.setFont(this, price1, CustomFont.RobotoCondensed_Regular);
		Util.Fonts.setFont(this, price2, CustomFont.RobotoCondensed_Regular);
		Util.Fonts.setFont(this, price3, CustomFont.RobotoCondensed_Regular);
		Util.Fonts.setFont(this, buyNow, CustomFont.RobotoCondensed_Regular);
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Premium; }
	
	@Override
	public void onBackPressed() {
        if (dh.closeDrawerIfOpen())
            return;

        Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
	}
}