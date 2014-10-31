package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;


public class Activity_About extends MuninActivity {
	
	@SuppressWarnings("deprecation")
	@SuppressLint("DefaultLocale")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.about);
		super.onContentViewSet();
		dh.setDrawerActivity(DrawerHelper.Activity_About);

		actionBar.setTitle(getString(R.string.aboutTitle));

		WebView wv = (WebView)findViewById(R.id.webView1);
		wv.setVerticalScrollBarEnabled(true);
		wv.getSettings().setDefaultTextEncodingName("utf-8");
		wv.setBackgroundColor(0x00000000);
		String content = getString(R.string.aboutText);
		String versionName = muninFoo.getAppVersion(this);
		content = content.replaceAll("#version#", versionName);
		wv.loadDataWithBaseURL(null, content, "text/html", "utf-8", null);
		wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		wv.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		
		TextView tv1 = (TextView) findViewById(R.id.about_txt1);
		TextView tv2 = (TextView) findViewById(R.id.about_txt2);
		Util.Fonts.setFont(this, tv1, CustomFont.RobotoCondensed_Regular);
		Util.Fonts.setFont(this, tv2, CustomFont.RobotoCondensed_Regular);
		tv1.setText(tv1.getText().toString().toUpperCase());
		tv2.setText(getString(R.string.app_name) + " " + versionName);
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
	}
}