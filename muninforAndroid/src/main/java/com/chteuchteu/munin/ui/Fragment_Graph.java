package com.chteuchteu.munin.ui;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.async.BitmapFetcher;
import com.chteuchteu.munin.obj.MuninPlugin;

public class Fragment_Graph extends Fragment {
	public static final String KEY_PLUGIN_POS = "pluginPosition";
	public static final String KEY_PERIOD = "period";

	private Activity_GraphView activity;
	private Context context;

	public static Fragment_Graph init(int position, MuninPlugin.Period period) {
		Fragment_Graph newFragment = new Fragment_Graph();
		Bundle arguments = new Bundle();
		arguments.putInt(KEY_PLUGIN_POS, position);
		arguments.putString(KEY_PERIOD, period.name());
		newFragment.setArguments(arguments);
		return newFragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = (Activity_GraphView) activity;
		this.context = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.fragment_graph, container, false);

		Bundle args = getArguments();
		int position = args.getInt(KEY_PLUGIN_POS, -1);
		view.findViewById(R.id.error).setVisibility(View.GONE);

		if (activity.loadGraphs) {
			ImageView imageView = (ImageView) view.findViewById(R.id.tiv);
			ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressbar);

			if (activity.isBitmapNull(position)) {
				//                                                                                  Avoid serial execution
				new BitmapFetcher(activity, imageView, progressBar, view, position, context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
			else {
				imageView.setImageBitmap(activity.getBitmap(position));
				progressBar.setVisibility(View.GONE);
			}
		}

		return view;
	}

	public void refresh() {
		// TODO
	}
}
