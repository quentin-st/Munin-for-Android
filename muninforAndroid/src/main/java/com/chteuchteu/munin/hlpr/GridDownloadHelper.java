package com.chteuchteu.munin.hlpr;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.ui.Fragment_Grid;

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
		for (GridItem item : grid.items)
			item.pb.setVisibility(View.VISIBLE);
		
		onStart();
		
		for (int i=0; i<this.nbSimultaneousDownloads; i++) {
			if (grid.items.size() > i)
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
		private Bitmap b;
		
		private DownloadBitmaps(int i, boolean forceUpdate) {
			this.i = i;
			this.forceUpdate = forceUpdate;
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (i < grid.items.size()) {
				GridItem gridItem = grid.items.get(i);
				if (forceUpdate || gridItem.iv.getDrawable() == null) {
					if (gridItem != null && gridItem.plugin != null && gridItem.plugin.getInstalledOn() != null
							&& gridItem.plugin.getInstalledOn().getParent() != null) {
						String graphUrl = gridItem.plugin.getImgUrl(period);
						b = Util.dropShadow(
								Util.removeBitmapBorder(
										gridItem.plugin.getInstalledOn().getParent().grabBitmap(graphUrl,
												MuninFoo.getInstance().getUserAgent()).getBitmap()));
					}
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (i == grid.items.size() - 1)
				onStop();
			if (i < grid.items.size()) {
				if (grid.items.get(i) != null) {
					if (b != null) {
						grid.items.get(i).iv.setImageBitmap(b);
						grid.items.get(i).graph = b;
					}
					grid.items.get(i).pb.setVisibility(View.GONE);
				}
				
				int next = i + nbSimultaneousDownloads;
				if (next < grid.items.size())
					new DownloadBitmaps(next, forceUpdate).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
	}
}
