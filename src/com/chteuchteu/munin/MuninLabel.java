package com.chteuchteu.munin;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class MuninLabel {
	private String name;
	
	public List<MuninPlugin> plugins;

	public MuninLabel(String name) {
		this.name = name;
		this.plugins = new ArrayList<MuninPlugin>();
	}
	
	// Returns a List of List of MuninPlugin, sorted by server.
	// To be displayed in Activity_LabelsPluginSelection
	public List<List<MuninPlugin>> getPluginsSortedByServer() {
		List<List<MuninPlugin>> l = new ArrayList<List<MuninPlugin>>();
		// Copie de la liste pl (= tmp plugins)
		List<MuninPlugin> pl = new ArrayList<MuninPlugin>();
		for (MuninPlugin p : plugins) {
			MuninPlugin newP = new MuninPlugin();
			newP.importData(p);
			pl.add(newP);
		}
		
		while (pl.size() > 0) {
			Log.v("", "Début opération");
			for (MuninPlugin p : plugins)
				Log.v("", p.getName() + "\t" + p.getInstalledOn());
			Log.v("" ,"====================");
			MuninServer s = pl.get(0).getInstalledOn();
			List<MuninPlugin> ltemp = new ArrayList<MuninPlugin>();
			ltemp.add(pl.get(0));
			List<Integer> posToRemove = new ArrayList<Integer>();
			for (int i=1; i<pl.size(); i++) {
				if (pl.get(i).getInstalledOn().equalsApprox(s)) {
					ltemp.add(pl.get(i));
					posToRemove.add(Integer.valueOf(i));
				}
			}
			for (Integer i : posToRemove)
				pl.remove(i.intValue());
			pl.remove(0);
			if (ltemp.size() > 0)
				l.add(ltemp);
		}
		return l;
	}
	
	public void addPlugin(MuninPlugin p) {
		plugins.add(p);
	}
	
	public void removePlugin(MuninPlugin p) {
		List<MuninPlugin> newList = new ArrayList<MuninPlugin>();
		for (MuninPlugin pl : plugins) {
			if (!pl.equals(p))
				newList.add(pl);
		}
		plugins = newList;
	}
	
	public boolean contains(MuninPlugin p) {
		for (MuninPlugin mp : plugins) {
			if (mp.equals(p))
				return true;
		}
		return false;
	}
	
	public boolean equals(MuninLabel l) {
		if (this.name.equals(l.getName()))
			return true;
		return false;
	}
	
	public void setPlugins(List<MuninPlugin> l) {
		this.plugins = l;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return this.name;
	}
}