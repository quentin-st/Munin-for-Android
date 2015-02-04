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
	private int nbSimultaneousDownloads;
	private Period period;
	private Fragment_Grid fragment;
	
	public GridDownloadHelper(Grid grid, int nbSimultaneousDownloads, Period period, Fragment_Grid fragment) {
		this.grid = grid;
		this.nbSimultaneousDownloads = nbSimultaneousDownloads;
		this.period = period;
		this.fragment = fragment;
	}

	public void setPeriod(Period val) { this.period = val; }
	
	public void start(boolean forceUpdate) {
		for (GridItem item : grid.getItems())
			item.pb.setVisibility(View.VISIBLE);
		
		onStart();
		
		for (int i=0; i<this.nbSimultaneousDownloads; i++) {
			if (grid.getItems().size() > i)
				new DownloadBitmaps(i, forceUpdate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}
	
	private void onStart() {
		fragment.setUpdating(true);
	}
	
	private void onStop() {
		fragment.setUpdating(false);
	}
	
	private class DownloadBitmaps extends AsyncTask<Void, Integer, Void> {
		private int i;
		private boolean forceUpdate;
		private Bitmap originalBitmap;
		private Bitmap croppedBitmap;
		
		private DownloadBitmaps(int i, boolean forceUpdate) {
			this.i = i;
			this.forceUpdate = forceUpdate;
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (i < grid.getItems().size()) {
				GridItem gridItem = grid.getItems().get(i);
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
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			int gridItemsSize = grid.getItems().size();
			List<GridItem> gridItems = grid.getItems();

			if (i == gridItemsSize - 1)
				onStop();
			if (i < gridItemsSize) {
				if (gridItems.get(i) != null) {
					if (originalBitmap != null) {
						gridItems.get(i).iv.setImageBitmap(croppedBitmap);
						gridItems.get(i).originalGraph = originalBitmap;
						gridItems.get(i).croppedGraph = croppedBitmap;
					}
					gridItems.get(i).pb.setVisibility(View.GONE);
				}
				
				int next = i + nbSimultaneousDownloads;
				if (next < gridItemsSize)
					new DownloadBitmaps(next, forceUpdate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
	}
}
