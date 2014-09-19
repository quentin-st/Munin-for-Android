package com.chteuchteu.munin;

import org.taptwo.android.widget.TitleProvider;

import uk.co.senab.photoview.PhotoViewAttacher;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.ui.Activity_GraphView;

public class Adapter_GraphView extends BaseAdapter implements TitleProvider {
	private MuninFoo		muninFoo;
	
	private LayoutInflater	mInflater;
	private int			count;
	
	public Adapter_GraphView(Context context, int count) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		muninFoo = MuninFoo.getInstance(context);
		this.count = count;
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
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null)
			convertView = mInflater.inflate(R.layout.fragment_graphview, null);
		
		ImageView imageView = (ImageView) convertView.findViewById(R.id.tiv);
		
		if (Activity_GraphView.bitmaps[position] == null) {
			new BitmapFetcher(imageView, position).execute();
		} else {
			imageView.setImageBitmap(Activity_GraphView.bitmaps[position]);
		}
		
		return convertView;
	}
	
	public class BitmapFetcher extends AsyncTask<Void, Integer, Void> {
		private ImageView imageView;
		private int position;
		
		public BitmapFetcher (ImageView iv, int position) {
			super();
			this.imageView = iv;
			this.position = position;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			imageView.setImageBitmap(null);
			
			Activity_GraphView.currentlyDownloading_begin();
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (Activity_GraphView.bitmaps[position] == null)
				Activity_GraphView.bitmaps[position] = 
					Util.dropShadow(Util.removeBitmapBorder(
							MuninFoo.grabBitmap(muninFoo.currentServer, muninFoo.currentServer.getPlugin(position).getImgUrl(Activity_GraphView.load_period)))
				);
				
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			Activity_GraphView.currentlyDownloading_finished();
			
			if (Activity_GraphView.bitmaps[position] != null) {
				imageView.setImageBitmap(Activity_GraphView.bitmaps[position]);
				PhotoViewAttacher mAttacher = new PhotoViewAttacher(imageView);
				if (mAttacher.getMidScale() < 2f)
					mAttacher.setMaxScale(2f);
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
		if (position >= 0 && position < muninFoo.currentServer.getPlugins().size())
			return muninFoo.currentServer.getPlugin(position).getFancyName();
		return "";
	}
}
