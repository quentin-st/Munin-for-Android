package com.chteuchteu.munin.obj;

import java.util.ArrayList;
import java.util.List;

import com.chteuchteu.munin.MuninFoo;

public class MuninMaster {
	private long id;
	private String name;
	private String url;
	private MuninMaster parent;
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
	
	/*/**
	 * Allows to determine the MuninMaster type, since it can be :
	 * 		- ROOT : 	0 parent	1-* MuninMaster children
	 * 		- BRANCH : 	1 parent	1-* MuninMaster children
	 * 		- LEAF :	1 parent	1-* MuninServer children
	 */
	/*public enum MasterType { ROOT, BRANCH, LEAF };*/
	
	@Override
	public String toString() {
		return this.getName();
	}
	
	public long getId() {
		return this.id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getShortName() {
		if (this.name.length() > 12)
			return this.name.toString().substring(0, 11) + "...";
		else
			return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getUrl() {
		return this.url;
	}
	
	public void setParent(MuninMaster p) {
		this.parent = p;
	}
	
	public MuninMaster getParent() {
		return this.parent;
	}
	
	public List<MuninServer> getChildren() {
		return this.children;
	}
	
	public void setChildren(List<MuninServer> l) {
		this.children = l;
	}
	
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
	
	public boolean isTopParent() {
		return this.parent == null;
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
	
	public List<MuninMaster> getMastersChildren(MuninFoo f) {
		List<MuninMaster> l = new ArrayList<MuninMaster>();
		for (MuninMaster m : f.masters) {
			if (m.getParent() != null && m.getParent().getId() == this.id)
				l.add(m);
		}
		return l;
	}
	
	/*public MasterType getMasterType(MuninFoo f) {
		int nbServersChildren = getServersChildren(f).size();
		if (nbServersChildren > 0)	return MasterType.LEAF;
		int nbMastersChildren = getMastersChildren(f).size();
		
	}*/
}