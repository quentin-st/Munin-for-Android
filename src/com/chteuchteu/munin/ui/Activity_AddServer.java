package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


@SuppressLint("CommitPrefEdits")
public class Activity_AddServer extends Activity {
	private MuninFoo	muninFoo;
	private DrawerHelper dh;
	private Context 	c;
	
	private Menu 		menu;
	private String		activityName;
	private EditText 	tb_auth_login;
	private EditText 	tb_auth_password;
	private Spinner	sp_authType;
	private CheckBox 	cb_auth;
	private Spinner  	spinner;
	private AutoCompleteTextView 	tb_serverUrl;
	private LinearLayout ll_auth;
	private LinearLayout loading;
	private int		loading_width;
	private TextView 	popup_title1;
	private TextView 	popup_title2;
	private int 		popup_width;
	private PopupWindow popup;
	private View		layout_popup;
	private boolean 	popupIsShown;
	
	private boolean 	launching;
	private String 	contextServerUrl;	// Si modification de serveur: URL du serveur a modifier
	private MuninServer settingsServer;
	
	// Algo
	private String 	serverUrl;
	private boolean 	SSL;
	private List<String> oldServers;
	private List<String> newServers;
	private String 	type;
	private String 	message_title;
	private String 	message_text;
	Activity_AddServer_Algorithm task;
	private boolean	canCancel = true;
	private int		algo_state = 0;
	private int		AST_IDLE = 0;
	private int		AST_RUNNING = 1;
	private int		AST_WAITING_FOR_URL = 2;
	private int		AST_WAITING_FOR_CREDENTIALS = 3;
	
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		muninFoo.loadLanguage(this);
		c = this;
		
