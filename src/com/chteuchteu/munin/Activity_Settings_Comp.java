package com.chteuchteu.munin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.google.analytics.tracking.android.EasyTracker;

public class Activity_Settings_Comp extends Activity {
	/*
	 * Note : this activity is deprecated.
	 * This is because of the use of the switch element which is not compatible
	 * with 'old' versions of Android.
	 * When I'll have time, I'll dynamically display checkbox or switch depending
	 * on version of Android with the main Activity_Settings activity.
	 */
	public static CheckBox 		cb_splash;
	public static Spinner		spinner_scale;
	public static Spinner		spinner_lang;
	public static Spinner		spinner_orientation;
	
	private MuninFoo muninFoo;
	
	
	@SuppressLint("NewApi")
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		setContentView(R.layout.settings_comp);
		
		// ==== ACTION BAR ====
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Mode naturel
			findViewById(R.id.viewTitle).setVisibility(View.GONE);
			findViewById(R.id.viewTitleSep).setVisibility(View.GONE);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getString(R.string.editServersTitle));
		} else {
			this.getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.grayBackground));
		}
		// ==== ACTION BAR ====
		
		
		cb_splash = 		(CheckBox)findViewById(R.id.checkbox_splash);
		spinner_scale = 	(Spinner)findViewById(R.id.spinner_scale);
		spinner_lang =		(Spinner)findViewById(R.id.spinner_lang);
		spinner_orientation = (Spinner)findViewById(R.id.spinner_orientation);
		
		
		// Bouton sauvegarder
		final Button save_button = (Button) findViewById(R.id.btn_settings_save);
		save_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				actionSave();
			}
		});
		
		spinner_scale = (Spinner)findViewById(R.id.spinner_scale);
		// Spinner default period
		List<String> list = new ArrayList<String>();
		list.add("day");	list.add("week");	list.add("month");	list.add("year");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_scale.setAdapter(dataAdapter);
		// Fin remplissage spinner */
		
		// Spinner language
		List<String> list2 = new ArrayList<String>();
		list2.add(getString(R.string.lang_english));
		list2.add(getString(R.string.lang_french));
		ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list2);
		dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_lang.setAdapter(dataAdapter2);
		
		
		// Spinner orientation
		List<String> list3 = new ArrayList<String>();
		list3.add(getString(R.string.text48_1));
		list3.add(getString(R.string.text48_2));
		list3.add(getString(R.string.text48_3));
		ArrayAdapter<String> dataAdapter3 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list3);
		dataAdapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_orientation.setAdapter(dataAdapter3);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Afficher dans les cases ce qu'il y a actuellement dans les settings
		if (getPref("splash").equals("false"))
			cb_splash.setChecked(true);
		
		if (getPref("defaultScale").equals("day"))
			spinner_scale.setSelection(0, true);
		else if (getPref("defaultScale").equals("week"))
			spinner_scale.setSelection(1, true);
		else if (getPref("defaultScale").equals("month"))
			spinner_scale.setSelection(2, true);
		else if (getPref("defaultScale").equals("year"))
			spinner_scale.setSelection(3, true);
		
		String lang = "";
		if (!getPref("lang").equals(""))
			lang = getPref("lang");
		else
			lang = Locale.getDefault().getLanguage();
		
		if (lang.equals("en"))
			spinner_lang.setSelection(0, true);
		else if (lang.equals("fr"))
			spinner_lang.setSelection(1, true);
		else if (lang.equals("de"))
			spinner_lang.setSelection(2, true);
		
		// Graphview orientation
		if (getPref("graphview_orientation").equals("horizontal"))
			spinner_orientation.setSelection(0);
		else if (getPref("graphview_orientation").equals("vertical"))
			spinner_orientation.setSelection(1);
		else
			spinner_orientation.setSelection(2);
	}
	
	public void actionSave() {
		if (cb_splash.isChecked())
			setPref("splash", "false");
		else
			setPref("splash", "true");
		
		setPref("defaultScale", spinner_scale.getSelectedItem().toString());
		
		if (spinner_lang.getSelectedItemPosition() == 0)
			setPref("lang", "en");
		else if (spinner_lang.getSelectedItemPosition() == 1)
			setPref("lang", "fr");
		else if (spinner_lang.getSelectedItemPosition() == 2)
			setPref("lang", "de");
		else
			setPref("lang", "en");
		
		// Orientation
		if (spinner_orientation.getSelectedItemPosition() == 0)
			setPref("graphview_orientation", "horizontal");
		else if (spinner_orientation.getSelectedItemPosition() == 1)
			setPref("graphview_orientation", "vertical");
		else
			setPref("graphview_orientation", "auto");
		
		Intent intent = new Intent(Activity_Settings_Comp.this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("action", "settingsSave");
		startActivity(intent);
		setTransition("shallower");
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// app icon in action bar clicked; go home
				Intent intent = new Intent(this, Activity_Main.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				setTransition("shallower");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		setTransition("shallower");
	}
	
	// SHARED PREFERENCES
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