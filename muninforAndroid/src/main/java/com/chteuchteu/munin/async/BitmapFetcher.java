package com.chteuchteu.munin.async;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.HTTPResponse_Bitmap;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_GraphView;

import uk.co.senab.photoview.PhotoViewAttacher;

public class BitmapFetcher extends AsyncTask<Void, Integer, Void> {
	private static final int[] AVERAGE_GRAPH_DIMENSIONS = {455, 350};

	private Activity_GraphView activity;
	private MuninFoo muninFoo;

	private ImageView imageView;
	private int position;
	private Context context;
	private ProgressBar progressBar;
	private View view;

	private MuninPlugin plugin;
	private MuninServer server;

	private HTTPResponse_Bitmap response;

	public BitmapFetcher (Activity_GraphView activity, ImageView iv, ProgressBar progressBar, View view, int position, Context context) {
		this.muninFoo = MuninFoo.getInstance();
		this.activity = activity;
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

			if (server.getParent().isDynazoomAvailable() == MuninMaster.DynazoomAvailability.TRUE
					&& !Util.getPref(context, Util.PrefKeys.HDGraphs).equals("false")) { // Dynazoom (HD graph)
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
			} else // Standard graph
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

			// Update or create PhotoViewAttacher
			if (Util.getPref(context, Util.PrefKeys.GraphsZoom).equals("true")) {
				if (activity.photoViewAttachers.keySet().contains(position)) {
					PhotoViewAttacher mAttacher = activity.photoViewAttachers.get(position);
					mAttacher.update();
				} else {
					PhotoViewAttacher newAttacher = new PhotoViewAttacher(imageView);
					activity.photoViewAttachers.put(position, newAttacher);
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
			imageView.setImageBitmap(null);

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
					&& this.server.getParent().isDynazoomAvailable() == MuninMaster.DynazoomAvailability.TRUE
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
						server.getParent().setDynazoomAvailable(MuninMaster.DynazoomAvailability.FALSE);
						MuninFoo.getInstance(context).sqlite.dbHlpr.updateMuninMaster(server.getParent());
						activity.fab.hide(true);
						activity.isFabShown = false;
						activity.actionRefresh();
					}
				});

				rescanHdGraphsUrl.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						new DynazoomUrlScanner(activity, server.getParent(), context).execute();
					}
				});
			} else {
				view.findViewById(R.id.error_rescanHdGraphsUrl).setVisibility(View.GONE);
				view.findViewById(R.id.error_disableHdGraphs).setVisibility(View.GONE);
			}

			view.findViewById(R.id.error_refresh).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					activity.actionRefresh();
				}
			});

			View openInBrowser = view.findViewById(R.id.error_openInBrowser);
			openInBrowser.setVisibility(plugin.hasPluginPageUrl() ? View.VISIBLE : View.GONE);
			openInBrowser.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					activity.actionOpenInBrowser();
				}
			});
		}

		// Connection type
		activity.updateConnectionType(response.getConnectionType());
	}
}
