package com.chteuchteu.munin.adptr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninMaster.DynazoomAvailability;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_GraphView;

import org.taptwo.android.widget.TitleProvider;

import uk.co.senab.photoview.PhotoViewAttacher;

public class Adapter_GraphView extends BaseAdapter implements TitleProvider {
	private MuninFoo muninFoo;
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
			
			if (activity.isBitmapNull(position)) {
				// Avoid serial execution
				new BitmapFetcher(imageView, progressBar, position, context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
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

		private MuninPlugin plugin;
		private MuninServer server;
		
		private BitmapFetcher (ImageView iv, ProgressBar progressBar, int position, Context context) {
			super();
			this.imageView = iv;
			this.position = position;
			this.context = context;
			this.progressBar = progressBar;

            // ViewFlowMode : graphs / labels
			if (activity.viewFlowMode == Activity_GraphView.VIEWFLOWMODE_GRAPHS) {
				this.plugin = muninFoo.getCurrentServer().getPlugin(position);
				this.server = muninFoo.getCurrentServer();
			} else { // VIEWFLOWMODE_LABELS
				this.plugin = activity.label.getPlugins().get(position);
				this.server = this.plugin.getInstalledOn();
			}
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
				if (server.getParent().isDynazoomAvailable() == DynazoomAvailability.TRUE
						&& !Util.getPref(context, Util.PrefKeys.HDGraphs).equals("false")) {
					int[] graphsDimensions = Util.HDGraphs.getBestImageDimensions(imageView, context);
					imgUrl = plugin.getHDImgUrl(activity.load_period, true, graphsDimensions[0], graphsDimensions[1]);
                    MuninFoo.log("Graph url : " + imgUrl);
				} else
					imgUrl = plugin.getImgUrl(activity.load_period);

				activity.addBitmap(Util.removeBitmapBorder(server.getParent().grabBitmap(imgUrl, muninFoo.getUserAgent())), position);
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			progressBar.setVisibility(View.GONE);
			
			if (!activity.isBitmapNull(position)) {
				imageView.setImageBitmap(activity.getBitmap(position));

				// PhotoViewAttacher
				if (Util.getPref(context, Util.PrefKeys.GraphsZoom).equals("true")) {
					if (!activity.photoViewAttached[position]) {
						activity.photoViewAttached[position] = true;
						PhotoViewAttacher mAttacher = new PhotoViewAttacher(imageView);
						if (mAttacher.getMidScale() < 2f)
							mAttacher.setMaxScale(2f);
					}
				}

				// If documentation shown && image just loaded : display it
				if (activity.iv_documentation != null) {
					Object tag = activity.iv_documentation.getTag();
					if (tag != null && tag.equals(plugin.getName()))
						activity.iv_documentation.setImageBitmap(activity.getBitmap(position));
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
		if (position < 0)
			return "";

		if (activity.viewFlowMode == Activity_GraphView.VIEWFLOWMODE_GRAPHS) {
			if (position > muninFoo.getCurrentServer().getPlugins().size())
				return "";

			return muninFoo.getCurrentServer().getPlugin(position).getFancyName();
		} else {
			if (position > activity.label.getPlugins().size())
				return "";

			return activity.label.getPlugins().get(position).getFancyName();
		}
	}
}
