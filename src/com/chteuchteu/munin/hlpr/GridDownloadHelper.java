package com.chteuchteu.munin.hlpr;

import java.util.List;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.MuninPlugin.Period;

public class GridDownloadHelper {
	private Grid g;
	
	private int nbSimultaneousDownloads = 3;
	public Period period = Period.DAY;
	
	private List<GridItem> items;
	
	public GridDownloadHelper(Grid g) {
		this.g = g;
	}
	
	public void init(int nbSimultaneousDownloads, Period p) {
		this.nbSimultaneousDownloads = nbSimultaneousDownloads;
		this.period = p;
		items = g.items;
	}
	
	public void start(boolean forceUpdate) {
		for (GridItem i : items)
			i.pb.setVisibility(View.VISIBLE);
		
		for (int i=0; i<this.nbSimultaneousDownloads; i++) {
			if (items.size() > i)
				new DownloadBitmaps(i, forceUpdate).execute();
		}
	}
	
	public class DownloadBitmaps extends AsyncTask<Void, Integer, Void> {
		private int i;
		private boolean forceUpdate;
		private Bitmap b;
		
		public DownloadBitmaps(int i, boolean forceUpdate) {
			this.i = i;
			this.forceUpdate = forceUpdate;
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (forceUpdate || items.get(i).iv.getDrawable() == null)
				b = Util.removeBitmapBorder(MuninFoo.grabBitmap(items.get(i).plugin.getInstalledOn(), items.get(i).plugin.getImgUrl(period.toString())));
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if (b != null) {
				items.get(i).iv.setImageBitmap(b);
				items.get(i).graph = b;
			}
			items.get(i).pb.setVisibility(View.GONE);
			
			int next = i + nbSimultaneousDownloads;
			if (next < items.size())
				new DownloadBitmaps(next, forceUpdate).execute();
		}
	}
}