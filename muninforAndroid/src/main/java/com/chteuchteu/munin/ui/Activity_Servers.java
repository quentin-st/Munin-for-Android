package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.Adapter_ExpandableListView;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.ImportExportHelper;
import com.chteuchteu.munin.hlpr.ImportExportHelper.Export.ExportRequestMaker;
import com.chteuchteu.munin.hlpr.ImportExportHelper.Import.ImportRequestMaker;
import com.chteuchteu.munin.hlpr.JSONHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("DefaultLocale")
public class Activity_Servers extends MuninActivity {
	private static Context context;
	private ExpandableListView expListView;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;

		setContentView(R.layout.servers);
		super.onContentViewSet();
		dh.setDrawerActivity(DrawerHelper.Activity_Servers);

		actionBar.setTitle(getString(R.string.serversTitle));
		
		expListView = (ExpandableListView) findViewById(R.id.servers_list);
		
		refreshList();
	}
	
	private void refreshList() {
		findViewById(R.id.servers_noserver).setVisibility(View.GONE);
		
		Intent i = getIntent();
		MuninMaster fromServersEdit = null;
		if (i.getExtras() != null && i.getExtras().containsKey("fromMaster"))
			fromServersEdit = muninFoo.getMasterById((int) i.getExtras().getLong("fromMaster"));
		
		List<MuninMaster> masters = muninFoo.masters;
        Map<MuninMaster, List<String>> serversCollection = getServersCollection();
		final Adapter_ExpandableListView expListAdapter = new Adapter_ExpandableListView(this, this, masters, serversCollection);
		expListView.setAdapter(expListAdapter);
		
		if (fromServersEdit != null)
			expListView.expandGroup(muninFoo.getMasterPosition(fromServersEdit));
		
		if (muninFoo.getServers().isEmpty())
			findViewById(R.id.servers_noserver).setVisibility(View.VISIBLE);
	}
	
	private Map<MuninMaster, List<String>> getServersCollection() {
		// Create collection
		LinkedHashMap<MuninMaster, List<String>> serversCollection = new LinkedHashMap<MuninMaster, List<String>>();
		
		for (MuninMaster m : muninFoo.masters) {
			List<String> childList = new ArrayList<String>();
			for (MuninServer s : m.getOrderedChildren())
				childList.add(s.getName());
			serversCollection.put(m, childList);
		}
		
		return serversCollection;
	}
	
	/**
	 * Called when a click event is triggered on a child-level element of the listview
	 * Called from @see com.chteuchteu.munin.Adapter_ExpandableListView#getChildView(int, int, boolean, View, android.view.ViewGroup)
	 * @param groupPosition
	 * @param childPosition
	 */
	public void onChildClick(int groupPosition, int childPosition) {
		Toast.makeText(this, R.string.long_click, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Called when a long click event is triggered on a child-level element of the listview
	 * Called from @see com.chteuchteu.munin.Adapter_ExpandableListView#getChildView(int, int, boolean, View, android.view.ViewGroup)
	 * @param groupPosition
	 * @param childPosition
	 * @return
	 */
	public boolean onChildLongClick(int groupPosition, int childPosition) {
		final MuninServer server = muninFoo.masters.get(groupPosition).getServerFromFlatPosition(childPosition);
		
		// Display actions list
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				context, android.R.layout.simple_list_item_1);
		arrayAdapter.add(context.getString(R.string.menu_addserver_delete));
		
		builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0: // Delete server
						new AlertDialog.Builder(context)
						.setTitle(R.string.delete)
						.setMessage(R.string.text83)
						.setPositiveButton(R.string.text33, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// When going back : expand the list to the current master if possible
								MuninMaster m = null;
								if (server.getParent() != null && server.getParent().getChildren().size() > 1)
									m = server.getParent();
								
								muninFoo.sqlite.dbHlpr.deleteServer(server);
								muninFoo.deleteServer(server, true);
								
								// Delete labels relations stored in MuninFoo.labels for the current session
								for (MuninPlugin plugin : server.getPlugins())
										muninFoo.removeLabelRelation(plugin);
								
								if (muninFoo.getCurrentServer().equalsApprox(server))
									muninFoo.updateCurrentServer(context);
								
								Intent intent = getIntent();
								if (m != null && intent != null) {
									intent.putExtra("fromMaster", m.getId());
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								}
								refreshList();
							}
						})
						.setNegativeButton(R.string.text34, null)
						.show();
						break;
				}
			}
		});
		builderSingle.setTitle(server.getName());
		builderSingle.show();
		
		return true;
	}
	
	/**
	 * Called when a click event is triggered on the overflow icon on each
	 * parent-level list item
	 * Called from @see com.chteuchteu.munin.Adapter_ExpandableListView#getGroupView(int, boolean, View, android.view.ViewGroup)
	 * @param position
	 */
	public void onGroupItemOptionsClick(final int position) {
		// Display actions list
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				context, android.R.layout.simple_list_item_1);
		arrayAdapter.add(context.getString(R.string.rescan));
		arrayAdapter.add(context.getString(R.string.renameMaster));
		arrayAdapter.add(context.getString(R.string.editServersTitle));
		arrayAdapter.add(context.getString(R.string.update_credentials));
		arrayAdapter.add(context.getString(R.string.delete_master));
		
		builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
			@SuppressLint("InflateParams")
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final MuninMaster master = muninFoo.masters.get(position);
				
				switch (which) {
					case 0: // Rescan
						new MasterScanner(master, context).execute();
						break;
					case 1: // Rename master
						final EditText input = new EditText(context);
						input.setText(master.getName());
						
						new AlertDialog.Builder(context)
						.setTitle(R.string.renameMaster)
						.setView(input)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String value = input.getText().toString();
								if (!value.equals(master.getName())) {
									master.setName(value);
									MuninFoo.getInstance(context).sqlite.dbHlpr.updateMuninMaster(master);
									refreshList();
								}
								dialog.dismiss();
							}
						}).setNegativeButton(R.string.text64, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) { }
						}).show();
						break;
					case 2: // Reorganize / delete servers
						Intent i = new Intent(context, Activity_ServersEdit.class);
						i.putExtra("masterId", master.getId());
						context.startActivity(i);
						Util.setTransition(context, TransitionStyle.DEEPER);
						break;
					case 3: // Edit connection credentials
						displayCredentialsDialog(master);

						break;
					case 4: // Delete master
						new AlertDialog.Builder(context)
						.setTitle(R.string.delete)
						.setMessage(R.string.text84)
						.setPositiveButton(R.string.text33, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								new DeleteMaster(master, context).execute();
							}
						})
						.setNegativeButton(R.string.text34, null)
						.show();
						break;
				}
			}
		});
		builderSingle.show();
	}

    public void onGroupItemCredentialsClick(int parentPosition) {
        displayCredentialsDialog(muninFoo.getMasters().get(parentPosition));
    }

    private void displayCredentialsDialog(final MuninMaster master) {
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View dialog_updatecredentials = vi.inflate(R.layout.dialog_updatecredentials, null);

        final Spinner sp_authType = (Spinner) dialog_updatecredentials.findViewById(R.id.spinner_auth_type);
        final CheckBox cb_auth = (CheckBox) dialog_updatecredentials.findViewById(R.id.checkbox_http_auth);
        final EditText tb_authLogin = (EditText) dialog_updatecredentials.findViewById(R.id.auth_login);
        final EditText tb_authPassword = (EditText) dialog_updatecredentials.findViewById(R.id.auth_password);
        final View ll_auth = dialog_updatecredentials.findViewById(R.id.authIds);

        List<String> list = new ArrayList<String>();
        list.add("Basic");
        list.add("Digest");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_authType.setAdapter(dataAdapter);

        if (master.isAuthNeeded()) {
            cb_auth.setChecked(true);

            tb_authLogin.setText(master.getAuthLogin());
            tb_authPassword.setText(master.getAuthPassword());
            if (master.getAuthType() == AuthType.BASIC)
                sp_authType.setSelection(0);
            else if (master.getAuthType() == AuthType.DIGEST)
                sp_authType.setSelection(1);
        } else
            ll_auth.setVisibility(View.GONE);

        cb_auth.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    ll_auth.setVisibility(View.VISIBLE);
                else
                    ll_auth.setVisibility(View.GONE);
            }
        });


        new AlertDialog.Builder(context)
                .setTitle(R.string.update_credentials)
                .setView(dialog_updatecredentials)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (cb_auth.isChecked()) {
                            AuthType authType;
                            int index = sp_authType.getSelectedItemPosition();
                            if (index == 0)
                                authType = AuthType.BASIC;
                            else
                                authType = AuthType.DIGEST;

                            if (authType == AuthType.DIGEST == !muninFoo.premium) {
                                Toast.makeText(context, context.getString(R.string.text65), Toast.LENGTH_SHORT).show();
                            } else {
                                master.setAuthIds(tb_authLogin.getText().toString(),
                                        tb_authPassword.getText().toString(), authType);
                            }
                        } else
                            master.setAuthIds("", "", AuthType.NONE);

                        MuninFoo.getInstance(context).sqlite.dbHlpr.updateMuninMaster(master);
                    }
                }).setNegativeButton(R.string.text64, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { }
        }).show();
    }

	private void displayImportDialog() {
		final View dialogView = View.inflate(this, R.layout.dialog_import, null);
		new AlertDialog.Builder(this)
		.setTitle(R.string.import_title)
		.setView(dialogView)
		.setCancelable(true)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String code = ((EditText) dialogView.findViewById(R.id.import_code)).getText().toString();
				code = code.toLowerCase();
				new ImportRequestMaker(code, context).execute();
				dialog.dismiss();
			}
		})
		.setNegativeButton(R.string.text64, null)
		.show();
	}
	
	public static void onExportSuccess(String pswd) {
		final View dialogView = View.inflate(context, R.layout.dialog_export_success, null);
		TextView code = (TextView) dialogView.findViewById(R.id.export_succes_code);
		Util.Fonts.setFont(context, code, CustomFont.RobotoCondensed_Bold);
		code.setText(pswd);
		
		new AlertDialog.Builder(context)
		.setTitle(R.string.export_success_title)
		.setView(dialogView)
		.setCancelable(true)
		.setPositiveButton("OK", null)
		.show();
	}
	
	public static void onExportError() {
		Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
	}
	
	public static void onImportSuccess() {
		new AlertDialog.Builder(context)
		.setTitle(R.string.import_success_title)
		.setMessage(R.string.import_success_txt1)
		.setCancelable(true)
		.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				context.startActivity(new Intent(context, Activity_Servers.class));
			}
		})
		.show();
	}
	
	public static void onImportError() {
		Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
	}
	
	private void displayExportDialog() {
		new AlertDialog.Builder(context)
		.setTitle(R.string.export_servers)
		.setMessage(R.string.export_explanation)
		.setCancelable(true)
		.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String json = JSONHelper.getMastersJSONString(MuninFoo.getInstance(context).getMasters(), ImportExportHelper.ENCRYPTION_SEED);
				if (json.equals(""))
					Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show();
				else
					new ExportRequestMaker(json, context).execute();
			}
		})
		.setNegativeButton(R.string.text64, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.show();
	}
	
	private class MasterScanner extends AsyncTask<Void, Integer, Void> {
		private ProgressDialog dialog;
		private Context context;
		private MuninMaster original;
		private String report;
		
		private MasterScanner(MuninMaster master, Context context) {
			this.original = master;
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			dialog = ProgressDialog.show(context, "", getString(R.string.loading), true);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			report = original.rescan(context, muninFoo);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			
			new AlertDialog.Builder(activity)
			.setTitle(R.string.sync_reporttitle)
			.setMessage(report)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// if "No change" => don't reload servers list
					if (!report.equals(context.getString(R.string.sync_nochange)))
						refreshList();
				}
			})
			.show();
		}
	}
	
	private class DeleteMaster extends AsyncTask<Void, Integer, Void> {
		private ProgressDialog dialog;
		private Context context;
		private MuninMaster toBeDeleted;
		
		private DeleteMaster(MuninMaster master, Context context) {
			this.toBeDeleted = master;
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			dialog = ProgressDialog.show(context, "", getString(R.string.loading), true);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			muninFoo.deleteMuninMaster(toBeDeleted);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			dialog.dismiss();
			
			refreshList();
		}
	}

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.servers, menu);
		MenuItem importExportMenuItem = menu.findItem(R.id.menu_importexport);
		MenuItem exportMenuItem = menu.findItem(R.id.menu_export);
		if (!MuninFoo.isPremium(context))
			importExportMenuItem.setVisible(false);
		if (muninFoo.getServers().isEmpty())
			exportMenuItem.setVisible(false);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		Intent intent;
		switch (item.getItemId()) {
			case R.id.menu_add:
				intent = new Intent(this, Activity_Server.class);
				intent.putExtra("contextServerUrl", "");
				startActivity(intent);
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_import:
				displayImportDialog();
				return true;
			case R.id.menu_export:
				displayExportDialog();
				return true;
		}

		return true;
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
	}
}