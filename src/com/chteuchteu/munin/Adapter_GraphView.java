package com.chteuchteu.munin;

import org.taptwo.android.widget.TitleProvider;

import uk.co.senab.photoview.PhotoViewAttacher;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninPlugin.Period;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_GraphView;

public class Adapter_GraphView extends BaseAdapter implements TitleProvider {
	private MuninFoo		muninFoo;
	
	private LayoutInflater	mInflater;
	private int 			position;
	
	public Adapter_GraphView(Context context) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		muninFoo = MuninFoo.getInstance(context);
	}
	
	@Override
	public int getCount() {
		if (muninFoo == null)
			muninFoo = MuninFoo.getInstance();
		return muninFoo.currentServer.getPlugins().size();
	}
	
	@Override
	public Object getItem(int position) {
		return position;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null)
			convertView = mInflater.inflate(R.layout.fragment_graphview, null);
		this.position = position;
		
		ImageView imageView = (ImageView) convertView.findViewById(R.id.tiv);
		ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.loading_spin);
		
		imageView.setTag(position);
		
		if (this.position >= 0 && this.position < Activity_GraphView.bitmaps.length
				&& Activity_GraphView.bitmaps[this.position] == null)
			new ApplyBitmap(imageView, progressBar, position).execute();
		
		
		return convertView;
	}
	
	public class ApplyBitmap extends AsyncTask<Void, Integer, Void> {
		private ImageView tiv;
		private ProgressBar loading_spin;
		private int position;
		
		public ApplyBitmap (ImageView iv, ProgressBar ls, int position) {
			super();
			this.tiv = iv;
			this.loading_spin = ls;
			this.position = position;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			tiv.setImageBitmap(null);
			
			if (Activity_GraphView.bitmaps[position] == null) {
				loading_spin.setIndeterminate(true);
				loading_spin.setVisibility(View.VISIBLE);
			}
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			Period period = Activity_GraphView.load_period;
			MuninServer server = muninFoo.currentServer;
			Bitmap[] bitmaps = Activity_GraphView.bitmaps;
			
			if (period == null || server == null || bitmaps == null)
				return null;
			
			String imgUrl = server.getPlugin(position).getImgUrl(period);
			
			bitmaps[position] = Util.removeBitmapBorder(
				MuninFoo.grabBitmap(server, imgUrl)
			);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			loading_spin.setVisibility(View.GONE);
			
			if (Activity_GraphView.bitmaps[position] != null) {
				tiv.setImageBitmap(Activity_GraphView.bitmaps[position]);
				PhotoViewAttacher mAttacher = new PhotoViewAttacher(tiv);
				if (mAttacher.getMidScale() < 2f)
					mAttacher.setMaxScale(2f);
			} else {
				// It seems that can actually fire OutOfMemoryError (BitmapFactory.nativeDecodeAsset)
				try {
					tiv.setImageResource(R.drawable.download_error);
				} catch (Exception e) { e.printStackTrace(); }
			}
		}
	}
	
	@Override
	public String getTitle(int position) {
		if (position >= 0 && position < muninFoo.currentServer.getPlugins().size())
			return muninFoo.currentServer.getPlugin(position).getFancyName();
		return "";
	}
}
