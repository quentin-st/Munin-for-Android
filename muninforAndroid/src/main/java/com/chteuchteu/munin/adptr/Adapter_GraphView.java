package com.chteuchteu.munin.adptr;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.HTTPResponse_Bitmap;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninMaster.DynazoomAvailability;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_GraphView;

import org.taptwo.android.widget.TitleProvider;

import uk.co.senab.photoview.PhotoViewAttacher;

public class Adapter_GraphView extends BaseAdapter implements TitleProvider {
	private static final int[] AVERAGE_GRAPH_DIMENSIONS = {455, 350};
	private MuninFoo muninFoo;
	private Activity_GraphView activity;
	private Context context;
	private LayoutInflater mInflater;
	private int count;

	public Adapter_GraphView(Activity_GraphView activity, MuninFoo muninFoo, int count) {
		this.activity = activity;
		this.muninFoo = muninFoo;
		this.count = count;
		this.context = activity;
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
		activity.updateAdapterPosition(position);

		if (convertView == null) {
			if (mInflater == null)
				mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = mInflater.inflate(R.layout.fragment_graphview, parent, false);
		}

		convertView.findViewById(R.id.error).setVisibility(View.GONE);
		
		if (activity.loadGraphs) {
			ImageView imageView = (ImageView) convertView.findViewById(R.id.tiv);
			ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressbar);
			
			if (activity.isBitmapNull(position)) {
				//                                                                         Avoid serial execution
				new BitmapFetcher(imageView, progressBar, convertView, position, context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
		private View view;

		private MuninPlugin plugin;
		private MuninServer server;

		private HTTPResponse_Bitmap response;
		
		private BitmapFetcher (ImageView iv, ProgressBar progressBar, View view, int position, Context context) {
			super();
			this.imageView = iv;
			this.position = position;
			this.context = context;
			this.progressBar = progressBar;
			this.view = view;

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
			view.findViewById(R.id.error).setVisibility(View.GONE);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (activity.isBitmapNull(position)) {
				String imgUrl;
				if (server.getParent().isDynazoomAvailable() == DynazoomAvailability.TRUE
						&& !Util.getPref(context, Util.PrefKeys.HDGraphs).equals("false")) {
					// Check if HD graph is really needed : if the standard-res bitmap isn't upscaled, it's OK
					float xScale = ((float) imageView.getWidth()) / AVERAGE_GRAPH_DIMENSIONS[0];
					float yScale = ((float) imageView.getHeight()) / AVERAGE_GRAPH_DIMENSIONS[1];
					float scale = (xScale <= yScale) ? xScale : yScale;

					// Acceptable upscaling factor
					if (scale > 2.5) {
						int[] graphsDimensions = Util.HDGraphs.getBestImageDimensions(imageView, context);
						imgUrl = plugin.getHDImgUrl(activity.load_period, true, graphsDimensions[0], graphsDimensions[1]);
					}
					else
						imgUrl = plugin.getImgUrl(activity.load_period);
				} else
					imgUrl = plugin.getImgUrl(activity.load_period);

				this.response = server.getParent().grabBitmap(imgUrl, muninFoo.getUserAgent());

				if (response.hasSucceeded())
					activity.addBitmap(Util.removeBitmapBorder(response.getBitmap()), position);
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

				// If documentation shown && image just loaded: display it
				if (activity.iv_documentation != null) {
					Object tag = activity.iv_documentation.getTag();
					if (tag != null && tag.equals(plugin.getName()))
						activity.iv_documentation.setImageBitmap(activity.getBitmap(position));
				}
			} else {
				// Display error
				view.findViewById(R.id.error).setVisibility(View.VISIBLE);
				TextView errorText = (TextView) view.findViewById(R.id.error_text);
				Util.Fonts.setFont(context, errorText, Util.Fonts.CustomFont.Roboto_Regular);

				Util.Fonts.setFont(context, ((TextView) view.findViewById(R.id.error_title)), Util.Fonts.CustomFont.Roboto_Regular);

				if (response.getResponseCode() < 0) { // Not HTTP error
					if (response.getResponseCode() == HTTPResponse_Bitmap.UnknownHostExceptionError
							&& !Util.isOnline(context))
						errorText.setText(context.getString(R.string.text30) + "\n" + response.getResponsePhrase());
					else
						errorText.setText(response.getResponsePhrase());
				}
				else
					errorText.setText(response.getResponseCode() + " - " + response.getResponsePhrase());

				// Allow user to disable HD Graphs / rescan HD Graphs URL
				if (!Util.getPref(context, Util.PrefKeys.HDGraphs).equals("false")
						&& this.server.getParent().isDynazoomAvailable() == DynazoomAvailability.TRUE
						&& Util.isOnline(context)) {
					Button rescanHdGraphsUrl = (Button) view.findViewById(R.id.error_rescanHdGraphsUrl);
					Button disableHdGraphs = (Button) view.findViewById(R.id.error_disableHdGraphs);
					rescanHdGraphsUrl.setVisibility(View.VISIBLE);
					disableHdGraphs.setVisibility(View.VISIBLE);

					Util.Fonts.setFont(context, rescanHdGraphsUrl, Util.Fonts.CustomFont.Roboto_Regular);
					Util.Fonts.setFont(context, disableHdGraphs, Util.Fonts.CustomFont.Roboto_Regular);

					disableHdGraphs.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							server.getParent().setDynazoomAvailable(DynazoomAvailability.FALSE);
							MuninFoo.getInstance(context).sqlite.dbHlpr.updateMuninMaster(server.getParent());
							activity.fab.hide(true);
							activity.isFabShown = false;
							activity.actionRefresh();
						}
					});

					rescanHdGraphsUrl.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							new DynazoomUrlScanner(server.getParent(), context).execute();
						}
					});
				} else {
					view.findViewById(R.id.error_rescanHdGraphsUrl).setVisibility(View.GONE);
					view.findViewById(R.id.error_disableHdGraphs).setVisibility(View.GONE);
				}
			}

			// Connection type
			activity.updateConnectionType(response.getConnectionType());
		}
	}

	private class DynazoomUrlScanner extends AsyncTask<Void, Integer, Void> {
		private ProgressDialog dialog;
		private Context context;
		private MuninMaster master;

		private DynazoomUrlScanner(MuninMaster master, Context context) {
			this.master = master;
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			master.rescan(context, muninFoo);

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (dialog != null && dialog.isShowing()) {
				try {
					dialog.dismiss();
				} catch (Exception ex) { ex.printStackTrace(); }
			}

			if (master.isDynazoomAvailable() == DynazoomAvailability.FALSE) {
				activity.fab.hide(true);
				activity.isFabShown = false;
			}

			activity.actionRefresh();
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
