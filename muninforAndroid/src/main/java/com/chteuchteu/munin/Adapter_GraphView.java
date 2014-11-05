package com.chteuchteu.munin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninMaster.HDGraphs;
import com.chteuchteu.munin.ui.Activity_GraphView;

import org.taptwo.android.widget.TitleProvider;

import uk.co.senab.photoview.PhotoViewAttacher;

public class Adapter_GraphView extends BaseAdapter implements TitleProvider {
	private MuninFoo		muninFoo;
	private Activity_GraphView activity;
	private Context		context;
	private LayoutInflater mInflater;

	private int			count;
	
	public Adapter_GraphView(Activity_GraphView activity, MuninFoo muninFoo, Context context, int count) {
		this.activity = activity;
		this.muninFoo = muninFoo;
		this.count = count;
		this.context = context;
	}
	
	@Override
	public int getCount() {
		return count;
	}
	
	@Override
	public Object getItem(int position) {
		return position;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		activity.updateAdapterPosition(position);

		if (convertView == null) {
			if (mInflater == null)
				mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = mInflater.inflate(R.layout.fragment_graphview, null);
		}
		
		if (Activity_GraphView.loadGraphs) {
			ImageView imageView = (ImageView) convertView.findViewById(R.id.tiv);
			ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressbar);
			
			if (activity.isBitmapNull(position))
				new BitmapFetcher(imageView, progressBar, position, context).execute();
			else {
				imageView.setImageBitmap(activity.getBitmap(position));
				progressBar.setVisibility(View.GONE);
			}
		}
		
		return convertView;
	}
	
	private class BitmapFetcher extends AsyncTask<Void, Integer, Void> {
		private ImageView imageView;
		private int position;
		private Context context;
		private ProgressBar progressBar;
		
		private BitmapFetcher (ImageView iv, ProgressBar progressBar, int position, Context context) {
			super();
			this.imageView = iv;
			this.position = position;
			this.context = context;
			this.progressBar = progressBar;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			imageView.setImageBitmap(null);
			progressBar.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (activity.isBitmapNull(position)) {
				String imgUrl;
				if (muninFoo.getCurrentServer(context).getParent().getHDGraphs() == HDGraphs.TRUE && !Util.getPref(context, "hdGraphs").equals("false")) {
					int[] graphsDimensions = Util.HDGraphs.getBestImageDimensions(imageView, context);
					imgUrl = muninFoo.getCurrentServer().getPlugin(position).getHDImgUrl(
							Activity_GraphView.load_period, true, graphsDimensions[0], graphsDimensions[1]);
				} else
					imgUrl = muninFoo.getCurrentServer().getPlugin(position).getImgUrl(Activity_GraphView.load_period);


				activity.addBitmap(Util.removeBitmapBorder(
								muninFoo.getCurrentServer().getParent().grabBitmap(imgUrl)),
						position);
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			progressBar.setVisibility(View.GONE);
			
			if (!activity.isBitmapNull(position)) {
				imageView.setImageBitmap(activity.getBitmap(position));

				if (Util.getPref(context, "graphsZoom").equals("true")) {
					if (!activity.photoViewAttached[position]) {
						activity.photoViewAttached[position] = true;
						PhotoViewAttacher mAttacher = new PhotoViewAttacher(imageView);
						if (mAttacher.getMidScale() < 2f)
							mAttacher.setMaxScale(2f);
					}
				}
			} else {
				// It seems that can actually fire OutOfMemoryError (BitmapFactory.nativeDecodeAsset)
				try {
					imageView.setImageResource(R.drawable.download_error);
				} catch (Exception e) { e.printStackTrace(); }
			}
		}
	}
	
	@Override
	public String getTitle(int position) {
		if (position >= 0 && position < muninFoo.getCurrentServer().getPlugins().size())
			return muninFoo.getCurrentServer().getPlugin(position).getFancyName();
		return "";
	}
}
