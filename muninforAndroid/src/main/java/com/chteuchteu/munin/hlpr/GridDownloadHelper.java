package com.chteuchteu.munin.hlpr;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.ui.Fragment_Grid;

import java.util.List;

public class GridDownloadHelper {
	private Grid grid;
	private Period period;
	private Fragment_Grid fragment;
	
	public GridDownloadHelper(Grid grid, Period period, Fragment_Grid fragment) {
		this.grid = grid;
		this.period = period;
		this.fragment = fragment;
	}

	public void setPeriod(Period val) { this.period = val; }
	
	public void start(boolean forceUpdate) {
		for (GridItem item : grid.getItems())
			item.pb.setVisibility(View.VISIBLE);
		
		onStart();
		
		for (GridItem item : grid.getItems()) {
			boolean onStopWhenFinished = grid.getItems().indexOf(item) == grid.getItems().size()-1;
			new BitmapDownloader(item, forceUpdate, onStopWhenFinished).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	/**
	 * Only download graph for those items
	 */
	public void startForItems(List<GridItem> items) {
		if (items.isEmpty())
			return;

		for (GridItem item : items)
			item.pb.setVisibility(View.VISIBLE);

		onStart();

		for (GridItem item : items) {
			boolean onStopWhenFinished = items.indexOf(item) == items.size()-1;
			new BitmapDownloader(item, true, onStopWhenFinished).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}
	
	private void onStart() {
		fragment.setUpdating(true);
	}
	
	private void onStop() {
		fragment.setUpdating(false);
	}
	
	private class BitmapDownloader extends AsyncTask<Void, Integer, Void> {
		private GridItem gridItem;
		private boolean forceUpdate;
		private Bitmap originalBitmap;
		private Bitmap croppedBitmap;
		private boolean onStopWhenFinished;
		
		private BitmapDownloader(GridItem item, boolean forceUpdate, boolean onStopWhenFinished) {
			this.gridItem = item;
			this.forceUpdate = forceUpdate;
			this.onStopWhenFinished = onStopWhenFinished;
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (forceUpdate || gridItem.iv.getDrawable() == null) {
				if (gridItem != null && gridItem.getPlugin() != null && gridItem.getPlugin().getInstalledOn() != null
						&& gridItem.getPlugin().getInstalledOn().getParent() != null) {
					String graphUrl = gridItem.getPlugin().getImgUrl(period);

					Bitmap downloadedBitmap = Util.removeBitmapBorder(gridItem.getPlugin().getInstalledOn().getParent().grabBitmap(graphUrl,
							MuninFoo.getInstance().getUserAgent()).getBitmap());

					originalBitmap = Util.dropShadow(downloadedBitmap);
					croppedBitmap = Util.dropShadow(Util.extractGraph(downloadedBitmap));
				}
			}

			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (onStopWhenFinished)
				onStop();

			if (gridItem != null) {
				if (originalBitmap != null && croppedBitmap != null) {
					gridItem.iv.setImageBitmap(croppedBitmap);
					gridItem.originalGraph = originalBitmap;
					gridItem.croppedGraph = croppedBitmap;
				}
				else if (forceUpdate)
					gridItem.applyPlaceholder(true);
				gridItem.pb.setVisibility(View.GONE);
			}
		}
	}
}
