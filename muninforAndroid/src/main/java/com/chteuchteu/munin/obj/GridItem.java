package com.chteuchteu.munin.obj;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.adptr.Adapter_IconList;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.ui.Fragment_Grid;
import com.chteuchteu.munin.ui.IActivity_Grid;

import java.util.ArrayList;
import java.util.List;

public class GridItem {
	private long			id;
	private int 			X;
	private int 			Y;
	private MuninPlugin 	plugin;
	private Grid 			grid;

	private Context 		context;
	private IActivity_Grid  activity;
	private Fragment_Grid   fragment;
	public ImageView 		iv;
	public ProgressBar 		pb;
	public View             footer;
	public RelativeLayout   container;

	public boolean 		    editing = false;
	public Bitmap 			graph;
	public HDGraphDownloader hdGraphDownloader;

	// Action buttons
	private View action_up, action_left, action_down,
			action_right, action_delete;

	private static int 	ICONS_MAX_WIDTH = 220;
	private static float	ALPHA_EDITING = 0.2f;
	
	public GridItem(Grid grid, MuninPlugin plugin) {
		this.X = 0;
		this.Y = 0;
		this.plugin = plugin;
		this.grid = grid;
		this.hdGraphDownloader = null;
	}

	public void setActivityReferences(Context context, IActivity_Grid activity, Fragment_Grid fragment) {
		this.context = context;
		this.activity = activity;
		this.fragment = fragment;
	}
	
