package com.chteuchteu.munin.obj;

import android.graphics.Bitmap;

import com.chteuchteu.munin.MuninFoo;

public class MuninPlugin {
	private long id;
	private String name;
	private String fancyName;
	private MuninServer installedOn;
	private String category;
	private String state;
	public boolean isPersistant;
	
	public static String ALERTS_STATE_UNDEFINED = "undefined";
	public static String ALERTS_STATE_OK = "ok";
	public static String ALERTS_STATE_WARNING = "warning";
	public static String ALERTS_STATE_CRITICAL = "error";
	
	public MuninPlugin () {
		this.name = "unknown";
		this.state = MuninPlugin.ALERTS_STATE_UNDEFINED;
		this.isPersistant = false;
		this.category = "";
	}
	public MuninPlugin (String name, MuninServer server) {
		this.name = name;
		this.installedOn = server;
		this.state = MuninPlugin.ALERTS_STATE_UNDEFINED;
		this.category = "";
		this.isPersistant = false;
	}
	public MuninPlugin (String name, String fancyName, MuninServer installedOn, String category) {
		this.name = name;
		this.fancyName = fancyName;
		this.installedOn = installedOn;
		this.category = category;
		this.isPersistant = false;
	}
	
	public void importData(MuninPlugin source) {
		if (source != null) {
			this.name = source.name;
			this.fancyName = source.fancyName;
			this.installedOn = source.installedOn;
			this.state = source.state;
			this.category = source.category;
		}
	}
	
	@Override
	public String toString() {
		return this.getFancyName();
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
	
	public String getCategory() {
		if (category != null)
			return this.category;
		return "";
	}
	
	public String getFancyName() {
		return this.fancyName;
	}
	
	public MuninServer getInstalledOn() {
		return this.installedOn;
	}
	
	public String getImgUrl(String period) {
		return this.getInstalledOn().getGraphURL() + this.getName() + "-" + period + ".png";
	}
	
	public String getPluginUrl() {
		return this.getInstalledOn().getServerUrl() + this.getName() + ".html";
	}
	
	public Bitmap getGraph(String period, MuninServer server) {
		return MuninFoo.grabBitmap(this.installedOn, getImgUrl(period));
	}
	
	public Bitmap getGraph(String url) {
		return MuninFoo.grabBitmap(this.installedOn, url);
	}
	
	public String getState() {
		return this.state;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public void setFancyName(String fn) {
		this.fancyName = fn;
	}
	public MuninPlugin setInstalledOn(MuninServer s) {
		this.installedOn = s;
		return this;
	}
	public void setCategory(String c) {
		this.category = c;
	}
	public void setState(String st) {
		if (st.equals(MuninPlugin.ALERTS_STATE_CRITICAL) || st.equals(MuninPlugin.ALERTS_STATE_OK)
				|| st.equals(MuninPlugin.ALERTS_STATE_UNDEFINED) || st.equals(MuninPlugin.ALERTS_STATE_WARNING))
			this.state = st;
		else
			this.state = MuninPlugin.ALERTS_STATE_UNDEFINED;
	}
	public boolean equals(MuninPlugin p) {
		if (this.name.equals(p.name) && this.fancyName.equals(p.fancyName) && this.installedOn.equalsApprox(p.installedOn))
			return true;
		return false;
	}
	
	public boolean equalsApprox(MuninPlugin p) {
		if (this.name.equals(p.name) && this.fancyName.equals(p.fancyName))
			return true;
		return false;
		
	}
}