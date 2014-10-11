package com.chteuchteu.munin.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Grid;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;


public class Activity_Grids extends ListActivity {
	private MuninFoo		muninFoo;
	private DrawerHelper	dh;
	private Context			context;
	private Menu			menu;
	private String			activityName;
	
	private SimpleAdapter	sa;
	private ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
	
	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		muninFoo = MuninFoo.getInstance(this);
		MuninFoo.loadLanguage(this);
		context = this;
		
		setContentView(R.layout.gridselection);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(getString(R.string.button_grid));
		
		dh = new DrawerHelper(this, muninFoo);
		dh.setDrawerActivity(dh.Activity_Grids);
		
		Util.UI.applySwag(this);
		
		updateList();
	}
	
	private void updateList() {
		list.clear();
		setListAdapter(null);
		
		List<Grid> gridsList = muninFoo.sqlite.dbHlpr.getGrids(this, muninFoo);
		
		if (gridsList.size() == 0)
			findViewById(R.id.grids_nogrid).setVisibility(View.VISIBLE);
		else {
			HashMap<String,String> item;
			for (Grid g : gridsList) {
				item = new HashMap<String,String>();
				item.put("line1", g.name);
				item.put("line2", g.getFullWidth() + " x " + g.getFullHeight());
				list.add(item);
			}
			sa = new SimpleAdapter(this, list, R.layout.gridselection_list, new String[] { "line1","line2" }, new int[] {R.id.line_a, R.id.line_b});
			setListAdapter(sa);
			
			getListView().setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
					TextView gridName = (TextView) view.findViewById(R.id.line_a);
					Intent intent = new Intent(Activity_Grids.this, Activity_Grid.class);
					intent.putExtra("gridName", gridName.getText().toString());
					startActivity(intent);
					Util.setTransition(context, TransitionStyle.DEEPER);
				}
			});
			
			getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> adapter, final View view, int position, long arg) {
					// Display actions list
					AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
					final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
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
								final EditText input = new EditText(context);
								input.setText(gridName);
								
								new AlertDialog.Builder(context)
								.setTitle(R.string.rename_grid)
								.setView(input)
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
									public void onClick(DialogInterface dialog, int whichButton) { }
								}).show();
								break;
							case 1: // Delete grid
								new AlertDialog.Builder(context)
								.setTitle(R.string.delete)
								.setMessage(R.string.text80)
								.setPositiveButton(R.string.text33, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Grid grid = muninFoo.sqlite.dbHlpr.getGrid(context, muninFoo, gridName);
										muninFoo.sqlite.dbHlpr.deleteGrid(grid);
										updateList();
									}
								})
								.setNegativeButton(R.string.text34, null)
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
	}
	
	private void add() {
		final LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(10, 30, 10, 10);
		final EditText input = new EditText(this);
		ll.addView(input);
		
		AlertDialog.Builder b = new AlertDialog.Builder(Activity_Grids.this)
		.setTitle(getText(R.string.text69))
		.setView(ll)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Overrided by the CustomListener class
			}
		})
		.setNegativeButton(getText(R.string.text64), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) { }
		});
		AlertDialog d = b.create();
		d.show();
		Button okButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
		okButton.setOnClickListener(new CustomListener(input, d));
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.menu = menu;
		
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
		
		createOptionsMenu();
		return true;
	}
	private void createOptionsMenu() {
		menu.clear();
		
		getMenuInflater().inflate(R.menu.gridselection, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != android.R.id.home && dh != null)
			dh.closeDrawerIfOpened();
		switch (item.getItemId()) {
			case android.R.id.home:
				dh.getDrawer().toggle(true);
				return true;
			case R.id.menu_add:
				add();
				return true;
			case R.id.menu_settings:
				startActivity(new Intent(Activity_Grids.this, Activity_Settings.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			case R.id.menu_about:
				startActivity(new Intent(Activity_Grids.this, Activity_About.class));
				Util.setTransition(context, TransitionStyle.DEEPER);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
	}
	
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(this, Activity_Main.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		Util.setTransition(context, TransitionStyle.SHALLOWER);
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
	
	class CustomListener implements View.OnClickListener {
		private final Dialog dialog;
		private final EditText input;
		public CustomListener(EditText input, Dialog dialog) {
			this.dialog = dialog;
			this.input = input;
		}
		
		@Override
		public void onClick(View v) {
			String value = input.getText().toString();
			if (!value.equals("")) {
				List<String> existingNames = muninFoo.sqlite.dbHlpr.getGridsNames();
				boolean available = true;
				for (String s : existingNames) {
					if (s.equals(value))
						available = false;
				}
				if (available) {
					muninFoo.sqlite.dbHlpr.insertGrid(new Grid(value, muninFoo));
					dialog.dismiss();
					Intent i = new Intent(Activity_Grids.this, Activity_Grid.class);
					i.putExtra("gridName", value);
					startActivity(i);
					Util.setTransition(context, TransitionStyle.DEEPER);
				}
				else
					Toast.makeText(context, getString(R.string.text74), Toast.LENGTH_LONG).show();
			}
		}
	}
}