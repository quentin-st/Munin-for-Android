package com.chteuchteu.munin.async;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.HTTPResponse.BitmapResponse;
import com.chteuchteu.munin.obj.MuninPlugin;

public class LightBitmapFetcher extends AsyncTask<Void, Integer, Void> {
	private MuninFoo muninFoo;

	private ProgressBar progressBar;

	private MuninPlugin plugin;
	private MuninPlugin.Period period;

	private OnBitmapDownloaded callback;
	private Bitmap bitmap;

	public LightBitmapFetcher(ProgressBar progressBar,
							  MuninPlugin.Period period, MuninPlugin plugin,
							  OnBitmapDownloaded callback) {
		this.muninFoo = MuninFoo.getInstance();
		this.progressBar = progressBar;
		this.period = period;
		this.plugin = plugin;
		this.callback = callback;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		if (progressBar != null)
			progressBar.setVisibility(View.VISIBLE);
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		String imgUrl = plugin.getImgUrl(period);

		BitmapResponse response = plugin.getInstalledOn().getParent().downloadBitmap(imgUrl, muninFoo.getUserAgent());

		if (response.hasSucceeded())
			this.bitmap = Util.removeBitmapBorder(response.getBitmap());

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (progressBar != null)
			progressBar.setVisibility(View.GONE);

		this.callback.onBitmapDownloaded(bitmap);
	}

	public interface OnBitmapDownloaded
	{
		/**
		 * Will be called once the download has ended.
		 * @param bitmap may be null
         */
		void onBitmapDownloaded(Bitmap bitmap);
	}
}