	public LinearLayout getView(ViewGroup parent) {
		View view = LayoutInflater.from(context).inflate(R.layout.griditem, parent, false);

		LinearLayout outerContainer = (LinearLayout) view.findViewById(R.id.outerContainer);
		outerContainer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, grid.getGridItemHeight(context), 1.0f));

		container = (RelativeLayout) view.findViewById(R.id.container);
		iv = (ImageView) view.findViewById(R.id.iv);
		pb = (ProgressBar) view.findViewById(R.id.pb);

		// Footer
		footer = view.findViewById(R.id.gridItemFooter);
		TextView pluginName = (TextView) view.findViewById(R.id.pluginName);
		TextView serverName = (TextView) view.findViewById(R.id.serverName);
		Util.Fonts.setFont(context, pluginName, Util.Fonts.CustomFont.Roboto_Regular);
		Util.Fonts.setFont(context, serverName, Util.Fonts.CustomFont.Roboto_Regular);
		pluginName.setText(plugin.getFancyName());
		serverName.setText(plugin.getInstalledOn().getName());

		switch (Util.getPref(context, Util.PrefKeys.GridsLegend)) {
			case "none": footer.setVisibility(View.GONE); break;
			case "serverName": case "": pluginName.setVisibility(View.GONE); break;
			case "pluginName": serverName.setVisibility(View.GONE); break;
		}

		// Preview
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!editing && fragment.isEditing()) {
					grid.cancelAlpha();
					edit(context);
				}
				else if (!editing && !fragment.isEditing())
					preview();
			}
		});

		// Action buttons
		action_up = view.findViewById(R.id.iv_up);
		action_left = view.findViewById(R.id.iv_left);
		action_down = view.findViewById(R.id.iv_down);
		action_right = view.findViewById(R.id.iv_right);
		action_delete = view.findViewById(R.id.iv_remove);

		action_up.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				grid.move(X, Y, X, Y-1);
			}
		});
		action_left.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				grid.move(X, Y, X-1, Y);
			}
		});
		action_down.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				grid.move(X, Y, X, Y+1);
			}
		});
		action_right.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				grid.move(X, Y, X+1, Y);
			}
		});
		action_delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				remove();
			}
		});

		return outerContainer;
	}
	
	private void preview() {
		fragment.preview(this);
	}
	
	public static class HDGraphDownloader extends AsyncTask<Void, Integer, Void> {
		private MuninPlugin plugin;
		private ImageView imageView;
		private Period period;
		private Bitmap bitmap;
		private Context context;
		private boolean downloadKilled;
		private boolean isDownloading;
		
		public HDGraphDownloader (Context context, MuninPlugin plugin, ImageView imageView, Period period) {
			super();
			this.plugin = plugin;
			this.imageView = imageView;
			this.bitmap = null;
			this.downloadKilled = false;
			this.isDownloading = false;
			this.period = period;
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			this.isDownloading = true;
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			int[] dim = Util.HDGraphs.getBestImageDimensions(imageView, context);
			String graphUrl = plugin.getHDImgUrl(period, true, dim[0], dim[1]);
			bitmap = Util.dropShadow(
					Util.removeBitmapBorder(plugin.getInstalledOn().getParent().grabBitmap(
							graphUrl, MuninFoo.getInstance().getUserAgent()).getBitmap()));
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			isDownloading = false;
			
			if (!downloadKilled)
				imageView.setImageBitmap(bitmap);
		}
		
		public void killDownload() {
			downloadKilled = true;
			isDownloading = false;
		}
		public boolean isDownloading() { return this.isDownloading; }
	}
	
	public static LinearLayout getEmptyView(final Grid grid, final Context context, final MuninFoo muninFoo, final IActivity_Grid activity, final Fragment_Grid fragment,
	                                        final int X, final int Y, ViewGroup parent) {
		View view = LayoutInflater.from(context).inflate(R.layout.griditem_empty, parent, false);

		LinearLayout outerContainer = (LinearLayout) view.findViewById(R.id.outerContainer);
		outerContainer.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, grid.getGridItemHeight(context), 1.0f));

		RelativeLayout ll = (RelativeLayout) view.findViewById(R.id.ll);
		ll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				add(context, muninFoo, grid, activity, fragment, X, Y);
			}
		});

		if (!fragment.isEditing())
			outerContainer.setVisibility(View.INVISIBLE);

		return outerContainer;
	}
	
	private static void add(Context c, MuninFoo f, Grid g, IActivity_Grid activity, Fragment_Grid fragment, int X, int Y) {
		add_serversListDialog(c, f, g, activity, fragment, X, Y);
	}
	
	@SuppressWarnings("deprecation")
	private static void add_serversListDialog(final Context c, final MuninFoo f, final Grid g, final IActivity_Grid activity, final Fragment_Grid fragment,
	                                          final int X, final int Y) {
		AlertDialog.Builder builder = new AlertDialog.Builder(c);
		builder.setTitle(c.getText(R.string.text71));
		ListView modeList = new ListView(c);
		String[] stringArray = new String[f.getServers().size()];
		for (int i=0; i<f.getServers().size(); i++)
			stringArray[i] = f.getServerFromFlatPosition(i).getName();
		
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(c, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
		modeList.setAdapter(modeAdapter);
		builder.setView(modeList);
		final Dialog dialog = builder.create();
		modeList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				dialog.dismiss();
				add_pluginsListDialog(c, pos, f, g, activity, fragment, X, Y);
			}
		});
		dialog.show();
	}
	
	private static void add_pluginsListDialog(final Context c, int pos, final MuninFoo f, final Grid g, final IActivity_Grid activity, final Fragment_Grid fragment,
	                                          final int X, final int Y) {
		@SuppressWarnings("deprecation")
		final MuninServer s = f.getServerFromFlatPosition(pos);
		List<MuninPlugin> l = s.getPlugins();
		
		final CharSequence[] items = new CharSequence[l.size()];
		for (int i=0; i<l.size(); i++)
			items[i] = l.get(i).getFancyName();
		
		final List<Integer> selectedItems = new ArrayList<>();
		
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
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				if (g.getNbColumns() < 3 && selectedItems.size() > 3)
					while (g.getNbColumns() < 3)
						g.addColumn(c, true);
				
				int maxWidth = g.getNbColumns();
				for (Integer i : selectedItems) {
					MuninPlugin p = s.getPlugin(i);
					if (!alreadyAdded(g, p)) {
						GridItem item = new GridItem(g, p);
						item.setActivityReferences(c, activity, fragment);
						int[] pos = g.getNextAvailable(X, Y, maxWidth, c);
						item.X = pos[0];
						item.Y = pos[1];
						g.add(item, c, f, true);
						g.swapViews(g.getViewAt(item.X, item.Y), item.getView(null));
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
		for (GridItem item : g.getItems()) {
			if (item != null && item.plugin != null && item.plugin.equals(p)) // hotfix
				return true;
		}
		return false;
	}
	
	private void edit(final Context c) {
		if (container.getWidth() > ICONS_MAX_WIDTH) {
			// Enough space to display buttons on gridItem
			editing = true;
			showActionButtons();
			if (iv != null)
				iv.setAlpha(ALPHA_EDITING);
		} else {
			// Not enough space : show actions list in dialog
			final List<String> items_l = new ArrayList<>();
			List<Integer> icons_l = new ArrayList<>();
			
			if (Y != 0) {
				items_l.add(c.getString(R.string.move_up));
				icons_l.add(R.drawable.ic_action_up);
			}
			if (X != 0) {
				items_l.add(c.getString(R.string.move_left));
				icons_l.add(R.drawable.ic_action_previous_item);
			}
			if (Y != grid.getNbLines()-1) {
				items_l.add(c.getString(R.string.move_down));
				icons_l.add(R.drawable.ic_action_down);
			}
			if (X != grid.getNbColumns()-1) {
				items_l.add(c.getString(R.string.move_right));
				icons_l.add(R.drawable.ic_action_next_item);
			}
			items_l.add(c.getString(R.string.delete));
			icons_l.add(R.drawable.ic_action_remove);
			
			
			final String[] items = items_l.toArray(new String[items_l.size()]);
			final Integer[] icons = icons_l.toArray(new Integer[icons_l.size()]);
			ListAdapter adapter = new Adapter_IconList(c, items, icons);
			
			new AlertDialog.Builder(c)
					.setAdapter(adapter, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int pos) {
							String selectedPos = items_l.get(pos);

							if (selectedPos.equals(c.getString(R.string.move_up))) grid.move(X, Y, X, Y - 1); // up
							else if (selectedPos.equals(c.getString(R.string.move_left))) grid.move(X, Y, X - 1, Y); // left
							else if (selectedPos.equals(c.getString(R.string.move_down))) grid.move(X, Y, X, Y + 1); // down
							else if (selectedPos.equals(c.getString(R.string.move_right))) grid.move(X, Y, X + 1, Y); // right
							else if (selectedPos.equals(c.getString(R.string.delete))) remove(); // delete
						}
					}).show();
		}
	}
	
	public void cancelEdit() {
		if (editing) {
			if (iv != null)
				iv.setAlpha(1f);
			hideActionButtons();
		}
	}

	private void showActionButtons() {
		for (GridItem i : grid.getItems()) {
			if (i.editing)
				i.hideActionButtons();
		}
		editing = true;
		
		if (iv != null)
			iv.setAlpha(ALPHA_EDITING);

		if (Y != 0)
			action_up.setVisibility(View.VISIBLE);
		if (X != 0)
			action_left.setVisibility(View.VISIBLE);
		if (Y != grid.getNbLines()-1)
			action_down.setVisibility(View.VISIBLE);
		if (X != grid.getNbColumns()-1)
			action_right.setVisibility(View.VISIBLE);
		action_delete.setVisibility(View.VISIBLE);
	}

	private void hideActionButtons() {
		editing = false;
		if (iv != null)
			iv.setAlpha(1f);

		action_up.setVisibility(View.GONE);
		action_left.setVisibility(View.GONE);
		action_down.setVisibility(View.GONE);
		action_right.setVisibility(View.GONE);
		action_delete.setVisibility(View.GONE);
	}
	
	private void remove() {
		grid.f.sqlite.dbHlpr.deleteGridItemRelation(this);
		grid.remove(X, Y);
		grid.swapViews(grid.getViewAt(X, Y), getEmptyView(grid, context, grid.f, activity, fragment, X, Y, null));
	}
	
	public void updateActionButtonsAfterAddingColumn() {
		hideActionButtons();
		int deviceWidth = Util.getDeviceSize(context)[1];
		int diff = deviceWidth / (grid.getNbColumns()-1) - deviceWidth / (grid.getNbColumns());
		int newContainerWidth = container.getWidth() - diff;
		if (newContainerWidth > ICONS_MAX_WIDTH)
			showActionButtons();
	}
	
	public void updateActionButtons() {
		hideActionButtons();
		if (container.getWidth() > ICONS_MAX_WIDTH)
			showActionButtons();
	}

	public long getId() { return id; }
	public void setId(long id) { this.id = id; }

	public int getX() { return X; }
	public void setX(int x) { this.X = x; }

	public int getY() { return this.Y; }
	public void setY(int y) { this.Y = y; }

	public MuninPlugin getPlugin() { return plugin; }
	public void setPlugin(MuninPlugin plugin) { this.plugin = plugin; }

	public Grid getGrid() { return grid; }
	public void setGrid(Grid grid) { this.grid = grid; }
}
