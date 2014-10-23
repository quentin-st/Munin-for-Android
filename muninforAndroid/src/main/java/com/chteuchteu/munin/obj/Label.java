package com.chteuchteu.munin.obj;

import java.util.ArrayList;
import java.util.List;

import com.chteuchteu.munin.MuninFoo;

public class Label {
	private long id;
	private String name;
	
	public List<MuninPlugin> plugins;
	
	public Label() {
		this.plugins = new ArrayList<MuninPlugin>();
	}
	
	public Label(String name) {
		this.name = name;
		this.plugins = new ArrayList<MuninPlugin>();
	}
	
	public void setId(long id) { this.id = id; }
	public long getId() { return this.id; }
	
	
	// Returns a List of List of MuninPlugin, sorted by server.
	// To be displayed in Activity_Label
	// The loop is made from MuninFoo's servers list (more easy than previous way)
	//		, and made according to servers sorting positions
	public List<List<MuninPlugin>> getPluginsSortedByServer(MuninFoo f) {
		List<List<MuninPlugin>> l = new ArrayList<List<MuninPlugin>>();
		
		List<MuninPlugin> curList;
		for (MuninServer s : f.getOrderedServers()) {
			curList = new ArrayList<MuninPlugin>();
			
			for (MuninPlugin p : plugins) {
				if (p.getInstalledOn() != null &&
						p.getInstalledOn().equalsApprox(s))
					curList.add(p);
			}
			
			if (curList.size() > 0)
				l.add(curList);
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
		if (plugins == null)
			return false;
		
		for (MuninPlugin mp : plugins) {
			if (mp != null && mp.equals(p))
				return true;
		}
		return false;
	}
	
	public boolean equals(Label l) {
		return (this.name.equals(l.getName()));
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