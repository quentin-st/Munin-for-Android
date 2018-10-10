package com.chteuchteu.munin.async;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.BuildConfig;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.GraphWidget;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.ui.Activity_GoPremium;
import com.chteuchteu.munin.ui.Activity_Plugins;
import com.chteuchteu.munin.ui.Activity_Server;
import com.chteuchteu.munin.ui.Activity_Servers;

import java.util.ArrayList;
import java.util.List;

/**
 * Used in Activity_Server
 */
public class ServerScanner extends AsyncTask<Void, Integer, Void> {
    private Activity_Server activity;
    private Context context;
    private MuninFoo muninFoo;

    private ProgressBar progressBar;
    private TextView 	alert_title2;
    private View		cancelButton;

    private String 	serverUrl;
    private String 	type;
    private String 	message_title;
    private String 	message_text;

    public enum ScannerState { IDLE, RUNNING, WAITING_FOR_URL, WAITING_FOR_CREDENTIALS }
    public ScannerState scannerState;

	private enum ReturnCode {
		UNDEFINED, SERVER_SUCCESS, SERVERS_SUCCESS, OK, // Success
		NOT_PREMIUM, NO_CONNECTION, MALFORMED_URL // Error
	}
	private ReturnCode returnCode;

    public ServerScanner(Activity_Server activity) {
        this.activity = activity;
        this.context = activity;
        this.muninFoo = MuninFoo.getInstance(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (Util.isOnline(context)) {
            if (scannerState != ScannerState.WAITING_FOR_CREDENTIALS
                    && scannerState != ScannerState.WAITING_FOR_URL) {
	            if (!activity.isAlertShown) {
		            final View view = LayoutInflater.from(context).inflate(R.layout.server_popup, null, false);

		            activity.alertDialog = new AlertDialog.Builder(context)
                            .setTitle(R.string.loading)
				            .setView(view)
				            .setCancelable(false)
				            .show();
		            activity.isAlertShown = true;
	            }

                progressBar = (ProgressBar) activity.alertDialog.findViewById(R.id.progressbar);
                progressBar.setProgress(0);
                progressBar.setIndeterminate(true);
                alert_title2 = (TextView) activity.alertDialog.findViewById(R.id.popup_text_b);
                cancelButton = activity.alertDialog.findViewById(R.id.cancelButton);

                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.cancelSave();
                    }
                });
            }
            setPopupState(0);
        }

        activity.alertDialog.findViewById(R.id.popup_credentials).setVisibility(View.GONE);
        activity.alertDialog.findViewById(R.id.popup_url).setVisibility(View.GONE);
    }

    private ReturnCode start() {
        if (Util.isOnline(context)) {
            setPopupState(0);
            setPopupText("", activity.getString(R.string.text42));

            type = "";
            serverUrl = activity.tv_serverUrl.getText().toString().trim();

            // URL modifications
            if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://"))
                serverUrl = "http://" + serverUrl;

            // Add trailing slash
            if (!serverUrl.endsWith("/"))
                serverUrl += "/";

            return ReturnCode.OK;
        } else
            return ReturnCode.NO_CONNECTION;
    }

    private void askAgainForUrl(final String err) {
        final EditText et_url = (EditText) activity.alertDialog.findViewById(R.id.popup_url_edittext);
        final Button cancel = (Button) activity.alertDialog.findViewById(R.id.popup_url_cancel);
        final Button continu = (Button) activity.alertDialog.findViewById(R.id.popup_url_continue);

        activity.runOnUiThread(new Runnable() {
            public void run() {
                TextView popup_url_message = (TextView) activity.alertDialog.findViewById(R.id.popup_url_message);
                TextView popup_url_message2 = (TextView) activity.alertDialog.findViewById(R.id.popup_url_message2);
                activity.alertDialog.setTitle("");
                alert_title2.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                activity.alertDialog.findViewById(R.id.popup_url).setVisibility(View.VISIBLE);

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

        scannerState = ScannerState.WAITING_FOR_URL;

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alert_title2 != null)	alert_title2.setText("");

                activity.isAlertShown = false;
                scannerState = ScannerState.IDLE;
                activity.master = null;
                activity.alertDialog.dismiss();
                muninFoo.resetInstance(context);
            }
        });
        continu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String url = et_url.getText().toString();
                serverUrl = url;

                activity.master.setUrl(url);
                //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                //imm.hideSoftInputFromWindow(tv_serverUrl.getWindowToken(), 0);
                //imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
                scannerState = ScannerState.WAITING_FOR_URL;

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        activity.tv_serverUrl.setText(url);
                        alert_title2.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        activity.alertDialog.findViewById(R.id.popup_url).setVisibility(View.GONE);
                        progressBar.setIndeterminate(true);
                    }
                });

                stop();
                activity.task = new ServerScanner(activity);
                activity.task.execute();
            }
        });
        this.cancel(true);
    }

    private void cancelFetch(ReturnCode returnCode) { cancelFetch(returnCode, ""); }
    private void cancelFetch(ReturnCode returnCode, final String s) {
        if (activity.isAlertShown) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    activity.alertDialog.dismiss();
                    activity.isAlertShown = false;
                }
            });
        }

        activity.task.cancel(true);
        activity.master = null;
        muninFoo.resetInstance(context);
        scannerState = ScannerState.IDLE;

	    switch (returnCode) {
		    case NOT_PREMIUM:
			    message_title = "";
			    if (s.equals("digest"))
				    message_text = activity.getString(R.string.text65_1);

			    activity.runOnUiThread(new Runnable() {
				    public void run() {
					    AlertDialog.Builder builder = new AlertDialog.Builder(context);
					    builder.setMessage(message_text)
							    .setCancelable(false)
									    // Yes
							    .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
								    public void onClick(DialogInterface dialog, int id) {
									    try {
										    Intent intent = new Intent(Intent.ACTION_VIEW);
										    intent.setData(Uri.parse("market://details?id=com.chteuchteu.muninforandroidfeaturespack"));
										    activity.startActivity(intent);
									    } catch (Exception ex) {
										    final AlertDialog ad = new AlertDialog.Builder(context).create();
										    // Error!
										    ad.setTitle(context.getString(R.string.text09));
										    ad.setMessage(context.getString(R.string.text11));
										    ad.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.ok), new DialogInterface.OnClickListener() {
											    public void onClick(DialogInterface dialog, int which) { ad.dismiss(); }
										    });
										    ad.setIcon(R.drawable.alerts_and_states_error);
										    ad.show();
									    }
								    }
							    })
									    // Learn more...
							    .setNeutralButton(context.getString(R.string.text35), new DialogInterface.OnClickListener() {
								    public void onClick(DialogInterface dialog, int id) {
									    Intent intent = new Intent(context, Activity_GoPremium.class);
									    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
									    activity.startActivity(intent);
								    }
							    })
									    // No
							    .setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
								    public void onClick(DialogInterface dialog, int id) {
									    dialog.cancel();
								    }
							    });
					    AlertDialog dialog = builder.create();
					    dialog.show();
				    }
			    });
			    break;
		    case NO_CONNECTION:
			    activity.runOnUiThread(new Runnable() {
				    public void run() {
					    Toast.makeText(context, context.getString(R.string.text30), Toast.LENGTH_LONG).show();
				    }
			    });
			    break;
		    case MALFORMED_URL:
			    AlertDialog.Builder builder = new AlertDialog.Builder(context);
			    builder.setMessage(context.getString(R.string.text16))
					    .setCancelable(true)
					    .setPositiveButton(context.getString(R.string.text64), new DialogInterface.OnClickListener() {
						    public void onClick(DialogInterface dialog, int id) {
							    dialog.dismiss();
						    }
					    });
			    AlertDialog dialog = builder.create();
			    dialog.show();
			    break;
	    }
    }

    private String initialization() {
        setPopupText(context.getString(R.string.text44), "");

			/* 			DETECT URL TYPE 		*/
        if (activity.master == null) {
            activity.master = new MuninMaster();
            activity.master.setUrl(serverUrl);
        }
        type = activity.master.detectPageType(muninFoo.getUserAgent());


        activity.runOnUiThread(new Runnable() {
            public void run() {
                progressBar.setIndeterminate(false);
            }
        });

        return type;
    }

    private ReturnCode finish() {
        ReturnCode ret = ReturnCode.UNDEFINED;

        if (type.equals("munin/")) {
			/*		CONTENT OF THE PAGE: NODES LIST	*/
            int nbNewNodes = activity.master.fetchChildren(muninFoo.getUserAgent());

            boolean fetchSuccess = nbNewNodes > 0;

            if (fetchSuccess) {
                int popupstate = 30;
                setPopupState(popupstate);

                // Plugins lookup for each node
                for (MuninNode node : activity.master.getChildren()) {
                    setPopupText("", context.getString(R.string.text46) + " " + (activity.master.getChildren().indexOf(node)+1) + "/" + nbNewNodes);

                    node.fetchPluginsList(muninFoo.getUserAgent());

                    if (popupstate < 80) {
                        popupstate += Math.round(50/nbNewNodes);
                        setPopupState(popupstate);
                    }
                }

                setPopupState(100);
                setPopupText(context.getString(R.string.text45), " ");
                setPopupState(-1);

                cancelButton.getHandler().post(new Runnable() {
                    public void run() {
                        cancelButton.setVisibility(View.GONE);
                    }
                });

                // Check if there is already a activity.master with this url
                MuninMaster alreadyThereMaster = null;
                for (MuninMaster muninFooMaster : muninFoo.getMasters()) {
                    if (muninFooMaster.equalsApprox(activity.master)) {
                        alreadyThereMaster = muninFooMaster;
                        break;
                    }
                }

                List<GraphWidget> widgetsToUpdate = new ArrayList<>();
                List<Label> labelsToUpdate = new ArrayList<>();
                List<GridItem> gridItemsToUpdate = new ArrayList<>();
                if (alreadyThereMaster != null) {
                    // Replace
                    // Check if there are labels / widgets / grids in the hierarchy
                    widgetsToUpdate = activity.master.reattachWidgets(muninFoo, alreadyThereMaster);
                    labelsToUpdate = activity.master.reattachLabels(muninFoo, alreadyThereMaster);
                    gridItemsToUpdate = activity.master.reattachGrids(muninFoo, alreadyThereMaster);
                }

                // Delete old duplicate
                if (alreadyThereMaster != null) {
                    muninFoo.sqlite.dbHlpr.deleteMaster(alreadyThereMaster, true);
                    muninFoo.getNodes().removeAll(alreadyThereMaster.getChildren());
                    muninFoo.getMasters().remove(alreadyThereMaster);
                }

                muninFoo.getMasters().add(activity.master);
                muninFoo.getNodes().addAll(activity.master.getChildren());
                // Insert activity.master
                muninFoo.sqlite.insertMuninMaster(activity.master);

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
                message_title = context.getString(R.string.text18);

                // X node(s) added!
                message_text = nbNewNodes + " " + context.getString(R.string.text21_1) + (nbNewNodes > 1 ? "s" : "") + " " + context.getString(R.string.text21_2);

                return ReturnCode.SERVERS_SUCCESS;
            }
        }
        /*else if (type.equals("munin/x/")) {
            // TODO : get parent page
        }*/
        return ret;
    }

    private void askForCredentials() {
        final EditText et_login = (EditText) activity.alertDialog.findViewById(R.id.popup_credentials_login);
        final EditText et_password = (EditText) activity.alertDialog.findViewById(R.id.popup_credentials_password);
        final Button cancel = (Button) activity.alertDialog.findViewById(R.id.popup_credentials_cancel);
        final Button continu = (Button) activity.alertDialog.findViewById(R.id.popup_credentials_continue);
        final Spinner pop_sp_authType = (Spinner) activity.alertDialog.findViewById(R.id.popup_credentials_authtype);

        activity.runOnUiThread(new Runnable() {
            public void run() {
                activity.alertDialog.setTitle(context.getString(R.string.settings_http_auth2));
                alert_title2.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                activity.alertDialog.findViewById(R.id.popup_credentials).setVisibility(View.VISIBLE);

                // AuthType spinner
                List<String> list2 = new ArrayList<>();
                list2.add("Basic");
                list2.add("Digest");
                ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, list2);
                dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                pop_sp_authType.setAdapter(dataAdapter2);

                if (activity.master.isAuthNeeded()) {
                    et_login.setText(activity.master.getAuthLogin());
                    et_password.setText(activity.master.getAuthPassword());
                    if (activity.master.getAuthType() == MuninMaster.AuthType.BASIC)
                        pop_sp_authType.setSelection(0);
                    else if (activity.master.getAuthType() == MuninMaster.AuthType.DIGEST)
                        pop_sp_authType.setSelection(1);
                }

                pop_sp_authType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int select, long arg3) {
                        if (!muninFoo.premium) {
                            activity.alertDialog.findViewById(R.id.popup_credentials_premium)
                                    .setVisibility(select == 1 ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override public void onNothingSelected(AdapterView<?> arg0) { }
                });
            }
        });

        scannerState = ScannerState.WAITING_FOR_CREDENTIALS;

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alert_title2 != null)	alert_title2.setText("");

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        pop_sp_authType.setSelection(0);
                        et_login.setText("");
                        et_password.setText("");
                        alert_title2.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        activity.alertDialog.findViewById(R.id.popup_credentials).setVisibility(View.GONE);
                    }
                });
                activity.isAlertShown = false;
                scannerState = ScannerState.IDLE;
                activity.master = null;
                activity.alertDialog.dismiss();
                muninFoo.resetInstance(context);
            }
        });
        continu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!muninFoo.premium && pop_sp_authType.getSelectedItemPosition() == 1)
                    cancelFetch(ReturnCode.NOT_PREMIUM, "digest");
                else {
                    String login = et_login.getText().toString();
                    String password = et_password.getText().toString();
                    activity.master.setAuthIds(login, password);
                    if (pop_sp_authType.getSelectedItemPosition() == 0)
                        activity.master.setAuthType(MuninMaster.AuthType.BASIC);
                    else
                        activity.master.setAuthType(MuninMaster.AuthType.DIGEST);

                    scannerState = ScannerState.RUNNING;

                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            alert_title2.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.VISIBLE);
                            activity.alertDialog.findViewById(R.id.popup_credentials).setVisibility(View.GONE);
                        }
                    });

	                stop();
                    activity.task = new ServerScanner(activity);
                    activity.task.execute();
                }
            }
        });
        this.cancel(true);
    }

    public boolean stop() {
        activity.alertDialog.setTitle("");
        if (alert_title2 != null)
            alert_title2.setText("");
        activity.alertDialog.dismiss();
	    activity.isAlertShown = false;
        scannerState = ScannerState.IDLE;

        return super.cancel(true);
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        // Don't execute steps that have already been done
        boolean stop = false;
        if (scannerState != ScannerState.WAITING_FOR_CREDENTIALS) {
            ReturnCode res1 = start();
            if (res1 == ReturnCode.NO_CONNECTION || res1 == ReturnCode.NOT_PREMIUM || res1 == ReturnCode.MALFORMED_URL) {
                cancelFetch(res1);
                stop = true;
            }
        }

        // Show activity.alertDialog dialog
        if (!stop) {
            String res2 = initialization();
            if (!res2.equals("munin/") && !res2.equals("munin/x/")) {
                if (res2.length() > 3) {
                    if (res2.equals("RES_NOT_PREMIUM"))
                        cancelFetch(ReturnCode.NOT_PREMIUM);

                    String result = res2.substring(0, 3);
                    if (result.equals("401"))
                        askForCredentials();
                    else if (res2.equals("timeout"))
                        askAgainForUrl(context.getString(R.string.text68));
                    else
                        askAgainForUrl(res2);
                }
                else // RES_UNKNOWN_HTTP_ERROR
                    askAgainForUrl(res2);
            } else {
                returnCode = finish();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        muninFoo.updateCurrentNode(context);

        cancelButton.setVisibility(View.GONE);
        scannerState = ScannerState.IDLE;
        if (returnCode != ReturnCode.UNDEFINED) {
            Button b = (Button) activity.alertDialog.findViewById(R.id.popup_button);

	        switch (returnCode) {
		        case SERVER_SUCCESS:
			        setPopupText(context.getString(R.string.text18), message_title + " " + context.getString(R.string.text27));
			        progressBar.setVisibility(View.GONE);
			        b.setVisibility(View.VISIBLE);
			        b.setOnClickListener(new View.OnClickListener() {
				        @Override
				        public void onClick(View v) {
					        activity.alertDialog.dismiss();
					        Intent intent = new Intent(context, Activity_Servers.class);
					        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					        activity.startActivity(intent);
					        Util.setTransition(activity, Util.TransitionStyle.SHALLOWER);
					        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
				        }
			        });
			        break;
		        case SERVERS_SUCCESS:
			        setPopupText(message_title, message_text);
			        progressBar.setVisibility(View.GONE);
			        b.setVisibility(View.VISIBLE);
			        b.setOnClickListener(new View.OnClickListener() {
				        @Override
				        public void onClick(View v) {
					        activity.alertDialog.dismiss();
					        muninFoo.setCurrentNode(activity.master.getChildren().get(0));
					        Intent intent = new Intent(context, Activity_Plugins.class);
					        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					        activity.startActivity(intent);
					        Util.setTransition(activity, Util.TransitionStyle.SHALLOWER);
					        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
				        }
			        });
			        break;
	        }
        }
        if (BuildConfig.DEBUG)
            muninFoo.sqlite.logMasters();
        if (!Util.isOnline(context))
            Toast.makeText(context, context.getString(R.string.text30), Toast.LENGTH_LONG).show();
    }


    private void setPopupState(final int progress) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (progress >= 0 && progress <= 100)
                    progressBar.setProgress(progress);
                else if (progress < 0)
                    progressBar.setIndeterminate(true);
            }
        });
    }

    private void setPopupText(final String title1, final String title2) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (alert_title2.getVisibility() == View.GONE)
                    alert_title2.setVisibility(View.VISIBLE);
                activity.alertDialog.setTitle(title1);
                if (!title2.equals("")) alert_title2.setText(title2);
            }
        });
    }
}
