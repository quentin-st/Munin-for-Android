package com.chteuchteu.munin.hlpr;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_GraphView;
import com.edmodo.rangebar.RangeBar;

public final class DynazoomHelper {
	public static int GRAPH_LEFT_MARGIN = 65; // px
	public static int GRAPH_RIGHT_MARGIN = 30; // px

	public static int RANGEBAR_TICKS_COUNT = 30;

	public static void updateHighlightedArea(View highlightArea, RangeBar rangeBar, ImageView imageView) {
		int[] steps = { rangeBar.getLeftIndex(),  rangeBar.getRightIndex() };
		int[] highlightedAreaX = getHighlightedAreaFromSteps(steps, RANGEBAR_TICKS_COUNT, imageView.getWidth());

		// If we try to get the imageView position, we'll only get top=0, height=(full screen) since
		// the imageView layout_width && layout_height == MATCH_PARENT
		int[] bitmapPosition = Util.getBitmapPositionInsideImageView(imageView);

		int left = highlightedAreaX[0];
		int top = bitmapPosition[1];
		int width = highlightedAreaX[1] - highlightedAreaX[0];
		int height = bitmapPosition[3];

		MuninFoo.log("Height = " + height + ", top = " + top);

		FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) highlightArea.getLayoutParams();
		layoutParams.width = width;
		layoutParams.height = height;
		layoutParams.leftMargin = left;
		layoutParams.topMargin = top;

		highlightArea.setLayoutParams(layoutParams);
		highlightArea.invalidate();
	}

	public static int[] getHighlightedAreaFromSteps(int[] steps, int nbSteps, int imageViewWidth) {
		int[] areaXDimens = new int[2];

		int imgX1 = 140;
		int imgX2 = imageViewWidth - DynazoomHelper.GRAPH_RIGHT_MARGIN;
		int imgXDiff = imgX2 - imgX1;

		int step1 = steps[0];
		int step2 = steps[1];

		int hlAreaX1 = step1 * imgXDiff / nbSteps + imgX1;
		int hlAreaX2 = step2 * imgXDiff / nbSteps + imgX1;

		areaXDimens[0] = hlAreaX1;
		areaXDimens[1] = hlAreaX2;
		return areaXDimens;
	}

	public static class DynazoomFetcher extends AsyncTask<Void, Integer, Void> {
		private MuninServer server;
		private MuninPlugin plugin;

		private ImageView imageView;
		private ProgressBar progressBar;

		private Context context;
		private String userAgent;

		private Bitmap bitmap;

		public DynazoomFetcher (MuninServer server, MuninPlugin plugin, ImageView iv, ProgressBar progressBar, Context context, String userAgent) {
			super();
			this.server = server;
			this.plugin = plugin;

			this.imageView = iv;
			this.progressBar = progressBar;

			this.context = context;
			this.userAgent = userAgent;

			this.bitmap = null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			imageView.setImageBitmap(null);
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			int[] graphsDimensions = Util.HDGraphs.getBestImageDimensions(imageView, context);
			String imgUrl = plugin.getHDImgUrl(Activity_GraphView.load_period, true, graphsDimensions[0], graphsDimensions[1]);

			bitmap = Util.removeBitmapBorder(server.getParent().grabBitmap(imgUrl, userAgent));

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			progressBar.setVisibility(View.GONE);

			if (bitmap != null)
				imageView.setImageBitmap(bitmap);
			else {
				// It seems that can actually fire OutOfMemoryError (BitmapFactory.nativeDecodeAsset)
				try {
					imageView.setImageResource(R.drawable.download_error);
				} catch (Exception e) { e.printStackTrace(); }
			}
		}
	}
}
