package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.async.ServerScanner;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.SampleServers;
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;

import java.util.ArrayList;
import java.util.Arrays;

public class Activity_Server extends MuninActivity {
	public AutoCompleteTextView tv_serverUrl;

    public boolean isAlertShown;

	public MuninMaster master;
	public ServerScanner task;
	public AlertDialog alertDialog;

	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_server);
		super.onContentViewSet();

		actionBar.setTitle(getString(R.string.addServerTitle));


		tv_serverUrl = findViewById(R.id.textbox_serverUrl);

		// Servers history autocomplete
		ArrayAdapter<String> addServerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getHistory());
		tv_serverUrl.setAdapter(addServerAdapter);

		// Trigger save on "Down" or "Enter" keypress
		tv_serverUrl.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
					actionSave();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onBackPressed() {
        if (drawerHelper.closeDrawerIfOpen())
            return;

        // Hitting "back" within the AlertDialog will call its own onBackPressed
		// (which can't be overriden BTW).
		if (!isAlertShown) {
			Intent intent = new Intent(this, Activity_Servers.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(this, TransitionStyle.SHALLOWER);
		}
	}

	private void actionSave() {
		if (!tv_serverUrl.getText().toString().equals("") && !tv_serverUrl.getText().toString().equals("http://")) {
			addInHistory(tv_serverUrl.getText().toString().trim());
			Util.hideKeyboard(this, tv_serverUrl);

			task = new ServerScanner(this);
			task.execute();
		}
	}


	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.server, menu);

		menu.findItem(R.id.menu_clear_history).setVisible(settings.has(Settings.PrefKeys.AddServer_History));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_save:
                actionSave();
                return true;
			case R.id.menu_clear_history:
				settings.remove(Settings.PrefKeys.AddServer_History);
				item.setVisible(false);
				Toast.makeText(getApplicationContext(), getString(R.string.text66_1), Toast.LENGTH_SHORT).show();
				return true;
		}

		return true;
	}

	public void cancelSave() {
		task.stop();
		muninFoo.resetInstance(context);
	}

	private void addInHistory(String url) {
	    // Don't add - again - sample servers.
	    if (SampleServers.isSample(url)) {
	        return;
        }

		boolean contains = false;
		for (String s : getHistory()) {
			if (s.equals(url))
				contains = true;
		}
		if (!contains) {
			String history = settings.getString(Settings.PrefKeys.AddServer_History, "");
			history += url.replaceAll(";", ",") + ";";
			settings.set(Settings.PrefKeys.AddServer_History, history);
		}
	}

    /**
     * Returns previously typed URLs, alongside with default servers.
     */
	private String[] getHistory() {
		String history = settings.getString(Settings.PrefKeys.AddServer_History, "");
        String[] historyUrls = history.split(";");

		ArrayList<String> urls = new ArrayList<>();

		// Strip sample URLs
		for (String historyUrl : historyUrls) {
            if (!SampleServers.isSample(historyUrl)) {
		        urls.add(historyUrl);
            }
        }

        // Add them back at the end
        urls.addAll(Arrays.asList(SampleServers.URLS));

        return urls.toArray(new String[0]);
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Servers; }
}
