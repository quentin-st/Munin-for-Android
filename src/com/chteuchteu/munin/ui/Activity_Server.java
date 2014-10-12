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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.ProgressBar;
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
public class Activity_Server extends Activity {
	private MuninFoo	muninFoo;
	private DrawerHelper dh;
	private Context 	context;
	private Menu 		menu;
	private String		activityName;
	
	private EditText 	tb_auth_login;
	private EditText 	tb_auth_password;
	private Spinner	sp_authType;
	private CheckBox 	cb_auth;
	private Spinner  	spinner;
	private AutoCompleteTextView 	tb_serverUrl;
	private LinearLayout ll_auth;
	
	private ProgressBar progressBar;
	private TextView 	popup_title1;
	private TextView 	popup_title2;
	private AlertDialog popup;
	private boolean 	popupIsShown;
	
	private boolean 	launching;
	private String 	contextServerUrl;	// Si modification de serveur: URL du serveur a modifier
	private MuninServer settingsServer;
	
	private Activity_Mode mode;
	private enum Activity_Mode { ADD_SERVER, EDIT_SERVER }
	
	// AddServer stuff
	private String 	serverUrl;
	private boolean 	ssl;
	private List<String> oldServers;
	private List<String> newServers;
	private String 	type;
	private String 	message_title;
	private String 	message_text;
	private AddServerThread task;
	private boolean	canCancel;
	private int		algo_state = 0;
	private int		AST_IDLE = 0;
	private int		AST_RUNNING = 1;
	private int		AST_WAITING_FOR_URL = 2;
	private int		AST_WAITING_FOR_CREDENTIALS = 3;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		context = this;
		
		setContentView(R.layout.addserver);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		Intent thisIntent = getIntent();
		contextServerUrl = "";
		if (thisIntent != null && thisIntent.getExtras().containsKey("contextServerUrl"))
			contextServerUrl = thisIntent.getExtras().getString("contextServerUrl");
		
		dh = new DrawerHelper(this, muninFoo);
		if (contextServerUrl.equals("")) {
			mode = Activity_Mode.ADD_SERVER;
			actionBar.setTitle(getString(R.string.addServerTitle)); // Add a server
			dh.setDrawerActivity(dh.Activity_Server_Add);
			findViewById(R.id.addserver_auth).setVisibility(View.GONE);
		}
		else {
			mode = Activity_Mode.EDIT_SERVER;
			actionBar.setTitle(R.string.editServerTitle); // Edit a server
			dh.setDrawerActivity(dh.Activity_Server_Edit);
			findViewById(R.id.ll_sampleServer).setVisibility(View.GONE);
		}
		
		Util.UI.applySwag(this);
		Util.Fonts.setFont(this, (TextView) findViewById(R.id.muninMasterUrlLabel), CustomFont.RobotoCondensed_Regular);
		
		tb_auth_login = 	(EditText)findViewById(R.id.auth_login);
		tb_auth_password = 	(EditText)findViewById(R.id.auth_password);
		cb_auth = 			(CheckBox)findViewById(R.id.checkbox_http_auth);
		spinner = 			(Spinner)findViewById(R.id.spinner);
		tb_serverUrl = 		(AutoCompleteTextView)findViewById(R.id.textbox_serverUrl);
		ll_auth =			(LinearLayout)findViewById(R.id.authIds);
		sp_authType =		(Spinner)findViewById(R.id.spinner_auth_type);
		
		launching = true;
		
