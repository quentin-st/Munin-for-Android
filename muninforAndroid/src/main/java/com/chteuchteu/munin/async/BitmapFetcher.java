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
import com.chteuchteu.munin.hlpr.Settings;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.HTTPResponse.BitmapResponse;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
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
	private MuninNode node;

	private BitmapResponse response;

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
			this.plugin = muninFoo.getCurrentNode().getPlugin(position);
			this.node = muninFoo.getCurrentNode();
		} else { // VIEWFLOWMODE_LABELS
			this.plugin = activity.label.getPlugins().get(position);
			this.node = this.plugin.getInstalledOn();
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

			Settings settings = Settings.getInstance(context);

			if (node.getParent().isDynazoomAvailable() == MuninMaster.DynazoomAvailability.TRUE
					&& !settings.getBool(Settings.PrefKeys.HDGraphs)) { // Dynazoom (HD graph)
				// Check if HD graph is really needed : if the standard-res bitmap isn't upscaled, it's OK
				float xScale = ((float) activity.imageViewDimensions[0]) / AVERAGE_GRAPH_DIMENSIONS[0];
				float yScale = ((float) activity.imageViewDimensions[1]) / AVERAGE_GRAPH_DIMENSIONS[1];
				float scale = (xScale <= yScale) ? xScale : yScale;

				// Acceptable upscaling factor
				if (scale > 2.4) {
					int[] graphsDimensions = Util.HDGraphs.getBestImageDimensions(imageView, context);
					imgUrl = plugin.getHDImgUrl(activity.load_period, true, graphsDimensions[0], graphsDimensions[1]);
				}
				else
					imgUrl = plugin.getImgUrl(activity.load_period);
			} else // Standard graph
				imgUrl = plugin.getImgUrl(activity.load_period);

			this.response = node.getParent().downloadBitmap(imgUrl, muninFoo.getUserAgent());

			if (response.hasSucceeded())
				activity.addBitmap(Util.removeBitmapBorder(response.getBitmap()), position);
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		progressBar.setVisibility(View.GONE);

		Settings settings = Settings.getInstance(context);

		if (!activity.isBitmapNull(position)) {
			imageView.setImageBitmap(activity.getBitmap(position));

			// Update or create PhotoViewAttacher
			if (settings.getBool(Settings.PrefKeys.GraphsZoom)) {
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
				if (response.getResponseCode() == BitmapResponse.UnknownHostExceptionError
						&& !Util.isOnline(context))
					errorText.setText(context.getString(R.string.text30) + "\n" + response.getResponseMessage());
				else
					errorText.setText(response.getResponseMessage());
			}
			else
				errorText.setText(response.getResponseCode() + " - " + response.getResponseMessage());

			// Allow user to disable HD Graphs / rescan HD Graphs URL
			if (!settings.getBool(Settings.PrefKeys.HDGraphs)
					&& this.node.getParent().isDynazoomAvailable() == MuninMaster.DynazoomAvailability.TRUE
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
						node.getParent().setDynazoomAvailable(MuninMaster.DynazoomAvailability.FALSE);
						MuninFoo.getInstance(context).sqlite.dbHlpr.updateMuninMaster(node.getParent());
						activity.fab.hide(true);
						activity.isFabShown = false;
						activity.actionRefresh();
					}
				});

				rescanHdGraphsUrl.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
                        node.getParent().setDynazoomAvailable(MuninMaster.DynazoomAvailability.AUTO_DETECT);
						new DynazoomUrlScanner(activity, node.getParent(), context).execute();
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
		if (response != null)
            activity.updateConnectionType(response.getConnectionType());
	}
}
