package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.ImportExportHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Grid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Activity_Grids extends MuninActivity implements IImportExportActivity {
	private List<HashMap<String, String>> list;
	private ListView listview;
	private List<Grid> grids;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_grids);
		super.onContentViewSet();

		actionBar.setTitle(getString(R.string.button_grid));

		listview = (ListView) findViewById(R.id.listview);
		list = new ArrayList<>();

		updateList();

		// Init fab
		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				add();
			}
		});
	}

	private Grid getGridFromName(String gridName) {
		for (Grid grid : grids) {
			if (grid.getName().equals(gridName))
				return grid;
		}

		return null;
	}

	private void updateList() {
		list.clear();
		listview.setAdapter(null);

		// Load grids list
		grids = muninFoo.sqlite.dbHlpr.getGrids(muninFoo);

		if (grids.isEmpty()) {
            findViewById(R.id.grids_nogrid).setVisibility(View.VISIBLE);
            return;
        }


        for (Grid g : grids) {
            HashMap<String,String> item = new HashMap<>();
            item.put("line1", g.getName());
            item.put("line2", g.getFullWidth() + " x " + g.getFullHeight());
            list.add(item);
        }
        SimpleAdapter sa = new SimpleAdapter(this, list, R.layout.gridselection_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
        listview.setAdapter(sa);

        // Navigate to grid on click
        listview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
                TextView gridName = (TextView) view.findViewById(R.id.line_a);
                Intent intent = new Intent(Activity_Grids.this, Activity_Grid.class);
                intent.putExtra(Activity_Grid.ARG_GRIDID, getGridFromName(gridName.getText().toString()).getId());
                startActivity(intent);
                Util.setTransition(activity, TransitionStyle.DEEPER);
            }
        });

        // Display grid options on click
        listview.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapter, final View view, int position, long arg) {
                // Display actions list
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                        context, android.R.layout.simple_list_item_1);
                arrayAdapter.add(context.getString(R.string.rename_grid));
                arrayAdapter.add(context.getString(R.string.text73)); // Delete grid

                builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final TextView gridNameTextView = (TextView) view.findViewById(R.id.line_a);
                        final String gridName = gridNameTextView.getText().toString();

                        switch (which) {
                            case 0: // Rename grid
                                LayoutInflater layoutInflater = LayoutInflater.from(context);
                                ViewGroup alertDialogView = (ViewGroup) layoutInflater.inflate(R.layout.dialog_edittext, null, false);
                                final EditText input = (EditText) alertDialogView.findViewById(R.id.input);
                                input.setText(gridName);

                                new AlertDialog.Builder(context)
                                        .setTitle(R.string.rename_grid)
                                        .setView(alertDialogView)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                String value = input.getText().toString();
                                                if (!value.equals(gridName)) {
                                                    // Check if there's a grid with this name
                                                    boolean alreadyExists = muninFoo.sqlite.dbHlpr.gridExists(value);
                                                    if (!alreadyExists) {
                                                        MuninFoo.getInstance(context).sqlite.dbHlpr.updateGridName(gridName, value);
                                                        gridNameTextView.setText(value);
                                                    } else
                                                        Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
                                                }
                                                dialog.dismiss();
                                            }
                                        }).setNegativeButton(R.string.text64, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                }).show();
                                break;
                            case 1: // Delete grid
                                new AlertDialog.Builder(context)
                                        .setTitle(R.string.delete)
                                        .setMessage(R.string.text80)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Grid grid = getGridFromName(gridName);
                                                muninFoo.sqlite.dbHlpr.deleteGrid(grid);
                                                updateList();
                                            }
                                        })
                                        .setNegativeButton(R.string.no, null)
                                        .show();

                                break;
                        }
                    }
                });
                builderSingle.show();

                return true;
            }
        });
	}

	private void add() {
		LayoutInflater inflater = LayoutInflater.from(this);
		View dialogLayout = inflater.inflate(R.layout.dialog_edittext, null, false);
		final EditText input = (EditText) dialogLayout.findViewById(R.id.input);

		AlertDialog.Builder b = new AlertDialog.Builder(Activity_Grids.this)
				.setTitle(getText(R.string.text69))
				.setView(dialogLayout)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						if (!value.equals("")) {
							List<String> existingNames = muninFoo.sqlite.dbHlpr.getGridsNames();
							boolean available = true;
							for (String s : existingNames) {
								if (s.equals(value))
									available = false;
							}
							if (available) {
								long id = muninFoo.sqlite.dbHlpr.insertGrid(value);
								dialog.dismiss();
								Intent i = new Intent(Activity_Grids.this, Activity_Grid.class);
								i.putExtra("gridId", id);
								startActivity(i);
								Util.setTransition(activity, TransitionStyle.DEEPER);
							}
							else
								Toast.makeText(context, getString(R.string.text74), Toast.LENGTH_LONG).show();
						}
					}
				})
				.setNegativeButton(getText(R.string.text64), null);
		AlertDialog d = b.create();
		d.show();
	}


	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Grids; }

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.grids, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_import:
				ImportExportHelper.showImportDialog(muninFoo, context, ImportExportHelper.ImportExportType.GRIDS, this);
				return true;
			case R.id.menu_export:
				ImportExportHelper.showExportDialog(muninFoo, context, ImportExportHelper.ImportExportType.GRIDS, this);
		}

		return true;
	}

	@Override
	public void onExportSuccess(String pswd) {
		final View dialogView = View.inflate(context, R.layout.dialog_export_success, null);
		TextView code = (TextView) dialogView.findViewById(R.id.export_succes_code);
		Util.Fonts.setFont(context, code, Util.Fonts.CustomFont.RobotoCondensed_Bold);
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
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						context.startActivity(new Intent(context, Activity_Grids.class));
					}
				})
				.show();
	}

	@Override
	public void onImportError() {
		Toast.makeText(context, R.string.text09, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(this, TransitionStyle.SHALLOWER);
	}
}
