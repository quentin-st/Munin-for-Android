package com.chteuchteu.munin.obj;

import java.util.ArrayList;
import java.util.List;

import com.chteuchteu.munin.MuninFoo;

public class MuninMaster {
	private long id;
	private String name;
	private String url;
	private List<MuninServer> children;
	
	public boolean defaultMaster;
	
	public MuninMaster () {
		this.name = "default";
		this.id = -1;
		this.url = "";
		this.defaultMaster = false;
		this.children = new ArrayList<MuninServer>();
	}
	public MuninMaster (String name) {
		this.name = name;
		this.id = -1;
		this.url = "";
		this.defaultMaster = false;
		this.children = new ArrayList<MuninServer>();
	}
	
	public void rebuildChildren(MuninFoo f) {
		this.children = new ArrayList<MuninServer>();
		for (MuninServer s : f.getServers()) {
			if (s.getParent().getId() == this.id)
				this.children.add(s);
		}
		if (this.children.size() == 0)
			deleteSelf(f);
	}
	
	public boolean manualRebuildChildren(MuninFoo f) {
		this.children = new ArrayList<MuninServer>();
		for (MuninServer s : f.getServers()) {
			if (s.getParent().getId() == this.id)
				this.children.add(s);
		}
		return this.children.size() == 0; // true = to be deleted
	}
	
	public void deleteSelf(MuninFoo f) {
		// If there's no more server under this, delete self.
		f.deleteMuninMaster(this);
	}
	
	public void setId(long id) { this.id = id; }
	public long getId() { return this.id; }
	
	public void setName(String name) { this.name = name; }
	public String getName() { return this.name; }
	
	public void setUrl(String url) { this.url = url; }
	public String getUrl() { return this.url; }
	
	public void setChildren(List<MuninServer> l) { this.children = l; }
	public List<MuninServer> getChildren() { return this.children; }
	public void addChild(MuninServer s) {
		this.children.add(s);
	}
	public boolean deleteChild(MuninServer s) {
		return this.children.remove(s);
	}
	public MuninServer getChildAt(int i) {
		if (i >= 0 && i < this.children.size())
			return this.children.get(i);
		return null;
	}
	
	public boolean equalsApprox(MuninMaster p) {
		if (p == null)
			return false;
		if (this.name.equals(p.name))
			return true;
		return false;
	}
	
	public List<MuninServer> getServersChildren(MuninFoo f) {
		if (this.getName().equals("default"))	return f.getServers();
		
		List<MuninServer> l = new ArrayList<MuninServer>();
		
		for (MuninServer s : f.getServers()) {
			if (s.getParent() != null && s.getParent().getId() == this.id)
				l.add(s);
		}
		
		return l;
	}
	
	public MuninServer getServerFromFlatPosition(int position) {
		// si pos -> 0 1 4 8 9 11
		// gSFFP(2) -> 4 (!= null)
		if (position >= 0 && position < getOrderedServers().size())
			return getOrderedServers().get(position);
		return null;
	}
	
	public MuninServer getServerFromPosition(int position) {
		for (MuninServer s : this.children) {
			if (s != null && s.getPosition() == position)
				return s;
		}
		return null;
	}
	
	public MuninMaster copy() {
		MuninMaster m = new MuninMaster(this.name);
		m.setId(this.id);
		m.setUrl(this.getUrl());
		m.setChildren(this.children);
		return m;
	}
	
	public List<MuninServer> getOrderedServers() {
		List<MuninServer> l = new ArrayList<MuninServer>();
		int pos = 0;
		int remainingServers = this.children.size();
		
		int maxPos = 0;
		for (MuninServer s : this.children) {
			if (s.getPosition() > maxPos)
				maxPos = s.getPosition();
		}
		
		while(remainingServers > 0 && pos <= maxPos) {
			if (getServerFromPosition(pos) != null) {
				l.add(getServerFromPosition(pos));
				remainingServers--;
			}
			pos++;
		}
		
		return l;
	}
}