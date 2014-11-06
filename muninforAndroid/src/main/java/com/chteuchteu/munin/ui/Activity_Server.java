package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.GraphWidget;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;

import java.util.ArrayList;
import java.util.List;

/**
 * Only used in order to add a server
 */

@SuppressLint("CommitPrefEdits")
public class Activity_Server extends MuninActivity {
	private Spinner  	spinner;
	private AutoCompleteTextView tb_serverUrl;
	
	// Alert dialog
	private ProgressBar progressBar;
	private TextView 	alert_title1;
	private TextView 	alert_title2;
	private AlertDialog alert;
	private boolean 	alertIsShown;
	private View		cancelButton;
	
	private MuninMaster master;
	
	// AddServer stuff
	private String 	serverUrl;
	private String 	type;
	private String 	message_title;
	private String 	message_text;
	private AddServerThread task;
	private int		algo_state = 0;
	private static final int AST_IDLE = 0;
	private static final int AST_RUNNING = 1;
	private static final int AST_WAITING_FOR_URL = 2;
	private static final int AST_WAITING_FOR_CREDENTIALS = 3;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_server);
		super.onContentViewSet();
		dh.setDrawerActivity(DrawerHelper.Activity_Server_Add);

		actionBar.setTitle(getString(R.string.addServerTitle)); // Add a server

		Util.Fonts.setFont(this, (TextView) findViewById(R.id.muninMasterUrlLabel), CustomFont.Roboto_Medium);
		
		spinner = (Spinner)findViewById(R.id.spinner);
		tb_serverUrl = (AutoCompleteTextView)findViewById(R.id.textbox_serverUrl);
		
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
	
	@Override
	public void onBackPressed() {
		// Hitting "back" within the AlertDialog will call its own onBackPressed
		// (which can't be overriden BTW).
		if (!alertIsShown) {
			Intent intent = new Intent(this, Activity_Servers.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(context, TransitionStyle.SHALLOWER);
		}
	}
	
	// Actions
	private void actionSave() {
		if (!tb_serverUrl.getText().toString().equals("") && !tb_serverUrl.getText().toString().equals("http://")) {
			addInHistory(tb_serverUrl.getText().toString().trim());
			Util.hideKeyboard(this, tb_serverUrl);
			algo_state = AST_RUNNING;
			task = new AddServerThread();
			task.execute();
		}
	}

	
	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.server, menu);
		
		if (Util.getPref(context, "addserver_history").equals(""))
			menu.findItem(R.id.menu_clear_history).setVisible(false);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_save:	actionSave();		return true;
			case R.id.menu_clear_history:
				Util.setPref(context, "addserver_history", "");
				createOptionsMenu();
				Toast.makeText(getApplicationContext(), getString(R.string.text66_1), Toast.LENGTH_SHORT).show();
				return true;
		}

		return true;
	}
	
	private void cancelSave() {
		if (alert_title1 != null)	alert_title1.setText("");
		if (alert_title2 != null)	alert_title2.setText("");
		task.cancel(true);
		alertIsShown = false;
		algo_state = AST_IDLE;
		master = null;
		alert.dismiss();
		muninFoo.resetInstance(context);
	}
	
	@SuppressLint("InflateParams")
	private class AddServerThread extends AsyncTask<Void, Integer, Void> {
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
					if (alert_title1.getVisibility() == View.GONE)	alert_title1.setVisibility(View.VISIBLE);
					if (alert_title2.getVisibility() == View.GONE)	alert_title2.setVisibility(View.VISIBLE);
					if (!title1.equals(""))	alert_title1.setText(title1);
					if (!title2.equals(""))	alert_title2.setText(title2);
				}
			});
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			if (Util.isOnline(context)) {
				if (algo_state != AST_WAITING_FOR_CREDENTIALS && algo_state != AST_WAITING_FOR_URL) {
					final View view = LayoutInflater.from(context).inflate(R.layout.server_popup, null);
					
					alert = new AlertDialog.Builder(context)
					.setView(view)
					.setCancelable(false)
					.show();
					alertIsShown = true;
					
					progressBar = (ProgressBar) alert.findViewById(R.id.progressbar);
					progressBar.setProgress(0);
					progressBar.setIndeterminate(true);
					alert_title1 = (TextView) alert.findViewById(R.id.popup_text_a);
					alert_title2 = (TextView) alert.findViewById(R.id.popup_text_b);
					Fonts.setFont(context, alert_title1, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, alert_title2, CustomFont.RobotoCondensed_Regular);
					alertIsShown = true;
					alert_title1.setText(getString(R.string.text43)); // Please wait...
					cancelButton = alert.findViewById(R.id.cancelButton);
					
					cancelButton.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							cancelSave();
						}
					});
				}
				setPopupState(0);
			}
		}
		
		private int start() {
			if (Util.isOnline(context)) {
				setPopupState(0);
				setPopupText("", getString(R.string.text42));
				
				type = "";
				serverUrl = tb_serverUrl.getText().toString().trim();
				
				boolean ssl = false;
				
				// URL modifications
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
			final EditText et_url = (EditText) alert.findViewById(R.id.popup_url_edittext);
			final Button cancel = (Button) alert.findViewById(R.id.popup_url_cancel);
			final Button continu = (Button) alert.findViewById(R.id.popup_url_continue);
			
			runOnUiThread(new Runnable() {
				public void run() {
					TextView popup_url_message = (TextView)alert.findViewById(R.id.popup_url_message);
					TextView popup_url_message2 = (TextView)alert.findViewById(R.id.popup_url_message2);
					Fonts.setFont(context, popup_url_message, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, popup_url_message2, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, cancel, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, continu, CustomFont.RobotoCondensed_Regular);
					alert_title1.setVisibility(View.GONE);
					alert_title2.setVisibility(View.GONE);
					progressBar.setVisibility(View.GONE);
					alert.findViewById(R.id.popup_url).setVisibility(View.VISIBLE);
					
					if (err != null && err.contains("Timeout")) {
						popup_url_message.setVisibility(View.GONE);
						popup_url_message2.setText(err);
					} else if (err != null && err.length() > 3 && !err.substring(0, 3).equals("200"))
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
					if (alert_title1 != null)	alert_title1.setText("");
					if (alert_title2 != null)	alert_title2.setText("");
					
					alertIsShown = false;
					algo_state = AST_IDLE;
					master = null;
					alert.dismiss();
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
						master.setUrl(url);
						//InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						//imm.hideSoftInputFromWindow(tb_serverUrl.getWindowToken(), 0);
						//imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
						algo_state = AST_WAITING_FOR_URL;
						
						runOnUiThread(new Runnable() {
							public void run() {
								tb_serverUrl.setText(url);
								alert_title1.setVisibility(View.VISIBLE);
								alert_title2.setVisibility(View.VISIBLE);
								progressBar.setVisibility(View.VISIBLE);
								alert.findViewById(R.id.popup_url).setVisibility(View.GONE);
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
			if (alertIsShown) {
				runOnUiThread(new Runnable() {
					public void run() {
						alert.dismiss();
						alertIsShown = false;
					}
				});
			}
			
			task.cancel(true);
			master = null;
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
			setPopupText(getString(R.string.text44), "");
			
			/* 			DETECTION DU TYPE D'URL 		*/
			if (master == null) {
				master = new MuninMaster();
				master.setUrl(serverUrl);
			}
			type = master.detectPageType();
			
			// The first connection to the server has been done : settingsServer.ssl has been set to true if necessary.
			// If not premium :
			// If ssl was true : we're not supposed to be there.
			// If ssl was false and is now true : display error msg.
			if (!muninFoo.premium && master.getSSL())
				type = "RES_NOT_PREMIUM";
			
			progressBar.setIndeterminate(false);
			
			return type;
		}
		
		private int finish() {
			int ret = RES_UNDEFINED;
			
			if (type.equals("munin/")) {
				/*		CONTENT OF THE PAGE: SERVERS LIST	*/
				int nbNewServers = master.fetchChildren();
				
				boolean fetchSuccess = nbNewServers > 0;
				
				if (fetchSuccess) {
					int popupstate = 30;
					setPopupState(popupstate);
					
					// Plugins lookup for each server
					for (MuninServer server : master.getChildren()) {
						setPopupText("", getString(R.string.text46) + " " + (master.getChildren().indexOf(server)+1) + "/" + nbNewServers);
						
						server.fetchPluginsList();
						
						if (popupstate < 80) {
							popupstate += Math.round(50/nbNewServers);
							setPopupState(popupstate);
						}
					}

					setPopupState(100);
					setPopupText(getString(R.string.text45), " ");

					cancelButton.getHandler().post(new Runnable() {
						public void run() {
							cancelButton.setVisibility(View.GONE);
						}
					});

					// Check if there is already a master with this url
					MuninMaster alreadyThereMaster = null;
					for (MuninMaster muninFooMaster : muninFoo.getMasters()) {
						if (muninFooMaster.equalsApprox(master)) {
							alreadyThereMaster = muninFooMaster;
							break;
						}
					}

					ArrayList<GraphWidget> widgetsToUpdate = new ArrayList<GraphWidget>();
					ArrayList<Label> labelsToUpdate = new ArrayList<Label>();
					ArrayList<GridItem> gridItemsToUpdate = new ArrayList<GridItem>();
					if (alreadyThereMaster != null) {
						// Replace
						// Check if there are labels / widgets / grids in the hierarchy
						widgetsToUpdate = master.reattachWidgets(muninFoo, alreadyThereMaster);
						labelsToUpdate = master.reattachLabels(muninFoo, alreadyThereMaster);
						gridItemsToUpdate = master.reattachGrids(muninFoo, context, alreadyThereMaster);
					}

					// Delete old duplicate
					if (alreadyThereMaster != null) {
						muninFoo.sqlite.dbHlpr.deleteMaster(alreadyThereMaster, true);
						muninFoo.getServers().removeAll(alreadyThereMaster.getChildren());
						muninFoo.getMasters().remove(alreadyThereMaster);
					}

					muninFoo.getMasters().add(master);
					muninFoo.getServers().addAll(master.getChildren());
					// Insert master
					muninFoo.sqlite.insertMuninMaster(master);

					// Widgets, labels and gridItems have been deleted from DB
					// (recursive delete). Let's add them if needed
					// Save reattached widgets if needed
					for (GraphWidget graphWidget : widgetsToUpdate)
						muninFoo.sqlite.dbHlpr.insertGraphWidget(graphWidget);
					// Save reattached labels if needed
					for (Label label : labelsToUpdate) {
						for (MuninPlugin plugin : label.plugins)
							muninFoo.sqlite.dbHlpr.insertLabelRelation(plugin, label);
					}
					// Save reattached grid items if needed
					for (GridItem gridItem : gridItemsToUpdate)
						muninFoo.sqlite.dbHlpr.insertGridItemRelation(gridItem);

					cancelButton.getHandler().post(new Runnable() {
						public void run() {
							cancelButton.setVisibility(View.VISIBLE);
						}
					});

					// Success!
					message_title = getString(R.string.text18);

					String s = "";
					if (nbNewServers > 1)	s = "s";
					// X sub-server(s) added!
					message_text = nbNewServers + " " + getString(R.string.text21_1) + s + " " + getString(R.string.text21_2);

					return RES_SERVERS_SUCCESS;
				}
			}	// ending if (type.equals("munin/")) (servers)
			/*else if (type.equals("munin/x/")) {
				// TODO : get parent page
			}*/
			return ret;
		}
		
		private void askForCredentials() {
			final EditText et_login = (EditText) alert.findViewById(R.id.popup_credentials_login);
			final EditText et_password = (EditText) alert.findViewById(R.id.popup_credentials_password);
			final Button cancel = (Button) alert.findViewById(R.id.popup_credentials_cancel);
			final Button continu = (Button) alert.findViewById(R.id.popup_credentials_continue);
			final Spinner pop_sp_authType = (Spinner) alert.findViewById(R.id.popup_credentials_authtype);
			
			runOnUiThread(new Runnable() {
				public void run() {
					alert_title1.setVisibility(View.GONE);
					alert_title2.setVisibility(View.GONE);
					progressBar.setVisibility(View.GONE);
					alert.findViewById(R.id.popup_credentials).setVisibility(View.VISIBLE);
					Fonts.setFont(context, cancel, CustomFont.RobotoCondensed_Regular);
					Fonts.setFont(context, continu, CustomFont.RobotoCondensed_Regular);
					
					// AuthType spinner
					List<String> list2 = new ArrayList<String>();
					list2.add("Basic");
					list2.add("Digest");
					ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, list2);
					dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					pop_sp_authType.setAdapter(dataAdapter2);
					
					if (master.isAuthNeeded()) {
						et_login.setText(master.getAuthLogin());
						et_password.setText(master.getAuthPassword());
						if (master.getAuthType() == AuthType.BASIC)
							pop_sp_authType.setSelection(0);
						else if (master.getAuthType() == AuthType.DIGEST)
							pop_sp_authType.setSelection(1);
					}
					
					pop_sp_authType.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> arg0, View arg1, int select, long arg3) {
							if (!muninFoo.premium) {
								if (select == 1)
									alert.findViewById(R.id.popup_credentials_premium).setVisibility(View.VISIBLE);
								else
									alert.findViewById(R.id.popup_credentials_premium).setVisibility(View.GONE);
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
					if (alert_title1 != null)	alert_title1.setText("");
					if (alert_title2 != null)	alert_title2.setText("");
					
					runOnUiThread(new Runnable() {
						public void run() {
							pop_sp_authType.setSelection(0);
							et_login.setText("");
							et_password.setText("");
							alert_title1.setVisibility(View.VISIBLE);
							alert_title2.setVisibility(View.VISIBLE);
							progressBar.setVisibility(View.VISIBLE);
							alert.findViewById(R.id.popup_credentials).setVisibility(View.GONE);
						}
					});
					alertIsShown = false;
					algo_state = AST_IDLE;
					master = null;
					alert.dismiss();
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
						master.setAuthIds(login, password);
						if (pop_sp_authType.getSelectedItemPosition() == 0)
							master.setAuthType(AuthType.BASIC);
						else
							master.setAuthType(AuthType.DIGEST);
						
						algo_state = AST_RUNNING;
						
						runOnUiThread(new Runnable() {
							public void run() {
								alert_title1.setVisibility(View.VISIBLE);
								alert_title2.setVisibility(View.VISIBLE);
								progressBar.setVisibility(View.VISIBLE);
								alert.findViewById(R.id.popup_credentials).setVisibility(View.GONE);
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
			alert.findViewById(R.id.popup_credentials).setVisibility(View.GONE);
			alert.findViewById(R.id.popup_url).setVisibility(View.GONE);
			// Don't execute steps that have already been done
			boolean stop = false;
			if (algo_state != AST_WAITING_FOR_CREDENTIALS) {
				int res1 = start();
				if (res1 == RES_NO_CONNECTION || res1 == RES_NOT_PREMIUM || res1 == RES_MALFORMED_URL) {
					cancelFetch(res1);
					stop = true;
				}
			}
			
			// Show alert dialog
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
			muninFoo.updateCurrentServer(context);

			cancelButton.setVisibility(View.GONE);
			algo_state = AST_IDLE;
			if (res != RES_UNDEFINED) {
				Button b = (Button) alert.findViewById(R.id.popup_button);
				Fonts.setFont(context, b, CustomFont.RobotoCondensed_Regular);
				
				if (res == RES_SERVER_SUCCESS) {
					// Congratulations!			X plugins found!
					setPopupText(getString(R.string.text18), message_title + " " + getString(R.string.text27));
					progressBar.setVisibility(View.GONE);
					b.setVisibility(View.VISIBLE);
					b.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							alert.dismiss();
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
							alert.dismiss();
							muninFoo.setCurrentServer(master.getChildren().get(0));
							Intent intent = new Intent(Activity_Server.this, Activity_Plugins.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							Util.setTransition(context, TransitionStyle.SHALLOWER);
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
						}
					});
				}
			}
			if (BuildConfig.DEBUG)
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

}