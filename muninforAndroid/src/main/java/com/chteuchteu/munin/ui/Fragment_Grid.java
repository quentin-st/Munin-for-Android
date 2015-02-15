package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.GridDownloadHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;

public class Fragment_Grid extends Fragment {
	public static final String ARG_GRIDID = "gridId";
	public static final String ARG_AUTOLOAD = "autoLoad";
	public static final String ARG_OVERFLOW_ACTIONS = "overflowActions";

	private Context context;
	private IGridActivity activity;
	private boolean editing;
	private Grid grid;
	private LinearLayout container;
	private MuninPlugin.Period currentPeriod;
	private View view;

	private boolean updating;

	private MuninFoo muninFoo;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.context = activity;
		this.activity = (IGridActivity) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		muninFoo = MuninFoo.getInstance();
		currentPeriod = Util.getDefaultPeriod(context);
		activity.updatePeriodMenuItem(currentPeriod);

		// Load grid
		Bundle args = getArguments();
		long gridId = args.getLong(ARG_GRIDID);
		boolean autoLoad = args.getBoolean(ARG_AUTOLOAD, true);
		boolean overflowActions = args.getBoolean(ARG_OVERFLOW_ACTIONS, false);
		this.grid = muninFoo.sqlite.dbHlpr.getGrid(muninFoo, gridId);

		if (this.grid == null)
			return inflater.inflate(R.layout.empty_view, container, false);

		this.grid.setActivityReferences(context, muninFoo, activity, this);
		activity.onGridLoaded(grid);
		this.editing = false;
		this.updating = false;

		this.view = inflater.inflate(R.layout.fragment_grid, container, false);

		this.container = (LinearLayout) view.findViewById(R.id.grid_root_container);
		this.currentPeriod = Util.getDefaultPeriod(context);

		// Init grid
		container.removeAllViews();
		grid.setupLayout();
		this.container.addView(grid.buildLayout(context));
		grid.updateLayoutSizes(context);

		// Launch graphs downloader
		grid.dHelper = new GridDownloadHelper(grid, 3, currentPeriod, this);
		if (autoLoad)
			grid.dHelper.start(true);