		setContentView(R.layout.addserver);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		Intent thisIntent = getIntent();
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("contextServerUrl"))
			contextServerUrl = thisIntent.getExtras().getString("contextServerUrl");
		if (contextServerUrl != null && contextServerUrl.equals(""))
			actionBar.setTitle(getString(R.string.addServerTitle)); // Add a server
		else
			actionBar.setTitle(R.string.editServerTitle); // Edit a server
		
		if (muninFoo.drawer) {
			dh = new DrawerHelper(this, muninFoo);
			if (contextServerUrl != null && contextServerUrl.equals(""))
				dh.setDrawerActivity(dh.Activity_AddServer_Add);
			else
				dh.setDrawerActivity(dh.Activity_AddServer_Edit);
		}
		
		Util.UI.applySwag(this);
		
		if (contextServerUrl != null && !contextServerUrl.equals(""))
			findViewById(R.id.ll_sampleServer).setVisibility(View.GONE);
		
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.muninMasterUrlLabel), CustomFont.RobotoCondensed_Regular);
		
		tb_auth_login = 	(EditText)findViewById(R.id.auth_login);
		tb_auth_password = 	(EditText)findViewById(R.id.auth_password);
		cb_auth = 			(CheckBox)findViewById(R.id.checkbox_http_auth);
		spinner = 			(Spinner)findViewById(R.id.spinner);
		tb_serverUrl = 		(AutoCompleteTextView)findViewById(R.id.textbox_serverUrl);
		ll_auth =			(LinearLayout)findViewById(R.id.authIds);
		sp_authType =		(Spinner)findViewById(R.id.spinner_auth_type);
		
		launching = true;
		contextServerUrl = "";
		
		ArrayAdapter<String> addServerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, getHistory());
		tb_serverUrl.setAdapter(addServerAdapter);
		
		if (thisIntent != null && thisIntent.getExtras() != null && thisIntent.getExtras().containsKey("contextServerUrl"))
			contextServerUrl = thisIntent.getExtras().getString("contextServerUrl");
		
		cb_auth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked && ll_auth.getVisibility() == View.GONE)
					ll_auth.setVisibility(View.VISIBLE);
				else if (!isChecked && ll_auth.getVisibility() == View.VISIBLE)
					ll_auth.setVisibility(View.GONE);
			}
		});
		
		tb_serverUrl.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {
				if (spinner.getSelectedItemPosition() != 0 &&
						!tb_serverUrl.getText().toString().contains("demo.munin-monitoring.org")
						&& !tb_serverUrl.getText().toString().contains("munin.ping.uio.no"))
						spinner.setSelection(0);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
		});
		
		// Sample server
		List<String> list = new ArrayList<String>();
		list.add("");
		list.add("demo.munin-monitoring.org");
		list.add("munin.ping.uio.no");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(dataAdapter);
		
		// Auth type
		List<String> list2 = new ArrayList<String>();
		list2.add("Basic");
		list2.add("Digest");
		ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list2);
		dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_authType.setAdapter(dataAdapter2);
		
		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
				if (!launching) {
					cb_auth.setChecked(false);
					tb_auth_login.setText("");
					tb_auth_password.setText("");
				} else
					launching = false;
				if (view != null) {
					String selectedItem = ((TextView)view).getText().toString();
					if (selectedItem.equals("demo.munin-monitoring.org"))
						tb_serverUrl.setText("http://demo.munin-monitoring.org/");
					else if (selectedItem.equals("munin.ping.uio.no"))
						tb_serverUrl.setText("http://munin.ping.uio.no/");
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parentView) { }
		});
		
		
		// Popup
		int screenH = 0;
		int screenW = 0;
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			screenH = size.y;
			screenW = size.x;
		} else {
			Display display = getWindowManager().getDefaultDisplay();
			screenH = display.getHeight();
			screenW = display.getWidth();
		}
		
		popup_width = screenW;
		int popupHeight = screenH;
		
		LinearLayout viewGroup = (LinearLayout) findViewById(R.id.popup);
		LayoutInflater layoutInflater = (LayoutInflater) c
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layout_popup = layoutInflater.inflate(R.layout.addserver_popup, viewGroup);
		
		if (contextServerUrl == null || (contextServerUrl != null && contextServerUrl.equals("")))
			findViewById(R.id.addserver_auth).setVisibility(View.GONE);
		
		
		// Creating the PopupWindow
		popup = new PopupWindow(c);
		popup.setContentView(layout_popup);
		popup.setWidth(popup_width);
		popup.setHeight(popupHeight);
		popup.setFocusable(false);
		popup.setOutsideTouchable(false);
		popup.setBackgroundDrawable(new BitmapDrawable());
		popup.setFocusable(true);
		//popup.update();
		popupIsShown = false;
	}
	
	@Override
	public void onBackPressed() {
		if (!popupIsShown) {
			Intent intent = new Intent(this, Activity_Servers.class);
			if (settingsServer != null && settingsServer.getParent() != null)
				intent.putExtra("fromMaster", settingsServer.getParent().getId());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(c, TransitionStyle.SHALLOWER);
		} else {
			if (popupIsShown && canCancel)
				cancelSave();
		}
	}
	
	// Actions
	public void actionSave() {
		if (!tb_serverUrl.getText().toString().equals("") && !tb_serverUrl.getText().toString().equals("http://")) {
			addInHistory(tb_serverUrl.getText().toString());
			//InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			//imm.hideSoftInputFromWindow(tb_serverUrl.getWindowToken(), 0);
			//imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
			algo_state = AST_RUNNING;
			task = new Activity_AddServer_Algorithm();
			task.execute();
		}
	}
	
	public void actionClear() {
		tb_serverUrl.setText("");
		tb_auth_login.setText("");
		tb_auth_password.setText("");
		cb_auth.setChecked(false);
		spinner.setSelection(0, true);
		sp_authType.setSelection(0, true);
		// Textfields cleared
		Toast.makeText(getApplicationContext(), getString(R.string.text14), Toast.LENGTH_SHORT).show();
	}
	
	public void actionDelete() {
		if (contextServerUrl != null && !contextServerUrl.equals("")) {
			// When going back : expand the list to the current master if possible
			MuninMaster m = null;
			if (muninFoo.currentServer.getParent() != null && muninFoo.currentServer.getParent().getChildren().size() > 1)
				m = muninFoo.currentServer.getParent();
			
			MuninServer curServer = muninFoo.getServer(contextServerUrl);
			muninFoo.sqlite.dbHlpr.deleteServer(curServer);
			muninFoo.deleteServer(curServer);
			
			if (muninFoo.currentServer != null && muninFoo.currentServer.getServerUrl().equals(contextServerUrl)) {
				if (muninFoo.getHowManyServers() == 0)
					muninFoo.currentServer = null;
				else
					muninFoo.currentServer = muninFoo.getServer(0);
			}
			
			Intent intent = new Intent(this, Activity_Servers.class);
			if (m != null)
				intent.putExtra("fromMaster", m.getId());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
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
		getMenuInflater().inflate(R.menu.addserver, menu);
		
		// Masquage éventuel du bouton delete
		if (contextServerUrl == null || (contextServerUrl != null && contextServerUrl.equals("")))
			menu.findItem(R.id.menu_delete).setVisible(false);
		if (Util.getPref(c, "addserver_history").equals(""))
			menu.findItem(R.id.menu_clear_history).setVisible(false);
	}
	
	public void onResume() {
		super.onResume();
		
		// Afficher dans les cases ce qu'il y a actuellement dans les settings
		tb_serverUrl.setText(contextServerUrl);
		
		// Recherche de settingsServer
		for (int i=0; i<muninFoo.getHowManyServers(); i++) {
			if (muninFoo.getServer(i) != null && muninFoo.getServer(i).equalsApprox(contextServerUrl))
				settingsServer = muninFoo.getServer(i);
		}
		
		if (settingsServer != null) {
			if (settingsServer.isAuthNeeded()) { // Parametres auth renseignes
				cb_auth.setChecked(true);
				//ll_auth.setVisibility(View.VISIBLE);
				
				tb_auth_login.setText(settingsServer.getAuthLogin());
				tb_auth_password.setText(settingsServer.getAuthPassword());
				if (settingsServer.getAuthType() == AuthType.BASIC)
					sp_authType.setSelection(0);
				else if (settingsServer.getAuthType() == AuthType.DIGEST)
					sp_authType.setSelection(1);
			} else
				ll_auth.setVisibility(View.GONE);
		} else
			ll_auth.setVisibility(View.GONE);
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
					Intent intent = new Intent(this, Activity_Servers.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				}
				return true;
			case R.id.menu_save:	actionSave();		return true;
			case R.id.menu_clear:	actionClear();		return true;
			case R.id.menu_delete:	actionDelete(); 	return true;
			case R.id.menu_clear_history:
				Util.setPref(c, "addserver_history", "");
				createOptionsMenu();
				Toast.makeText(getApplicationContext(), getString(R.string.text66_1), Toast.LENGTH_SHORT).show();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_AddServer.this, Activity_Settings.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_AddServer.this, Activity_About.class));
				Util.setTransition(c, TransitionStyle.DEEPER);
				return true;
			default:	return super.onOptionsItemSelected(item);
		}
	}
	
	public void cancelSave() {
		if (popup_title1 != null)	popup_title1.setText("");
		if (popup_title2 != null)	popup_title2.setText("");
		if (loading != null)		loading.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT));
		task.cancel(true);
		popupIsShown = false;
		algo_state = AST_IDLE;
		settingsServer = null;
		popup.dismiss();
		muninFoo.resetInstance(c);
	}
	public class Activity_AddServer_Algorithm extends AsyncTask<Void, Integer, Void> {
		private int res = 0;
		private int RES_UNDEFINED = 0;
		private int RES_SERVER_SUCCESS = 1;
		private int RES_SERVERS_SUCCESS = 2;
		private int RES_OK = 4;
		
		private int RES_NOT_PREMIUM = -1;
		private int RES_NO_CONNECTION = -2;
		//private int RES_UNKNOWN_HTTP_ERROR = -3;
		private int RES_ERR_UNDEFINED = -4;
		private int RES_MALFORMED_URL = -5;
		
		private void setPopupState(final int avancement) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (avancement >= 0 && avancement <= 100) {
						View l = popup.getContentView().findViewById(R.id.popup_container_avancement);
						if (l != null && l.getVisibility() == View.GONE)	l.setVisibility(View.VISIBLE);
						loading_width = Math.round(avancement * popup_width / 100);
						if (loading != null)	loading.setLayoutParams(new LinearLayout.LayoutParams(loading_width, LayoutParams.MATCH_PARENT));
					}
				}
			});
		}
		
		private void setPopupText(final String title1, final String title2) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (popup_title1.getVisibility() == View.GONE)	popup_title1.setVisibility(View.VISIBLE);
					if (popup_title2.getVisibility() == View.GONE)	popup_title2.setVisibility(View.VISIBLE);
					if (!title1.equals(""))	popup_title1.setText(title1);
					if (!title2.equals(""))	popup_title2.setText(title2);
				}
			});
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			if (Util.isOnline(c)) {
				if (algo_state != AST_WAITING_FOR_CREDENTIALS && algo_state != AST_WAITING_FOR_URL) {
					runOnUiThread(new Runnable() {
						public void run() {
							popup.showAtLocation(layout_popup, Gravity.CENTER, 0, 0);
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
						}
					});
					
					loading = (LinearLayout) popup.getContentView().findViewById(R.id.popup_loading_avancement);
					popup_title1 = (TextView) popup.getContentView().findViewById(R.id.popup_text_a);
					popup_title2 = (TextView) popup.getContentView().findViewById(R.id.popup_text_b);
					Fonts.setFont(c, popup_title1, CustomFont.Roboto_Thin);
					Fonts.setFont(c, popup_title2, CustomFont.Roboto_Thin);
					popupIsShown = true;
					popup_title1.setText(getString(R.string.text43)); // Please wait...
				}
				setPopupState(0);
			}
		}
		
		private int start() {
			if (Util.isOnline(c)) {
				setPopupState(0);
				setPopupText("", getString(R.string.text42));
				
				type = "";
				if (serverUrl == null || serverUrl.equals(""))
					serverUrl = tb_serverUrl.getText().toString();
				
				SSL = false;
				
				// Modifications de l'URL
				if (!serverUrl.contains("http://") && !serverUrl.contains("https://"))
					serverUrl = "http://" + serverUrl;
				if (serverUrl.contains("https://"))
					SSL = true;
				if (serverUrl.length() > 10 && !serverUrl.substring(serverUrl.length()-1).equals("/") && !serverUrl.contains("/index.html"))
					serverUrl = serverUrl + "/index.html";
				
				if (SSL && !muninFoo.premium)
					return RES_NOT_PREMIUM;
				
				return RES_OK;
			} else
				return RES_NO_CONNECTION;
		}
		
		private void askAgainForUrl(final String err) {
			final EditText et_url = (EditText) popup.getContentView().findViewById(R.id.popup_url_edittext);
			final Button cancel = (Button) popup.getContentView().findViewById(R.id.popup_url_cancel);
			final Button continu = (Button) popup.getContentView().findViewById(R.id.popup_url_continue);
			
			runOnUiThread(new Runnable() {
				public void run() {
					TextView popup_url_message = (TextView)popup.getContentView().findViewById(R.id.popup_url_message);
					TextView popup_url_message2 = (TextView)popup.getContentView().findViewById(R.id.popup_url_message2);
					Fonts.setFont(c, popup_url_message, CustomFont.Roboto_Thin);
					Fonts.setFont(c, popup_url_message2, CustomFont.Roboto_Thin);
					Fonts.setFont(c, cancel, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(c, continu, CustomFont.RobotoCondensed_Regular);
					popup_title1.setVisibility(View.GONE);
					popup_title2.setVisibility(View.GONE);
					popup.getContentView().findViewById(R.id.popup_container_avancement).setVisibility(View.GONE);
					popup.getContentView().findViewById(R.id.popup_url).setVisibility(View.VISIBLE);
					
					if (err != null && err.contains("Timeout")) {
						popup_url_message.setVisibility(View.GONE);
						popup_url_message2.setText(err);
					} else if (err != null && !err.substring(0, 3).equals("200"))
						popup_url_message.setText(err);
					else
						popup_url_message.setVisibility(View.GONE);
					//if (settingsServer != null)
						//et_url.setText(settingsServer.getServerUrl());
					//else
					et_url.setText(serverUrl);
					//InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					//imm.showSoftInput(this, 0);
				}
			});
			
			algo_state = AST_WAITING_FOR_URL;
			
			cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (popup_title1 != null)	popup_title1.setText("");
					if (popup_title2 != null)	popup_title2.setText("");
					if (loading != null)		loading.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT));
					runOnUiThread(new Runnable() {
						public void run() {
							popup_title1.setVisibility(View.VISIBLE);
							popup_title2.setVisibility(View.VISIBLE);
							popup.getContentView().findViewById(R.id.popup_container_avancement).setVisibility(View.VISIBLE);
							popup.getContentView().findViewById(R.id.popup_url).setVisibility(View.GONE);
						}
					});
					popupIsShown = false;
					algo_state = AST_IDLE;
					settingsServer = null;
					popup.dismiss();
					muninFoo.resetInstance(c);
				}
			});
			continu.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final String url = et_url.getText().toString();
					serverUrl = url;
					
					if (!muninFoo.premium && url.contains("https://")) {
						cancelFetch(RES_NOT_PREMIUM);
					} else {
						settingsServer.setServerUrl(url);
						//InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						//imm.hideSoftInputFromWindow(tb_serverUrl.getWindowToken(), 0);
						//imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
						algo_state = AST_RUNNING;
						
						runOnUiThread(new Runnable() {
							public void run() {
								tb_serverUrl.setText(url);
								popup_title1.setVisibility(View.VISIBLE);
								popup_title2.setVisibility(View.VISIBLE);
								popup.getContentView().findViewById(R.id.popup_container_avancement).setVisibility(View.VISIBLE);
								popup.getContentView().findViewById(R.id.popup_url).setVisibility(View.GONE);
							}
						});
						
						task = new Activity_AddServer_Algorithm();
						task.execute();
					}
				}
			});
			this.cancel(true);
			
		}
		
		private void cancelFetch(int res) { cancelFetch(res, ""); }
		private void cancelFetch(int res, final String s) {
			if (popupIsShown) {
				runOnUiThread(new Runnable() {
					public void run() {
						if (loading != null) {
							loading.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT));
							loading.setVisibility(View.GONE);
						}
						popup.dismiss();
						popupIsShown = false;
					}
				});
			}
			
			task.cancel(true);
			settingsServer = null;
			muninFoo.resetInstance(c);
			algo_state = AST_IDLE;
			
			if (res == RES_NOT_PREMIUM) {
				// SSL Support is available with the Munin for Android Features Pack. Do you want to purchase it on Google Play?
				message_title = "";
				if (s.equals("digest"))
					message_text = getString(R.string.text65_1);
				else // SSL
					message_text = getString(R.string.text41);
				
				runOnUiThread(new Runnable() {
					public void run() {
						AlertDialog.Builder builder = new AlertDialog.Builder(Activity_AddServer.this);
						builder.setMessage(message_text)
						.setCancelable(false)
						// Yes
						.setPositiveButton(getString(R.string.text33), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								try {
									Intent intent = new Intent(Intent.ACTION_VIEW);
									intent.setData(Uri.parse("market://details?id=com.chteuchteu.muninforandroidfeaturespack"));
									startActivity(intent);
								} catch (Exception ex) {
									final AlertDialog ad = new AlertDialog.Builder(Activity_AddServer.this).create();
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
						})
						// Learn more...
						.setNeutralButton(getString(R.string.text35), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent intent = new Intent(Activity_AddServer.this, Activity_GoPremium.class);
								intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								startActivity(intent);
							}
						})
						// No
						.setNegativeButton(getString(R.string.text34), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
						AlertDialog alert = builder.create();
						alert.show();
					}
				});
			} else if (res == RES_NO_CONNECTION) {
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(Activity_AddServer.this, getString(R.string.text30), Toast.LENGTH_LONG).show();
					}
				});
			}
			else if (res == RES_MALFORMED_URL) {
				// Popup pas ouverte à cet endroit -> dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(Activity_AddServer.this);
				builder.setMessage(getString(R.string.text16))
				.setCancelable(true)
				// OK
				.setPositiveButton(getString(R.string.text64), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
			else if (res == RES_ERR_UNDEFINED) {
				
			}
		}
		
		private String initialization() {
			oldServers = new ArrayList<String>();
			// Create current servers list (diff)
			for (MuninServer s : muninFoo.getServers())
				oldServers.add(s.getServerUrl());
			newServers = new ArrayList<String>();
			
			setPopupText(getString(R.string.text44), "");
			
			/* 			DETECTION DU TYPE D'URL 		*/
			if (settingsServer == null)
				settingsServer = new MuninServer("", serverUrl);
			settingsServer.createTitle();
			if (findViewById(R.id.addserver_auth).getVisibility() == View.VISIBLE && cb_auth.isChecked() && !tb_auth_login.getText().toString().equals("")) {
				settingsServer.setAuthIds(tb_auth_login.getText().toString(), tb_auth_password.getText().toString());
				if (sp_authType.getSelectedItemPosition() == 0)
					settingsServer.setAuthType(AuthType.BASIC);
				else
					settingsServer.setAuthType(AuthType.DIGEST);
			}
			settingsServer.setSSL(SSL);
			
			type = settingsServer.detectPageType();
			
			// The first connection to the server has been done : settingsServer.ssl has been set to true if necessary.
			// If not premium :
			// If ssl was true : we're not supposed to be there.
			// If ssl was false and is now true : display error msg.
			if (!muninFoo.premium && settingsServer.getSSL())
				type = "RES_NOT_PREMIUM";
			
			return type;
		}
		
		private int finish() {
			int ret = RES_UNDEFINED;
			if (type.equals("munin/")) {
				/*		CONTENT OF THE PAGE: SERVERS LIST	*/
				int nbNewServers = muninFoo.fetchServersListRecursive(settingsServer);
				
				boolean fetchSuccess = nbNewServers > 0;
				
				if (fetchSuccess) {
					int popupstate = 30;
					setPopupState(popupstate);
					
					// Plugins lookup for each server
					int tmpNewServer = 1;
					for (int i=0; i<muninFoo.getHowManyServers(); i++) {
						if (muninFoo.getServer(i) != null && muninFoo.getServer(i).getPlugins().size() == 0) {
							if (tmpNewServer <= nbNewServers)
								setPopupText("", getString(R.string.text46) + " " + tmpNewServer + "/" + nbNewServers);
							muninFoo.getServer(i).fetchPluginsList();
							
							boolean contains = false;
							for (String s : oldServers) {
								if (muninFoo.getServer(i).equalsApprox(s))
									contains = true;
							}
							if (!contains)
								newServers.add(muninFoo.getServer(i).getServerUrl());
							
							tmpNewServer++;
							if (popupstate < 80) {
								popupstate += Math.round(50/nbNewServers);
								setPopupState(popupstate);
							}
						}
					}
					if (nbNewServers > 0) {	// Added 1 server or more
						setPopupState(100);
						setPopupText(getString(R.string.text45), " ");
						
						canCancel = false;
						muninFoo.sqlite.saveServers();
						canCancel = true;
						
						// Success!
						message_title = getString(R.string.text18);
						if (newServers.size() == 0) {
							String s = "";
							if (nbNewServers > 1)	s = "s";
							// X sub-server(s) updated!
							message_text = nbNewServers + " " + getString(R.string.text21_1) + s + " " + getString(R.string.text21_3);
						} else {
							String s = "";
							if (newServers.size() > 1)	s = "s";
							// X sub-server(s) added!
							message_text = newServers.size() + " " + getString(R.string.text21_1) + s + " " + getString(R.string.text21_2);
						}
						return RES_SERVERS_SUCCESS;
					}
				}
			}	// ending if (type.equals("munin/")) (servers)
			else if (type.equals("munin/x/")) {
				/*		CONTENT OF THE PAGE: PLUGINS LIST	*/
				/*   (long code: here is a potato:	0	)	*/
				
				setPopupText(getString(R.string.text44), "");
				
				boolean fetchSuccess = settingsServer.fetchPluginsList();
				
				if (fetchSuccess) {
					setPopupState(50);
					
					boolean contains = false;
					for (String s : oldServers) {
						if (settingsServer.equalsApprox(s))
							contains = true;
					}
					if (!contains)
						newServers.add(settingsServer.getServerUrl());
					
					
					List<MuninPlugin> settingsServerPlugins = settingsServer.getPlugins();
					
					if (settingsServerPlugins.size() > 0) {
						settingsServer.setPluginsList(settingsServerPlugins);
						
						setPopupState(100);
						
						muninFoo.addServer(settingsServer);
						
						setPopupText(getString(R.string.text45), " ");
						
						canCancel = false;
						muninFoo.sqlite.saveServers();
						canCancel = true;
						
						message_title = settingsServerPlugins.size() + "";
						// Success
						message_text = getString(R.string.text24);
						return RES_SERVER_SUCCESS;
					}
				}
			} // Fin elseif (type: plugins)
			return ret;
		}
		
		private void askForCredentials() {
			final EditText et_login = (EditText) popup.getContentView().findViewById(R.id.popup_credentials_login);
			final EditText et_password = (EditText) popup.getContentView().findViewById(R.id.popup_credentials_password);
			final Button cancel = (Button) popup.getContentView().findViewById(R.id.popup_credentials_cancel);
			final Button continu = (Button) popup.getContentView().findViewById(R.id.popup_credentials_continue);
			final Spinner pop_sp_authType = (Spinner) popup.getContentView().findViewById(R.id.popup_credentials_authtype);
			
			runOnUiThread(new Runnable() {
				public void run() {
					popup_title1.setVisibility(View.GONE);
					popup_title2.setVisibility(View.GONE);
					popup.getContentView().findViewById(R.id.popup_container_avancement).setVisibility(View.GONE);
					popup.getContentView().findViewById(R.id.popup_credentials).setVisibility(View.VISIBLE);
					Fonts.setFont(c, cancel, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(c, continu, CustomFont.RobotoCondensed_Regular);
					
					// Remplissage spinner auth type
					List<String> list2 = new ArrayList<String>();
					list2.add("Basic");
					list2.add("Digest");
					ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, list2);
					dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					pop_sp_authType.setAdapter(dataAdapter2);
					
					if (settingsServer.isAuthNeeded() == true) {
						et_login.setText(settingsServer.getAuthLogin());
						et_password.setText(settingsServer.getAuthPassword());
						if (settingsServer.getAuthType() == AuthType.BASIC)
							pop_sp_authType.setSelection(0);
						else if (settingsServer.getAuthType() == AuthType.DIGEST)
							pop_sp_authType.setSelection(1);
					}
					
					pop_sp_authType.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> arg0, View arg1, int select, long arg3) {
							if (!muninFoo.premium) {
								if (select == 1)
									popup.getContentView().findViewById(R.id.popup_credentials_premium).setVisibility(View.VISIBLE);
								else
									popup.getContentView().findViewById(R.id.popup_credentials_premium).setVisibility(View.GONE);
							}
						}
						
						@Override
						public void onNothingSelected(AdapterView<?> arg0) { }
					});
				}
			});
			
			algo_state = AST_WAITING_FOR_CREDENTIALS;
			
			cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (popup_title1 != null)	popup_title1.setText("");
					if (popup_title2 != null)	popup_title2.setText("");
					if (loading != null)		loading.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT));
					runOnUiThread(new Runnable() {
						public void run() {
							pop_sp_authType.setSelection(0);
							et_login.setText("");
							et_password.setText("");
							popup_title1.setVisibility(View.VISIBLE);
							popup_title2.setVisibility(View.VISIBLE);
							popup.getContentView().findViewById(R.id.popup_container_avancement).setVisibility(View.VISIBLE);
							popup.getContentView().findViewById(R.id.popup_credentials).setVisibility(View.GONE);
						}
					});
					popupIsShown = false;
					algo_state = AST_IDLE;
					settingsServer = null;
					popup.dismiss();
					muninFoo.resetInstance(c);
				}
			});
			continu.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!muninFoo.premium && pop_sp_authType.getSelectedItemPosition() == 1)
						cancelFetch(RES_NOT_PREMIUM, "digest");
					else {
						String login = et_login.getText().toString();
						String password = et_password.getText().toString();
						settingsServer.setAuthIds(login, password);
						if (pop_sp_authType.getSelectedItemPosition() == 0)
							settingsServer.setAuthType(AuthType.BASIC);
						else
							settingsServer.setAuthType(AuthType.DIGEST);
						//InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						//imm.hideSoftInputFromWindow(tb_serverUrl.getWindowToken(), 0);
						//imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
						algo_state = AST_RUNNING;
						
						runOnUiThread(new Runnable() {
							public void run() {
								popup_title1.setVisibility(View.VISIBLE);
								popup_title2.setVisibility(View.VISIBLE);
								popup.getContentView().findViewById(R.id.popup_container_avancement).setVisibility(View.VISIBLE);
								popup.getContentView().findViewById(R.id.popup_credentials).setVisibility(View.GONE);
							}
						});
						
						task = new Activity_AddServer_Algorithm();
						task.execute();
					}
				}
			});
			this.cancel(true);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			popup.getContentView().findViewById(R.id.popup_credentials).setVisibility(View.GONE);
			popup.getContentView().findViewById(R.id.popup_url).setVisibility(View.GONE);
			// Zapper les étapes déjà faites
			boolean stop = false;
			if (algo_state != AST_WAITING_FOR_CREDENTIALS) {
				int res1 = start();
				if (res1 == RES_NO_CONNECTION || res1 == RES_NOT_PREMIUM || res1 == RES_MALFORMED_URL) {
					cancelFetch(res1);
					stop = true; // le thread n'a pas le temps de s'arrêter
				}
			}
			
			// Lancement de la popup
			if (!stop) {
				String res2 = initialization();
				if (!res2.equals("munin/") && !res2.equals("munin/x/")) {
					if (res2.length() > 3) {
						if (res2.equals("RES_NOT_PREMIUM"))
							cancelFetch(RES_NOT_PREMIUM);
						
						String result = res2.substring(0, 3);
						if (result.equals("401"))
							askForCredentials();
						else if (res2.equals("timeout"))
							askAgainForUrl(getString(R.string.text68));
						else
							askAgainForUrl(res2);
					}
					else // RES_UNKNOWN_HTTP_ERROR
						askAgainForUrl(res2);
				} else {
					res = finish();
				}
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (muninFoo.getHowManyServers() == 0)
				muninFoo.currentServer = null;
			else
				muninFoo.currentServer = muninFoo.getServer(0);
			
			canCancel = true;
			algo_state = AST_IDLE;
			if (res != RES_UNDEFINED) {
				Button b = (Button) popup.getContentView().findViewById(R.id.popup_button);
				Fonts.setFont(c, b, CustomFont.RobotoCondensed_Regular);
				LinearLayout loading_bar = (LinearLayout) popup.getContentView().findViewById(R.id.popup_container_avancement);
				if (res == RES_SERVER_SUCCESS) {
					// Congratulations!			X plugins found!
					setPopupText(getString(R.string.text18), message_title + " " + getString(R.string.text27));
					loading_bar.setVisibility(View.GONE);
					b.setVisibility(View.VISIBLE);
					b.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							popup.dismiss();
							Intent intent = new Intent(Activity_AddServer.this, Activity_Servers.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							Util.setTransition(c, TransitionStyle.SHALLOWER);
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
						}
					});
				} else if (res == RES_SERVERS_SUCCESS) {
					setPopupText(message_title, message_text);
					loading_bar.setVisibility(View.GONE);
					b.setVisibility(View.VISIBLE);
					b.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							popup.dismiss();
							Intent intent = new Intent(Activity_AddServer.this, Activity_Servers.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							Util.setTransition(c, TransitionStyle.SHALLOWER);
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
						}
					});
				}
			}
			if (MuninFoo.DEBUG)
				muninFoo.sqlite.logMasters();
			if (!Util.isOnline(c))
				Toast.makeText(Activity_AddServer.this, getString(R.string.text30), Toast.LENGTH_LONG).show();
		}
	}
	
	public void addInHistory(String url) {
		boolean contains = false;
		for (String s : getHistory()) {
			if (s.equals(url))
				contains = true;
		}
		if (!contains) {
			String his = Util.getPref(c, "addserver_history");
			his += url.replaceAll(";", ",") + ";";
			Util.setPref(c, "addserver_history", his);
		}
	}
	
	public String[] getHistory() {
		String his = Util.getPref(c, "addserver_history");
		if (his.equals(""))
			return new String[0];
		else
			return his.split(";");
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (!MuninFoo.DEBUG)
			EasyTracker.getInstance(this).activityStop(this);
	}
}