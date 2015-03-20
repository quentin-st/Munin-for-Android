package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.async.ServerScanner;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("CommitPrefEdits")
public class Activity_Server extends MuninActivity {
	private Spinner  	spinner;
	public AutoCompleteTextView tv_serverUrl;

    public boolean isAlertShown;

	public MuninMaster master;
	public ServerScanner task;
	public AlertDialog alertDialog;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_server);
		super.onContentViewSet();
		dh.setDrawerActivity(this);

		actionBar.setTitle(getString(R.string.addServerTitle));

		Util.Fonts.setFont(this, (TextView) findViewById(R.id.muninMasterUrlLabel), CustomFont.Roboto_Medium);
		
		spinner = (Spinner)findViewById(R.id.spinner);
		tv_serverUrl = (AutoCompleteTextView)findViewById(R.id.textbox_serverUrl);
		
		// Servers history
		ArrayAdapter<String> addServerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, getHistory());
		tv_serverUrl.setAdapter(addServerAdapter);
		
		tv_serverUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
                if (spinner.getSelectedItemPosition() != 0 &&
                        !tv_serverUrl.getText().toString().contains("demo.munin-monitoring.org")
                        && !tv_serverUrl.getText().toString().contains("munin.ping.uio.no"))
                    spinner.setSelection(0);
            }

            @Override public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
            @Override public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
        });
		
		// Sample server
		List<String> list = new ArrayList<>();
		list.add("");
		list.add("demo.munin-monitoring.org");
		list.add("munin.ping.uio.no");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(dataAdapter);
		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
				if (view != null) {
					String selectedItem = ((TextView)view).getText().toString();
					if (selectedItem.equals("demo.munin-monitoring.org"))
						tv_serverUrl.setText("http://demo.munin-monitoring.org/");
					else if (selectedItem.equals("munin.ping.uio.no"))
						tv_serverUrl.setText("http://munin.ping.uio.no/");
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parentView) { }
		});
	}
	
	@Override
	public void onBackPressed() {
		// Hitting "back" within the AlertDialog will call its own onBackPressed
		// (which can't be overriden BTW).
		if (!isAlertShown) {
			Intent intent = new Intent(this, Activity_Servers.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(context, TransitionStyle.SHALLOWER);
		}
	}

	private void actionSave() {
		if (!tv_serverUrl.getText().toString().equals("") && !tv_serverUrl.getText().toString().equals("http://")) {
			addInHistory(tv_serverUrl.getText().toString().trim());
			Util.hideKeyboard(this, tv_serverUrl);

			task = new ServerScanner(this);
			task.scannerState = ServerScanner.ScannerState.RUNNING;
			task.execute();
		}
	}

	
	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.server, menu);
		
		if (Util.getPref(context, Util.PrefKeys.AddServer_History).equals(""))
			menu.findItem(R.id.menu_clear_history).setVisible(false);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_save:	actionSave();		return true;
			case R.id.menu_clear_history:
				Util.setPref(context, Util.PrefKeys.AddServer_History, "");
				createOptionsMenu();
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
		boolean contains = false;
		for (String s : getHistory()) {
			if (s.equals(url))
				contains = true;
		}
		if (!contains) {
			String his = Util.getPref(context, Util.PrefKeys.AddServer_History);
			his += url.replaceAll(";", ",") + ";";
			Util.setPref(context, Util.PrefKeys.AddServer_History, his);
		}
	}
	
	private String[] getHistory() {
		String his = Util.getPref(context, Util.PrefKeys.AddServer_History);
		if (his.equals(""))
			return new String[0];
		else
			return his.split(";");
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Servers; }
}