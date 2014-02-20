package com.chteuchteu.munin.obj;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chteuchteu.munin.IconListAdapter;
import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.ui.Activity_Grid;

public class GridItem {
	public long			id;
	public int 			X;
	public int 			Y;
	public MuninPlugin 	plugin;
	public Period 		period;
	public ImageView 	iv;
	//public String 		name;
	public Grid 		grid;
	private Context 	c;
	public boolean 		editing = false;
	public LinearLayout outerContainer;
	public RelativeLayout container;
	public Bitmap 		graph;
	public ProgressBar 	pb;
	public boolean		isPersistant;
	
	public static int 		ICONS_MAX_WIDTH = 220;
	public static float	ALPHA_EDITING = 0.2f;
	
	public GridItem(Grid g, MuninPlugin p, Context c) {
		this.X = 0;
		this.Y = 0;
		this.plugin = p;
		//if (p != null)
			//this.name = p.getFancyName() + " : " + p.getInstalledOn().getName();
		this.period = Period.DAY;
		this.grid = g;
		this.c = c;
		this.isPersistant = false;
	}
	
	public LinearLayout getView(final Context c) {
		outerContainer = new LinearLayout(c);
		outerContainer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, grid.getGridItemHeight(c, grid.nbColumns), 1.0f));
		container = new RelativeLayout(c);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.setMargins(5, 5, 5, 5);
		iv = new ImageView(c);
		iv.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		pb = new ProgressBar(c);
		pb.setIndeterminate(true);
		pb.setVisibility(View.GONE);
		RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp3.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		pb.setLayoutParams(lp3);
		container.addView(iv);
		container.addView(pb);
		
		container.setLayoutParams(lp);
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!editing && Activity_Grid.editing) {
					grid.cancelAlpha(c);
					edit(c);
				}
				else if (!editing && !Activity_Grid.editing)
					preview(c);
			}
		});
		outerContainer.addView(container);
		return outerContainer;
	}
	
	public void preview(final Context c) {
		if (graph != null) {
			if (Activity_Grid.menu_open != null) 	Activity_Grid.menu_open.setVisible(true);
			if (Activity_Grid.menu_period != null)	Activity_Grid.menu_period.setVisible(false);
			if (Activity_Grid.menu_refresh != null)	Activity_Grid.menu_refresh.setVisible(false);
			if (Activity_Grid.menu_edit != null)	Activity_Grid.menu_edit.setVisible(false);
			
			grid.currentlyOpenedPlugin = plugin;
			((ImageView) ((Activity) c).findViewById(R.id.fullscreen_iv)).setImageBitmap(graph);
			((TextView) ((Activity) c).findViewById(R.id.fullscreen_tv)).setText(plugin.getInstalledOn().getName());
			View fs = ((Activity) c).findViewById(R.id.fullscreen);
			fs.setVisibility(View.VISIBLE);
			AlphaAnimation a = new AlphaAnimation(0.0f, 1.0f);
			a.setDuration(300);
			fs.startAnimation(a);
		}
	}
	
	public static LinearLayout getEmptyView(final Grid g, final Context c, final MuninFoo f, final int X, final int Y) {
		LinearLayout outerContainer = new LinearLayout(c);
		outerContainer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, g.getGridItemHeight(c, g.nbColumns), 1.0f));
		RelativeLayout ll = new RelativeLayout(c);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.setMargins(5, 5, 5, 5);
		ll.setLayoutParams(lp);
		ll.setBackgroundResource(R.drawable.grid_emptyitembg);
		ll.setClickable(true);
		ll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				add(c, f, g, X, Y);
			}
		});
		ImageView addButton = new ImageView(c);
		addButton.setImageResource(R.drawable.content_new_dark);
		RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp2.addRule(RelativeLayout.CENTER_IN_PARENT);
		addButton.setLayoutParams(lp2);
		ll.addView(addButton);
		outerContainer.addView(ll);
		if (!Activity_Grid.editing)
			outerContainer.setVisibility(View.INVISIBLE);
		return outerContainer;
	}
	
	private static void add(Context c, MuninFoo f, Grid g, int X, int Y) {
		add_serversListDialog(c, f, g, X, Y);
	}
	
	private static void add_serversListDialog(final Context c, final MuninFoo f, final Grid g, final int X, final int Y) {
		AlertDialog.Builder builder = new AlertDialog.Builder(c);
		builder.setTitle(c.getText(R.string.text71));
		ListView modeList = new ListView(c);
		String[] stringArray = new String[f.getHowManyServers()];
		for (int i=0; i<f.getServers().size(); i++)
			stringArray[i] = f.getServerFromFlatPosition(i).getName();
		
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(c, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
		modeList.setAdapter(modeAdapter);
		builder.setView(modeList);
		final Dialog dialog = builder.create();
		modeList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				dialog.dismiss();
				add_pluginsListDialog(c, pos, f, g, X, Y);
			}
		});
		dialog.show();
	}
	
	private static void add_pluginsListDialog(final Context c, int pos, final MuninFoo f, final Grid g, final int X, final int Y) {
		final MuninServer s = f.getServerFromFlatPosition(pos);
		List<MuninPlugin> l = s.getPlugins();
		
		final CharSequence[] items = new CharSequence[l.size()];
		for (int i=0; i<l.size(); i++)
			items[i] = l.get(i).getFancyName();
		
		final List<Integer> selectedItems = new ArrayList<Integer>();
		
		AlertDialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(c);
		builder.setTitle(c.getText(R.string.text72));
		builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
				if (isChecked)
					selectedItems.add(indexSelected);
				else if (selectedItems.contains(indexSelected))
					selectedItems.remove(Integer.valueOf(indexSelected));
			}
		})
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				if (g.nbColumns < 3 && selectedItems.size() > 3)
					while (g.nbColumns < 3)
						g.addColumn(c, true);
				
				int maxWidth = g.nbColumns;
				for (Integer i : selectedItems) {
					MuninPlugin p = s.getPlugin(i);
					if (!alreadyAdded(g, p)) {
						GridItem item = new GridItem(g, p, c);
						int[] pos = g.getNextAvailable(X, Y, maxWidth, c);
						item.X = pos[0];
						item.Y = pos[1];
						g.add(item, c, f, true);
						g.swapViews(g.getViewAt(item.X, item.Y), item.getView(c));
					}
				}
				
				if (selectedItems.size() > 0) {
					f.sqlite.saveGridItemRelations(g);
					g.dHelper.start(false);
				}
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});
		dialog = builder.create();
		dialog.show();
	}
	
	private static boolean alreadyAdded(Grid g, MuninPlugin p) {
		for (GridItem item : g.items) {
			if (item.plugin.equals(p))
				return true;
		}
		return false;
	}
	
	@SuppressLint("NewApi")
	private void edit(final Context c) {
		if (container.getWidth() > ICONS_MAX_WIDTH) {
			editing = true;
			putActionButtons();
			if (iv != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				iv.setAlpha(ALPHA_EDITING);
		} else {
			final List<String> items_l = new ArrayList<String>();
			List<Integer> icons_l = new ArrayList<Integer>();
			
			if (Y != 0) {
				items_l.add(c.getString(R.string.move_up));
				icons_l.add(R.drawable.ic_action_collapse);
			}
			if (X != 0) {
				items_l.add(c.getString(R.string.move_left));
				icons_l.add(R.drawable.ic_action_previous_item);
			}
			if (Y != grid.nbLines-1) {
				items_l.add(c.getString(R.string.move_down));
				icons_l.add(R.drawable.ic_action_expand);
			}
			if (X != grid.nbColumns-1) {
				items_l.add(c.getString(R.string.move_right));
				icons_l.add(R.drawable.ic_action_next_item);
			}
			items_l.add(c.getString(R.string.delete));
			icons_l.add(R.drawable.ic_action_remove);
			
			
			final String[] items = items_l.toArray(new String[items_l.size()]);
			final Integer[] icons = icons_l.toArray(new Integer[icons_l.size()]);
			ListAdapter adapter = new IconListAdapter(c, items, icons);
			
			new AlertDialog.Builder(c)
			.setAdapter(adapter, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int pos) {
					String selectedPos = items_l.get(pos);
					
					if (selectedPos.equals(c.getString(R.string.move_up)))			grid.move(X, Y, X, Y-1); // up
					else if (selectedPos.equals(c.getString(R.string.move_left)))	grid.move(X, Y, X-1, Y); // left
					else if (selectedPos.equals(c.getString(R.string.move_down)))	grid.move(X, Y, X, Y+1); // down
					else if (selectedPos.equals(c.getString(R.string.move_right)))	grid.move(X, Y, X+1, Y); // right
					else if (selectedPos.equals(c.getString(R.string.delete)))		remove(); // delete
				}
			}).show();
		}
	}
	
	@SuppressLint("NewApi")
	public void cancelEdit() {
		if (editing) {
			if (iv != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				iv.setAlpha(1f);
			removeActionButtons();
		}
	}
	
	@SuppressLint("NewApi")
	private void removeActionButtons() {
		editing = false;
		if (iv != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			iv.setAlpha(1f);
		List<View> toBeRemoved = new ArrayList<View>();
		for (int i=0; i<container.getChildCount(); i++) {
			if (container.getChildAt(i).getTag() != null && ((String) container.getChildAt(i).getTag()).equals("action"))
				toBeRemoved.add(container.getChildAt(i));
		}
		
		for (View v : toBeRemoved)
			container.removeView(v);
	}
	
	@SuppressLint("NewApi")
	private void putActionButtons() {
		for (GridItem i : grid.items) {
			if (i.editing)
				i.removeActionButtons();
		}
		editing = true;
		
		if (iv != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			iv.setAlpha(ALPHA_EDITING);
		
		if (Y != 0)
			container.addView(getActionButton("up", c));
		if (X != 0)
			container.addView(getActionButton("left", c));
		if (Y != grid.nbLines-1)
			container.addView(getActionButton("down", c));
		if (X != grid.nbColumns-1)
			container.addView(getActionButton("right", c));
		container.addView(getActionButton("delete", c));
	}
	
	private void remove() {
		grid.f.sqlite.dbHlpr.deleteGridItemRelation(this);
		grid.remove(X, Y);
		grid.swapViews(grid.getViewAt(X, Y), getEmptyView(grid, c, grid.f, X, Y));
	}
	
	public void updateActionButtonsAfterAddingColumn() {
		removeActionButtons();
		int deviceWidth = Util.getDeviceSize(c)[1];
		int diff = deviceWidth / (grid.nbColumns-1) - deviceWidth / (grid.nbColumns);
		int newContainerWidth = container.getWidth() - diff;
		if (newContainerWidth > ICONS_MAX_WIDTH)
			putActionButtons();
	}
	
	public void updateActionButtons() {
		removeActionButtons();
		if (container.getWidth() > ICONS_MAX_WIDTH)
			putActionButtons();
	}
	
	private LinearLayout getActionButton(String button, Context c) {
		LinearLayout ac = new LinearLayout(c);
		ac.setTag("action");
		RelativeLayout.LayoutParams lp = null;
		ImageView b = new ImageView(c);
		if (button.equals("up")) {
			lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			ac.setGravity(Gravity.CENTER_HORIZONTAL);
			b.setImageResource(R.drawable.ic_action_collapse);
			b.setContentDescription(c.getString(R.string.move_up));
			b.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
				grid.move(X, Y, X, Y-1);
			} });
		} else if (button.equals("left")) {
			lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			ac.setGravity(Gravity.CENTER_VERTICAL);
			b.setImageResource(R.drawable.ic_action_previous_item);
			b.setContentDescription(c.getString(R.string.move_left));
			b.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
				grid.move(X, Y, X-1, Y);
			} });
		} else if (button.equals("down")) {
			lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			ac.setGravity(Gravity.CENTER_HORIZONTAL);
			b.setImageResource(R.drawable.ic_action_expand);
			b.setContentDescription(c.getString(R.string.move_down));
			b.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
				grid.move(X, Y, X, Y+1);
			} });
		} else if (button.equals("right")) {
			lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			ac.setGravity(Gravity.CENTER_VERTICAL);
			b.setImageResource(R.drawable.ic_action_next_item);
			b.setContentDescription(c.getString(R.string.move_right));
			b.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
				grid.move(X, Y, X+1, Y);
			} });
		} else if (button.equals("delete")) {
			lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
			b.setImageResource(R.drawable.ic_action_remove);
			b.setContentDescription(c.getString(R.string.delete));
			b.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
				remove();
			} });
		}
		ac.setLayoutParams(lp);
		ac.setPadding(0, 0, 0, 0);
		ac.addView(b);
		return ac;
	}
}