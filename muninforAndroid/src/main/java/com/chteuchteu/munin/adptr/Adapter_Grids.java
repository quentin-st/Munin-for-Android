package com.chteuchteu.munin.adptr;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.ui.Fragment_Grid;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Adapter_Grids extends FragmentStatePagerAdapter {
	private List<Grid> grids;
	private HashMap<Integer, Fragment_Grid> fragments;
	private int count;

	public Adapter_Grids(FragmentManager fragmentManager, List<Grid> grids) {
		super(fragmentManager);
		this.grids = grids;
		this.count = grids.size();
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

		// Init fragment
		Fragment_Grid fragment = new Fragment_Grid();
		// Pass the gridId
		Bundle bundle = new Bundle();
		long gridId = this.grids.get(position).getId();
		bundle.putLong(Fragment_Grid.ARG_GRIDID, gridId);
		fragment.setArguments(bundle);
		this.fragments.put(position, fragment);

		return fragment;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return this.grids.get(position).getName();
	}

	public Collection<Fragment_Grid> getAll() {
		return this.fragments.values();
	}
}