		// Set edit onclick listeners
		view.findViewById(R.id.add_line_bottom).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				grid.addLine(context, true);
			}
		});
		view.findViewById(R.id.add_column_right).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				grid.addColumn(context, true);
			}
		});
		view.findViewById(R.id.fullscreen).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				hidePreview();
			}
		});

		Util.Fonts.setFont(context, (TextView) view.findViewById(R.id.fullscreen_tv), Util.Fonts.CustomFont.Roboto_Regular);

		if (grid.getItems().size() == 0)
			edit();

		if (!autoLoad) {
			view.findViewById(R.id.manual_load).setVisibility(View.VISIBLE);
			view.findViewById(R.id.manual_load_action).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					view.findViewById(R.id.manual_load).setVisibility(View.GONE);
					grid.dHelper.start(true);
				}
			});
		}

		if (overflowActions) {
			final ImageButton overflowButton = (ImageButton) view.findViewById(R.id.overflow);
			overflowButton.setVisibility(View.VISIBLE);
			overflowButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// Avoid menu opening when manual_hide is shown
					if (view.findViewById(R.id.manual_load).getVisibility() == View.VISIBLE)
						return;

					PopupMenu popup = new PopupMenu(context, overflowButton);
					final Menu menu = popup.getMenu();
					popup.getMenuInflater().inflate(R.menu.grid, menu);
					menu.findItem(R.id.menu_edit).setVisible(false);
					menu.findItem(R.id.menu_open).setVisible(false);

					popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						public boolean onMenuItemClick(MenuItem item) {
							switch (item.getItemId()) {
								case R.id.menu_refresh: refresh(); break;
								case R.id.menu_edit: edit(); break;
								case R.id.period_day:
									setCurrentPeriod(MuninPlugin.Period.DAY);
									refresh();
									return true;
								case R.id.period_week:
									setCurrentPeriod(MuninPlugin.Period.WEEK);
									refresh();
									return true;
								case R.id.period_month:
									setCurrentPeriod(MuninPlugin.Period.MONTH);
									refresh();
									return true;
								case R.id.period_year:
									setCurrentPeriod(MuninPlugin.Period.YEAR);
									refresh();
									return true;
							}
							return true;
						}
					});

					popup.show(); //showing popup menu
				}
			});
		}

		return view;
	}

	public void preview(final GridItem item) {
		if (item == null || item.originalGraph == null)
			return;

		grid.currentlyOpenedGridItem = item;

		activity.onPreview();

		final ImageView fullscreenImageView = (ImageView) view.findViewById(R.id.fullscreen_iv);
		fullscreenImageView.setImageBitmap(item.originalGraph);
		((TextView) view.findViewById(R.id.fullscreen_tv)).setText(item.getPlugin().getInstalledOn().getName());
		View fs = view.findViewById(R.id.fullscreen);

		// Lollipop animation (fallback if necessary)
		View mainContainer = view.findViewById(R.id.mainContainer);
		fs.setVisibility(View.VISIBLE);
		View parent = (View) item.container.getParent();
		View parentParent = (View) item.container.getParent().getParent();
		int cx = (parent.getLeft() + parent.getRight()) / 2;
		int cy = (parentParent.getTop() + parentParent.getBottom()) / 2;
		int finalRadius = Math.max(mainContainer.getWidth(), mainContainer.getHeight());
		Util.Animations.reveal_show(context, fs, new int[]{cx, cy}, finalRadius, Util.Animations.CustomAnimation.FADE_IN);

		// Translation animation between origin imageview location and fullscreen location
		// Set original imageview location
			/*int[] originalImageLocation = new int[2];
			iv.getLocationOnScreen(originalImageLocation);
			originalImageLocation[1] -= (Util.getStatusBarHeight(c) + Util.getActionBarHeight(c));

			LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(iv.getWidth(), iv.getHeight());
			lParams.setMargins(originalImageLocation[0], originalImageLocation[1], 0, 0);
			fullscreenImageView.setLayoutParams(lParams);*/

		// Download HD graph if possible
		if (grid.currentlyOpenedGridItem.getPlugin().getInstalledOn().getParent().isDynazoomAvailable() == MuninMaster.DynazoomAvailability.TRUE
				&& !Util.getPref(context, Util.PrefKeys.HDGraphs).equals("false")) {
			// We need to get imageView dimensions (which aren't available right now => globalLayoutListener)
			fullscreenImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					Util.removeOnGlobalLayoutListener(fullscreenImageView, this);

					// Check if HD graph is really needed : if the standard-res bitmap isn't upscaled, it's OK
					float xScale = ((float) fullscreenImageView.getWidth()) / item.originalGraph.getWidth();
					float yScale = ((float) fullscreenImageView.getHeight()) / item.originalGraph.getHeight();
					float scale = (xScale <= yScale) ? xScale : yScale;

					// Acceptable upscaling factor
					if (scale > 2.5) {
						if (item.hdGraphDownloader != null && item.hdGraphDownloader.isDownloading())
							item.hdGraphDownloader.killDownload();

						item.hdGraphDownloader = new GridItem.HDGraphDownloader(context, grid.currentlyOpenedGridItem.getPlugin(),
								fullscreenImageView, currentPeriod);
						item.hdGraphDownloader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					}
				}
			});
		}
	}

	public void hidePreview() {
		if (grid.currentlyOpenedGridItem == null)
			return;

		// Lollipop animation (fallback if necessary)
		View fs = view.findViewById(R.id.fullscreen);
		View gridItem = (View) grid.currentlyOpenedGridItem.container.getParent();
		View gridItemParent = (View) grid.currentlyOpenedGridItem.container.getParent().getParent();
		int cx = (gridItem.getLeft() + gridItem.getRight()) / 2;
		int cy = (gridItemParent.getTop() + gridItemParent.getBottom()) / 2;
		int initialRadius = Math.max(fs.getWidth(), fs.getHeight());
		Util.Animations.reveal_hide(context, fs, new int[]{cx, cy}, initialRadius, Util.Animations.CustomAnimation.FADE_OUT);

		grid.currentlyOpenedGridItem = null;
		activity.onPreviewHide();
	}

	public void edit() {
		activity.onEditModeChange(editing);

		if (editing) { // Cancel edit
			grid.cancelEdit(context);
			grid.toggleFootersVisibility(true);
			muninFoo.sqlite.dbHlpr.saveGridItemsRelations(grid);
		} else { // Edit
			grid.edit(view);
			grid.toggleFootersVisibility(false);
		}

		editing = !editing;
	}

	public boolean isUpdating() { return this.updating; }
	public void setUpdating(boolean val) { this.updating = val; }
	public boolean isEditing() { return this.editing; }
	public boolean isPreviewing() { return grid.currentlyOpenedGridItem != null; }
	public Grid getGrid() { return this.grid; }

	public MuninPlugin.Period getCurrentPeriod() { return this.currentPeriod; }
	public void setCurrentPeriod(MuninPlugin.Period period) { this.currentPeriod = period; }

	public void refresh() {
		if (!Util.isOnline(context))
			Toast.makeText(context, getString(R.string.text30), Toast.LENGTH_LONG).show();
		grid.dHelper.setPeriod(this.currentPeriod);
		grid.dHelper.start(true);
	}

	public void autoRefresh() {
		grid.dHelper.setPeriod(this.currentPeriod);
		grid.dHelper.start(true);
	}
}
