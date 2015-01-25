package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.TagFormat;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;


public class Activity_About extends MuninActivity {
	
	@SuppressWarnings("deprecation")
	@SuppressLint("DefaultLocale")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_about);
		super.onContentViewSet();
		dh.setDrawerActivity(this);

		actionBar.setTitle(getString(R.string.aboutTitle));

		// Build WebView content
		String html = TagFormat.from(this, R.string.aboutHTMLStructure)
				.with("firstPhrase", R.string.about_firstPhrase)
				.with("independantApp", R.string.about_independantApp)
				.with("changelog", R.string.about_changelog)
				.with("openSource", R.string.about_openSource)
				.with("specialThanksTitle", R.string.about_specialThanksTitle) // Special thanks
				.with("specialThanks", R.string.about_specialThanks) // Special thanks
				.with("librariesTitle", R.string.about_librariesTitle) // Libraries
				.with("libraries", R.string.about_libraries) // Libraries
				.format();

		WebView wv = (WebView)findViewById(R.id.webView1);
		wv.setVerticalScrollBarEnabled(true);
		wv.getSettings().setDefaultTextEncodingName("utf-8");
		wv.setBackgroundColor(0x00000000);
		String versionName = Util.getAppVersion(this);
		html = html.replaceAll("#version#", versionName);
		wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
		wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		wv.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		
		TextView tv1 = (TextView) findViewById(R.id.about_txt1);
		TextView tv2 = (TextView) findViewById(R.id.about_txt2);
		TextView userAgent = (TextView) findViewById(R.id.useragent);
		TextView userAgent_label = (TextView) findViewById(R.id.useragent_label);
		Util.Fonts.setFont(this, tv1, CustomFont.Roboto_Regular);
		Util.Fonts.setFont(this, tv2, CustomFont.Roboto_Regular);
		Util.Fonts.setFont(this, userAgent, CustomFont.Roboto_Regular);
		Util.Fonts.setFont(this, userAgent_label, CustomFont.Roboto_Medium);
		tv2.setText(getString(R.string.app_name) + " " + versionName);
		userAgent.setText(muninFoo.getUserAgent());
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
	}
}
