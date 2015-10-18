package com.chteuchteu.munin.adptr;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.chteuchteu.munin.ui.Fragment_Notifications_Pull;
import com.chteuchteu.munin.ui.Fragment_Notifications_Push;
import com.chteuchteu.munin.ui.INotificationsFragment;

import java.util.HashMap;

public class Adapter_Notifications extends FragmentStatePagerAdapter {
	public static final int FRAGMENT_PUSH = 0;
	public static final int FRAGMENT_PULL = 1;

	private HashMap<Integer, INotificationsFragment> fragments;

	public Adapter_Notifications(FragmentManager fragmentManager) {
		super(fragmentManager);
		this.fragments = new HashMap<>();
		this.fragments.put(FRAGMENT_PUSH, new Fragment_Notifications_Push());
		this.fragments.put(FRAGMENT_PULL, new Fragment_Notifications_Pull());
	}

	@Override
	public Fragment getItem(int position) {
		return (Fragment) this.fragments.get(position);
	}

	@Override
	public int getCount() {
		return 2;
	}

	public HashMap<Integer, INotificationsFragment> getFragments() { return this.fragments; }
}
