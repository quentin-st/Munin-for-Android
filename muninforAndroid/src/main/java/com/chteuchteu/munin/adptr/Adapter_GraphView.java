package com.chteuchteu.munin.adptr;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.ui.Activity_GraphView;
import com.chteuchteu.munin.ui.Fragment_Graph;

import java.util.HashMap;

public class Adapter_GraphView extends FragmentStatePagerAdapter {
	private MuninFoo muninFoo;
	private HashMap<Integer, Fragment_Graph> fragments;
	private Activity_GraphView activity;
	private int count;

	public Adapter_GraphView(FragmentManager fragmentManager, Activity_GraphView activity, MuninFoo muninFoo, int count) {
		super(fragmentManager);
		this.activity = activity;
		this.muninFoo = muninFoo;
		this.count = count;
		this.fragments = new HashMap<>();
	}
	
	@Override
	public int getCount() {
		return count;
	}
	
	@Override
	public Fragment getItem(int position) {
		if (fragments.containsKey(position))
			return fragments.get(position);

		Fragment_Graph newFragment = Fragment_Graph.init(position, activity.load_period);
		fragments.put(position, newFragment);
		return newFragment;
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		if (position < 0)
			return "";

		if (activity.viewFlowMode == Activity_GraphView.VIEWFLOWMODE_GRAPHS) {
			if (position > muninFoo.getCurrentNode().getPlugins().size())
				return "";

			return muninFoo.getCurrentNode().getPlugin(position).getFancyName();
		} else {
			if (position > activity.label.getPlugins().size())
				return "";

			return activity.label.getPlugins().get(position).getFancyName();
		}
	}

	public void refreshAll() {
		for (Fragment_Graph fragment : fragments.values())
			fragment.refresh();
	}
}
