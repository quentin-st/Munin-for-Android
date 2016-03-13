package com.chteuchteu.munin.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_ExpandableListView;
import com.chteuchteu.munin.async.MasterDeleter;
import com.chteuchteu.munin.async.MasterScanner;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.ImportExportHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninMaster.AuthType;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Activity_Servers extends MuninActivity implements IServersActivity, IImportExportActivity {
	private static Context context;
	private ExpandableListView expListView;
	
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;

		setContentView(R.layout.activity_servers);
		super.onContentViewSet();

		actionBar.setTitle(getString(R.string.serversTitle));
		
		expListView = (ExpandableListView) findViewById(R.id.servers_list);
		
		refreshList();

		// Init fab
		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(new Intent(activity, Activity_Server.class));
				Util.setTransition(activity, TransitionStyle.DEEPER);
			}
		});
	}
	
	public void refreshList() {
		findViewById(R.id.servers_noserver).setVisibility(View.GONE);
		
		Intent i = getIntent();
		MuninMaster fromServersEdit = null;
		if (i.getExtras() != null && i.getExtras().containsKey("fromMaster"))
			fromServersEdit = muninFoo.getMasterById((int) i.getExtras().getLong("fromMaster"));
		
		List<MuninMaster> masters = muninFoo.masters;
        Map<MuninMaster, List<String>> nodesCollection = getNodesCollection();
		final Adapter_ExpandableListView expListAdapter = new Adapter_ExpandableListView(this, this, masters, nodesCollection);
		expListView.setAdapter(expListAdapter);
		
		if (fromServersEdit != null)
			expListView.expandGroup(muninFoo.getMasterPosition(fromServersEdit));
		
		if (muninFoo.getNodes().isEmpty())
			findViewById(R.id.servers_noserver).setVisibility(View.VISIBLE);
	}
	
	private Map<MuninMaster, List<String>> getNodesCollection() {
		// Create collection
		LinkedHashMap<MuninMaster, List<String>> nodesCollection = new LinkedHashMap<>();
		
		for (MuninMaster master : muninFoo.masters) {
			List<String> childList = new ArrayList<>();
			for (MuninNode node : master.getChildren())
				childList.add(node.getName());
			nodesCollection.put(master, childList);
		}
		
		return nodesCollection;
	}

	/* IServersActivity */
	/**
	 * Called when a click event is triggered on a child-level element of the listview
	 */
	@Override
	public void onChildClick() {
		Toast.makeText(context, R.string.long_click, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Called when a long click event is triggered on a child-level element of the listview
	 */
	@Override
	public boolean onChildLongClick(int groupPosition, int childPosition) {
		final MuninNode node = muninFoo.masters.get(groupPosition).getChildren().get(childPosition);
		
		// Display actions list
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
				context, android.R.layout.simple_list_item_1);
		arrayAdapter.add(context.getString(R.string.menu_addserver_delete));
		
		builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0: // Delete node
						new AlertDialog.Builder(context)
						.setTitle(R.string.delete)
						.setMessage(R.string.text83)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								muninFoo.sqlite.dbHlpr.deleteNode(node);
								muninFoo.deleteNode(node);
								
								// Delete labels relations stored in MuninFoo.labels for the current session
								for (MuninPlugin plugin : node.getPlugins())
									muninFoo.removeLabelRelation(plugin);
								
								if (muninFoo.getCurrentNode().equalsApprox(node))
									muninFoo.updateCurrentNode(context);

								refreshList();
								updateDrawerIfNeeded();
							}
						})
						.setNegativeButton(R.string.no, null)
						.show();
						break;
				}
			}
		});
		builderSingle.setTitle(node.getName());
		builderSingle.show();
		
		return true;
	}

	/**
	 * When deleting a node / master, we should reinit the drawer
	 * if there's nothing to show
	 */
	public void updateDrawerIfNeeded() {
		if (muninFoo.getMasters().size() == 0)
			drawerHelper.reset();
	}
	
	/**
	 * Called when a click event is triggered on the overflow icon on each parent-level list item
	 */
	@Override
	public void onParentOptionsClick(View overflowIcon, final int position) {
		// Display actions list
		PopupMenu popupMenu = new PopupMenu(context, overflowIcon);
		Menu menu = popupMenu.getMenu();
		popupMenu.getMenuInflater().inflate(R.menu.servers_overflow, menu);
		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				final MuninMaster master = muninFoo.masters.get(position);

				switch (menuItem.getItemId()) {
					case R.id.menu_rescan:
						new MasterScanner(Activity_Servers.this, master).execute();
						return true;
					case R.id.menu_renameMaster:
						LayoutInflater inflater = LayoutInflater.from(context);
						ViewGroup view = (ViewGroup) inflater.inflate(R.layout.dialog_edittext, null, false);
						final EditText input = (EditText) view.findViewById(R.id.input);
						input.setText(master.getName());

						new AlertDialog.Builder(context)
								.setTitle(R.string.renameMaster)
								.setView(view)
								.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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
									public void onClick(DialogInterface dialog, int whichButton) {
									}
								}).show();

						return true;
					case R.id.menu_updateCredentials:
						displayCredentialsDialog(master);
						return true;
					case R.id.menu_HdGraphs:
						displayHDGraphsDialog(master);
						return true;
					case R.id.menu_deleteMaster:
						new AlertDialog.Builder(context)
								.setTitle(R.string.delete)
								.setMessage(R.string.text84)
								.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										new MasterDeleter(master, Activity_Servers.this).execute();
									}
								})
								.setNegativeButton(R.string.no, null)
								.show();
						return true;
				}
				return false;
			}
		});

		popupMenu.show();
	}

	@Override
    public void onParentCredentialsClick(int parentPosition) {
        displayCredentialsDialog(muninFoo.getMasters().get(parentPosition));
    }

    private void displayCredentialsDialog(final MuninMaster master) {
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams")
        final View dialog_updatecredentials = vi.inflate(R.layout.dialog_updatecredentials, null);

        final Spinner sp_authType = (Spinner) dialog_updatecredentials.findViewById(R.id.spinner_auth_type);
        final CheckBox cb_auth = (CheckBox) dialog_updatecredentials.findViewById(R.id.checkbox_http_auth);
        final EditText tb_authLogin = (EditText) dialog_updatecredentials.findViewById(R.id.auth_login);
        final EditText tb_authPassword = (EditText) dialog_updatecredentials.findViewById(R.id.auth_password);
        final View ll_auth = dialog_updatecredentials.findViewById(R.id.authIds);

        List<String> list = new ArrayList<>();
        list.add("Basic");
        list.add("Digest");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, list);
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
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
		                if (cb_auth.isChecked()) {
			                AuthType authType = sp_authType.getSelectedItemPosition() == 0
					                ? AuthType.BASIC
					                : AuthType.DIGEST;

			                if (authType == AuthType.DIGEST && !muninFoo.premium) {
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
	        public void onClick(DialogInterface dialog, int whichButton) {
	        }
        }).show();
    }

	private void displayHDGraphsDialog(final MuninMaster master) {
		LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		@SuppressLint("InflateParams")
		View dialog_checkbox = vi.inflate(R.layout.dialog_checkbox, null);

		final CheckBox checkbox = (CheckBox) dialog_checkbox.findViewById(R.id.checkbox);
		checkbox.setChecked(master.isDynazoomAvailable() == MuninMaster.DynazoomAvailability.TRUE);
		checkbox.setText(R.string.settings_hdgraphs_text);

		new AlertDialog.Builder(context)
				.setTitle(R.string.settings_hdgraphs)
				.setView(dialog_checkbox)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if (checkbox.isChecked())
							master.setDynazoomAvailable(MuninMaster.DynazoomAvailability.TRUE);
						else
							master.setDynazoomAvailable(MuninMaster.DynazoomAvailability.FALSE);

						MuninFoo.getInstance(context).sqlite.dbHlpr.updateMuninMaster(master);
					}
				}).setNegativeButton(R.string.text64, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		}).show();
	}

	@Override
	public void onExportSuccess(String pswd) {
		final View dialogView = View.inflate(context, R.layout.dialog_export_success, null);
		TextView code = (TextView) dialogView.findViewById(R.id.export_succes_code);
		Util.Fonts.setFont(context, code, CustomFont.RobotoCondensed_Bold);
		code.setText(pswd);
		
		new AlertDialog.Builder(context)
			.setTitle(R.string.export_success_title)
			.setView(dialogView)
			.setCancelable(true)
			.setPositiveButton(R.string.ok, null)
			.show();
	}

	@Override
	public void onExportError() {
		Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onImportSuccess() {
		new AlertDialog.Builder(context)
			.setTitle(R.string.import_success_title)
			.setMessage(R.string.import_success)
			.setCancelable(true)
			.setPositiveButton(R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					context.startActivity(new Intent(context, Activity_Servers.class));
				}
			})
			.show();
	}

	@Override
	public void onImportError() {
		Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Servers; }

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.servers, menu);
		MenuItem exportMenuItem = menu.findItem(R.id.menu_export);
		if (muninFoo.getNodes().isEmpty())
			exportMenuItem.setVisible(false);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_import:
				ImportExportHelper.showImportDialog(muninFoo, context, ImportExportHelper.ImportExportType.MASTERS, this);
				return true;
			case R.id.menu_export:
				ImportExportHelper.showExportDialog(muninFoo, context, ImportExportHelper.ImportExportType.MASTERS, this);
				return true;
		}

		return true;
	}
	
	@Override
	public void onBackPressed() {
        if (drawerHelper.closeDrawerIfOpen())
            return;

        Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
	}
}
