package com.chteuchteu.munin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;

/**
 * Since using a listView for alert parts would be too tricky,
 *  we're copying the way adapter works (using a getView method)
 */
public class Activity_Alerts extends MuninActivity implements IAlertsActivity {
	private Fragment_Alerts fragment;
	private ProgressBar progressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_alerts);
		super.onContentViewSet();
		dh.setDrawerActivity(this);
		actionBar.setTitle(getString(R.string.alertsTitle));
		progressBar = Util.UI.prepareGmailStyleProgressBar(this, actionBar);

		fragment = new Fragment_Alerts();
		getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();

		if (Util.getPref(this, Util.PrefKeys.ScreenAlwaysOn).equals("true"))
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public void setLoading(boolean val) {
		this.progressBar.setVisibility(val ? View.VISIBLE : View.GONE);
	}

	@Override
	public void setLoadingProgress(int progress) {
		this.progressBar.setProgress(progress);
	}

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.alerts, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_flatlist:
				fragment.switchListMode();
				return true;
			case R.id.menu_refresh:
				fragment.refresh(true);
				return true;
		}

		return true;
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Alerts; }

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		if (Util.getPref(this, Util.PrefKeys.ScreenAlwaysOn).equals("true"))
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