		// Servers history
		ArrayAdapter<String> addServerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, getHistory());
		tb_serverUrl.setAdapter(addServerAdapter);
		
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
		
		if (mode == Activity_Mode.ADD_SERVER) {
			// Sample server
			List<String> list = new ArrayList<String>();
			list.add("");
			list.add("demo.munin-monitoring.org");
			list.add("munin.ping.uio.no");
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
			dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(dataAdapter);
			
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
		}
		
		if (mode == Activity_Mode.EDIT_SERVER) {
			cb_auth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked && ll_auth.getVisibility() == View.GONE)
						ll_auth.setVisibility(View.VISIBLE);
					else if (!isChecked && ll_auth.getVisibility() == View.VISIBLE)
						ll_auth.setVisibility(View.GONE);
				}
			});
			
			// Auth type
			List<String> list2 = new ArrayList<String>();
			list2.add("Basic");
			list2.add("Digest");
			ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list2);
			dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sp_authType.setAdapter(dataAdapter2);
			
			// Fill server URL field
			tb_serverUrl.setText(contextServerUrl);
			
			// settingsServer lookup
			for (int i=0; i<muninFoo.getHowManyServers(); i++) {
				if (muninFoo.getServer(i) != null && muninFoo.getServer(i).equalsApprox(contextServerUrl))
					settingsServer = muninFoo.getServer(i);
			}
			
			
			if (settingsServer.isAuthNeeded()) {
				cb_auth.setChecked(true);
				
				tb_auth_login.setText(settingsServer.getAuthLogin());
				tb_auth_password.setText(settingsServer.getAuthPassword());
				if (settingsServer.getAuthType() == AuthType.BASIC)
					sp_authType.setSelection(0);
				else if (settingsServer.getAuthType() == AuthType.DIGEST)
					sp_authType.setSelection(1);
			} else
				ll_auth.setVisibility(View.GONE);
		}
		
		if (MuninFoo.DEBUG)
			tb_serverUrl.setText("https://a.andrewandcara.com/munin");
	}
	
	@Override
	public void onBackPressed() {
		if (!popupIsShown) {
			Intent intent = new Intent(this, Activity_Servers.class);
			if (settingsServer != null && settingsServer.getParent() != null)
				intent.putExtra("fromMaster", settingsServer.getParent().getId());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(context, TransitionStyle.SHALLOWER);
		} else {
			if (popupIsShown && canCancel)
				cancelSave();
		}
	}
	
	// Actions
	public void actionSave() {
		if (!tb_serverUrl.getText().toString().equals("") && !tb_serverUrl.getText().toString().equals("http://")) {
			addInHistory(tb_serverUrl.getText().toString());
			Util.hideKeyboard(this, tb_serverUrl);
			algo_state = AST_RUNNING;
			task = new AddServerThread();
			task.execute();
		}
	}
	
	public void actionDelete() {
		if (!contextServerUrl.equals("")) {
			new AlertDialog.Builder(context)
			.setTitle(R.string.delete)
			.setMessage(R.string.text83)
			.setPositiveButton(R.string.text33, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// When going back : expand the list to the current master if possible
					MuninMaster m = null;
					if (muninFoo.currentServer.getParent() != null && muninFoo.currentServer.getParent().getChildren().size() > 1)
						m = muninFoo.currentServer.getParent();
					
					MuninServer curServer = muninFoo.getServer(contextServerUrl);
					muninFoo.sqlite.dbHlpr.deleteServer(curServer);
					muninFoo.deleteServer(curServer, true);
					
					if (muninFoo.currentServer != null && muninFoo.currentServer.getServerUrl().equals(contextServerUrl)) {
						if (muninFoo.getHowManyServers() == 0)
							muninFoo.currentServer = null;
						else
							muninFoo.currentServer = muninFoo.getServer(0);
					}
					
					Intent intent = new Intent(context, Activity_Servers.class);
					if (m != null)
						intent.putExtra("fromMaster", m.getId());
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
				}
			})
			.setNegativeButton(R.string.text34, null)
			.show();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		
		dh.getDrawer().setOnOpenListener(new OnOpenListener() {
			@Override
			public void onOpen() {
				activityName = getActionBar().getTitle().toString();
				getActionBar().setTitle(R.string.app_name);
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
		
		createOptionsMenu();
		
		return true;
	}
	
	private void createOptionsMenu() {
		menu.clear();
		getMenuInflater().inflate(R.menu.server, menu);
		
		// Hide delete button if necessary
		if (mode == Activity_Mode.ADD_SERVER)
			menu.findItem(R.id.menu_delete).setVisible(false);
		if (Util.getPref(context, "addserver_history").equals(""))
			menu.findItem(R.id.menu_clear_history).setVisible(false);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_save:	actionSave();		return true;
			case R.id.menu_delete:	actionDelete(); 	return true;
			case R.id.menu_clear_history:
				Util.setPref(context, "addserver_history", "");
				createOptionsMenu();
				Toast.makeText(getApplicationContext(), getString(R.string.text66_1), Toast.LENGTH_SHORT).show();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Server.this, Activity_Settings.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Server.this, Activity_About.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			default:	return super.onOptionsItemSelected(item);
		}
	}
	
	public void cancelSave() {
		if (popup_title1 != null)	popup_title1.setText("");
		if (popup_title2 != null)	popup_title2.setText("");
		task.cancel(true);
		popupIsShown = false;
		algo_state = AST_IDLE;
		settingsServer = null;
		popup.dismiss();
		muninFoo.resetInstance(context);
	}
	
	public class AddServerThread extends AsyncTask<Void, Integer, Void> {
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
		
		private void setPopupState(final int progress) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (progress >= 0 && progress <= 100)
						progressBar.setProgress(progress);
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
			
			if (Util.isOnline(context)) {
				if (algo_state != AST_WAITING_FOR_CREDENTIALS && algo_state != AST_WAITING_FOR_URL) {
					final View view = LayoutInflater.from(context).inflate(R.layout.addserver_popup, null);
					
					popup = new AlertDialog.Builder(context)
					.setView(view)
					.setCancelable(false)
					.show();
					popupIsShown = true;
					
					progressBar = (ProgressBar) popup.findViewById(R.id.progressbar);
					progressBar.setProgress(0);
					progressBar.setIndeterminate(true);
					popup_title1 = (TextView) popup.findViewById(R.id.popup_text_a);
					popup_title2 = (TextView) popup.findViewById(R.id.popup_text_b);
					Fonts.setFont(context, popup_title1, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, popup_title2, CustomFont.RobotoCondensed_Regular);
					popupIsShown = true;
					popup_title1.setText(getString(R.string.text43)); // Please wait...
				}
				setPopupState(0);
			}
		}
		
		private int start() {
			if (Util.isOnline(context)) {
				setPopupState(0);
				setPopupText("", getString(R.string.text42));
				
				type = "";
				serverUrl = tb_serverUrl.getText().toString();
				
				ssl = false;
				
				// Modifications de l'URL
				if (!serverUrl.contains("http://") && !serverUrl.contains("https://"))
					serverUrl = "http://" + serverUrl;
				if (serverUrl.contains("https://"))
					ssl = true;
				if (serverUrl.length() > 10 && !serverUrl.substring(serverUrl.length()-1).equals("/") && !serverUrl.contains("/index.html"))
					serverUrl = serverUrl + "/index.html";
				
				if (ssl && !muninFoo.premium)
					return RES_NOT_PREMIUM;
				
				return RES_OK;
			} else
				return RES_NO_CONNECTION;
		}
		
		private void askAgainForUrl(final String err) {
			final EditText et_url = (EditText) popup.findViewById(R.id.popup_url_edittext);
			final Button cancel = (Button) popup.findViewById(R.id.popup_url_cancel);
			final Button continu = (Button) popup.findViewById(R.id.popup_url_continue);
			
			runOnUiThread(new Runnable() {
				public void run() {
					TextView popup_url_message = (TextView)popup.findViewById(R.id.popup_url_message);
					TextView popup_url_message2 = (TextView)popup.findViewById(R.id.popup_url_message2);
					Fonts.setFont(context, popup_url_message, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, popup_url_message2, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, cancel, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, continu, CustomFont.RobotoCondensed_Regular);
					popup_title1.setVisibility(View.GONE);
					popup_title2.setVisibility(View.GONE);
					progressBar.setVisibility(View.GONE);
					popup.findViewById(R.id.popup_url).setVisibility(View.VISIBLE);
					
					if (err != null && err.contains("Timeout")) {
						popup_url_message.setVisibility(View.GONE);
						popup_url_message2.setText(err);
					} else if (err != null && !err.substring(0, 3).equals("200"))
						popup_url_message.setText(err);
					else
						popup_url_message.setVisibility(View.GONE);
					
					et_url.setText(serverUrl);
				}
			});
			
			algo_state = AST_WAITING_FOR_URL;
			
			cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (popup_title1 != null)	popup_title1.setText("");
					if (popup_title2 != null)	popup_title2.setText("");
					
					popupIsShown = false;
					algo_state = AST_IDLE;
					settingsServer = null;
					popup.dismiss();
					muninFoo.resetInstance(context);
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
						algo_state = AST_WAITING_FOR_URL;
						
						runOnUiThread(new Runnable() {
							public void run() {
								tb_serverUrl.setText(url);
								popup_title1.setVisibility(View.VISIBLE);
								popup_title2.setVisibility(View.VISIBLE);
								progressBar.setVisibility(View.VISIBLE);
								popup.findViewById(R.id.popup_url).setVisibility(View.GONE);
								progressBar.setIndeterminate(true);
							}
						});
						
						task = new AddServerThread();
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
						popup.dismiss();
						popupIsShown = false;
					}
				});
			}
			
			task.cancel(true);
			settingsServer = null;
			muninFoo.resetInstance(context);
			algo_state = AST_IDLE;
			
			if (res == RES_NOT_PREMIUM) {
				// ssl Support is available with the Munin for Android Features Pack. Do you want to purchase it on Google Play?
				message_title = "";
				if (s.equals("digest"))
					message_text = getString(R.string.text65_1);
				else // ssl
					message_text = getString(R.string.text41);
				
				runOnUiThread(new Runnable() {
					public void run() {
						AlertDialog.Builder builder = new AlertDialog.Builder(Activity_Server.this);
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
									final AlertDialog ad = new AlertDialog.Builder(Activity_Server.this).create();
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
								Intent intent = new Intent(Activity_Server.this, Activity_GoPremium.class);
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
						Toast.makeText(Activity_Server.this, getString(R.string.text30), Toast.LENGTH_LONG).show();
					}
				});
			}
			else if (res == RES_MALFORMED_URL) {
				// Popup pas ouverte à cet endroit -> dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(Activity_Server.this);
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
			settingsServer.setSSL(ssl);
			
			type = settingsServer.detectPageType();
			
			// The first connection to the server has been done : settingsServer.ssl has been set to true if necessary.
			// If not premium :
			// If ssl was true : we're not supposed to be there.
			// If ssl was false and is now true : display error msg.
			if (!muninFoo.premium && settingsServer.getSSL())
				type = "RES_NOT_PREMIUM";
			
			progressBar.setIndeterminate(false);
			
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
			final EditText et_login = (EditText) popup.findViewById(R.id.popup_credentials_login);
			final EditText et_password = (EditText) popup.findViewById(R.id.popup_credentials_password);
			final Button cancel = (Button) popup.findViewById(R.id.popup_credentials_cancel);
			final Button continu = (Button) popup.findViewById(R.id.popup_credentials_continue);
			final Spinner pop_sp_authType = (Spinner) popup.findViewById(R.id.popup_credentials_authtype);
			
			runOnUiThread(new Runnable() {
				public void run() {
					popup_title1.setVisibility(View.GONE);
					popup_title2.setVisibility(View.GONE);
					progressBar.setVisibility(View.GONE);
					popup.findViewById(R.id.popup_credentials).setVisibility(View.VISIBLE);
					Fonts.setFont(context, cancel, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, continu, CustomFont.RobotoCondensed_Regular);
					
					// Remplissage spinner auth type
					List<String> list2 = new ArrayList<String>();
					list2.add("Basic");
					list2.add("Digest");
					ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, list2);
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
									popup.findViewById(R.id.popup_credentials_premium).setVisibility(View.VISIBLE);
								else
									popup.findViewById(R.id.popup_credentials_premium).setVisibility(View.GONE);
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
					
					runOnUiThread(new Runnable() {
						public void run() {
							pop_sp_authType.setSelection(0);
							et_login.setText("");
							et_password.setText("");
							popup_title1.setVisibility(View.VISIBLE);
							popup_title2.setVisibility(View.VISIBLE);
							progressBar.setVisibility(View.VISIBLE);
							popup.findViewById(R.id.popup_credentials).setVisibility(View.GONE);
						}
					});
					popupIsShown = false;
					algo_state = AST_IDLE;
					settingsServer = null;
					popup.dismiss();
					muninFoo.resetInstance(context);
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
								progressBar.setVisibility(View.VISIBLE);
								popup.findViewById(R.id.popup_credentials).setVisibility(View.GONE);
							}
						});
						
						task = new AddServerThread();
						task.execute();
					}
				}
			});
			this.cancel(true);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			popup.findViewById(R.id.popup_credentials).setVisibility(View.GONE);
			popup.findViewById(R.id.popup_url).setVisibility(View.GONE);
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
				Button b = (Button) popup.findViewById(R.id.popup_button);
				Fonts.setFont(context, b, CustomFont.RobotoCondensed_Regular);
				
				if (res == RES_SERVER_SUCCESS) {
					// Congratulations!			X plugins found!
					setPopupText(getString(R.string.text18), message_title + " " + getString(R.string.text27));
					progressBar.setVisibility(View.GONE);
					b.setVisibility(View.VISIBLE);
					b.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							popup.dismiss();
							Intent intent = new Intent(Activity_Server.this, Activity_Servers.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							Util.setTransition(context, TransitionStyle.SHALLOWER);
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
						}
					});
				} else if (res == RES_SERVERS_SUCCESS) {
					setPopupText(message_title, message_text);
					progressBar.setVisibility(View.GONE);
					b.setVisibility(View.VISIBLE);
					b.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							popup.dismiss();
							Intent intent = new Intent(Activity_Server.this, Activity_Servers.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							Util.setTransition(context, TransitionStyle.SHALLOWER);
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
						}
					});
				}
			}
			if (MuninFoo.DEBUG)
				muninFoo.sqlite.logMasters();
			if (!Util.isOnline(context))
				Toast.makeText(Activity_Server.this, getString(R.string.text30), Toast.LENGTH_LONG).show();
		}
	}
	
	private void addInHistory(String url) {
		boolean contains = false;
		for (String s : getHistory()) {
			if (s.equals(url))
				contains = true;
		}
		if (!contains) {
			String his = Util.getPref(context, "addserver_history");
			his += url.replaceAll(";", ",") + ";";
			Util.setPref(context, "addserver_history", his);
		}
	}
	
	private String[] getHistory() {
		String his = Util.getPref(context, "addserver_history");
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