package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.TagFormat;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;

import java.util.HashMap;
import java.util.Map;


public class Activity_About extends MuninActivity {
	private HashMap<String, String> libraries;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_about);
		super.onContentViewSet();

		actionBar.setTitle(getString(R.string.aboutTitle));

		// Build WebView content
		String html = TagFormat.from(this, R.string.aboutHTMLStructure)
				.with("firstPhrase", R.string.about_firstPhrase)
				.with("independantApp", R.string.about_independantApp)
				.with("changelog", R.string.about_changelog)
				.with("openSource", R.string.about_openSource)
				.with("specialThanksTitle", R.string.about_specialThanksTitle) // Special thanks
				.with("specialThanks", R.string.about_specialThanks) // Special thanks
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
		//wv.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

		TextView tv2 = (TextView) findViewById(R.id.about_txt2);
		TextView userAgent = (TextView) findViewById(R.id.useragent);
		tv2.setText(getString(R.string.app_name) + " " + versionName);
		userAgent.setText(muninFoo.getUserAgent());
	}

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.about, menu);

		menu.findItem(R.id.menu_debug_preferences).setVisible(BuildConfig.DEBUG);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_libraries:
				showLibrariesDialog();
				return true;
			case R.id.menu_debug_preferences:
				debugPreferences();
				return true;
		}

		return true;
	}

	private void buildLibrariesList() {
		if (this.libraries != null)
			return;

		this.libraries = new HashMap<>();
		this.libraries.put("jsoup", "http://jsoup.org/");
		this.libraries.put("Crashlytics", "https://crashlytics.com");
		this.libraries.put("AppRate", "https://github.com/TimotheeJeannin/AppRate");
		this.libraries.put("Floating Action Button", "https://github.com/makovkastar/FloatingActionButton");
		this.libraries.put("Range Bar", "https://github.com/edmodo/range-bar");
		this.libraries.put("PhotoView", "https://github.com/chrisbanes/PhotoView");
		this.libraries.put("Android DB Inspector", "https://github.com/infinum/android_dbinspector");
		this.libraries.put("MaterialDrawer", "https://github.com/mikepenz/MaterialDrawer");
		this.libraries.put("CommunityMaterialTypeface", null);
	}

	private void showLibrariesDialog() {
		buildLibrariesList();

		CharSequence[] libs = Util.stringArrayToCharSequenceArray(libraries.keySet().toArray());

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.about_librariesTitle)
				.setNeutralButton(R.string.close, null)
				.setItems(libs, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						String libraryName = (String) libraries.keySet().toArray()[i];
						String target = libraries.get(libraryName);

						if (target == null)
							return;

						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
						startActivity(browserIntent);
					}
				})
				.show();
	}

	private void debugPreferences() {
		Map<String, ?> allPrefs = settings.getAll();

		CharSequence[] prefs = new CharSequence[allPrefs.size()];
		int i=0;
		for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
			prefs[i] = entry.getKey() + ": " + (entry.getValue() == null ? "null" : entry.getValue().toString());
			i++;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setNeutralButton(R.string.close, null)
				.setItems(prefs, null)
				.show();
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.About; }
	
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
