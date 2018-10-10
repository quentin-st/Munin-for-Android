package com.chteuchteu.munin.hlpr;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.edmodo.rangebar.RangeBar;

import java.util.Calendar;

public class DynazoomHelper {
	private static final int GRAPH_LEFT_MARGIN = 155;
	private static final int GRAPH_RIGHT_MARGIN = 50;
	// Because of the bitmap shadow
	private static final int GRAPH_TOP_MARGIN = 7;
	private static final int GRAPH_BOTTOM_MARGIN = 13;

	public static int RANGEBAR_TICKS_COUNT = 30;

	public static void updateHighlightedArea(View highlightArea1, View highlightArea2, RangeBar rangeBar, ImageView imageView) {
		int[] steps = { rangeBar.getLeftIndex(),  rangeBar.getRightIndex() };
		int[] highlightedAreaX1 = getHighlightedAreaFromSteps(steps, RANGEBAR_TICKS_COUNT, imageView.getWidth(), 1);
		int[] highlightedAreaX2 = getHighlightedAreaFromSteps(steps, RANGEBAR_TICKS_COUNT, imageView.getWidth(), 2);

		// If we try to get the imageView position, we'll only get top=0, height=(full screen) since
		// the imageView layout_width && layout_height == MATCH_PARENT
		int[] bitmapPosition = Util.getBitmapPositionInsideImageView(imageView);

		int left, top, width, height;

		top = bitmapPosition[1] + GRAPH_TOP_MARGIN;
		height = bitmapPosition[3] - (GRAPH_TOP_MARGIN + GRAPH_BOTTOM_MARGIN);

		// Left mask
		left = highlightedAreaX1[0];
		width = highlightedAreaX1[1] - highlightedAreaX1[0];
		setLayoutParams(highlightArea1, width, height, left, top);

		// Right mask
		left = highlightedAreaX2[0];
		width = highlightedAreaX2[1] - highlightedAreaX2[0];
		setLayoutParams(highlightArea2, width, height, left, top);
	}

	private static void setLayoutParams(View view, int width, int height, int left, int top) {
		FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
		layoutParams.width = width;
		layoutParams.height = height;
		layoutParams.leftMargin = left;
		layoutParams.topMargin = top;

		view.setLayoutParams(layoutParams);
		view.invalidate();
	}

	private static int[] getHighlightedAreaFromSteps(int[] steps, int nbSteps, int imageViewWidth, int highlightedAreaIndex) {
		int[] areaXDimens = new int[2];

		int imgX1 = DynazoomHelper.GRAPH_LEFT_MARGIN;
		int imgX2 = imageViewWidth - DynazoomHelper.GRAPH_RIGHT_MARGIN;
		int imgXDiff = imgX2 - imgX1;

		int hlAreaX1 = 0, hlAreaX2 = 0;

		if (highlightedAreaIndex == 1) {
			hlAreaX1 = 3;
			hlAreaX2 = steps[0] * imgXDiff / nbSteps + imgX1;
		} else if (highlightedAreaIndex == 2) {
			hlAreaX1 = steps[1] * imgXDiff / nbSteps + imgX1;
			hlAreaX2 = imageViewWidth - 13;
		}

		areaXDimens[0] = hlAreaX1;
		areaXDimens[1] = hlAreaX2;
		return areaXDimens;
	}

	public static long getFromPinPoint(MuninPlugin.Period period) {
		Calendar cal = Calendar.getInstance();
		switch (period) {
			case DAY:
				cal.add(Calendar.HOUR, -24);
				break;
			case WEEK:
				cal.add(Calendar.DAY_OF_MONTH, -7);
				break;
			case MONTH:
				cal.add(Calendar.DAY_OF_MONTH, -30);
				break;
			case YEAR:
				cal.add(Calendar.YEAR, -1);
				break;
		}

		return cal.getTime().getTime() / 1000;
	}

	public static long getToPinPoint() {
		return Calendar.getInstance().getTime().getTime() / 1000;
	}

	public static class DynazoomFetcher extends AsyncTask<Void, Integer, Void> {
		private MuninNode node;
		private MuninPlugin plugin;
		private long pinPoint1;
		private long pinPoint2;

		private ImageView imageView;
		private ProgressBar progressBar;

		private Context context;
		private String userAgent;

		private Bitmap bitmap;

		public DynazoomFetcher (MuninPlugin plugin, ImageView iv, ProgressBar progressBar, Context context, String userAgent,
		                        long pinPoint1, long pinPoint2) {
			super();
			this.node = plugin.getInstalledOn();
			this.plugin = plugin;
			this.pinPoint1 = pinPoint1;
			this.pinPoint2 = pinPoint2;

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

			String imgUrl = plugin.getHDImgUrl(pinPoint1, pinPoint2, true, graphsDimensions[0], graphsDimensions[1]);

			bitmap = Util.removeBitmapBorder(node.getParent().downloadBitmap(imgUrl, userAgent).getBitmap());
			bitmap = Util.dropShadow(bitmap);

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
