package com.chteuchteu.munin.obj;

import android.graphics.Bitmap;
import android.util.Log;

import com.chteuchteu.munin.MuninFoo;

public class MuninPlugin {
	private long id;
	private String name;
	private String fancyName;
	private MuninServer installedOn;
	private String category;
	private AlertState state;
	private String pluginPageUrl;
	public boolean isPersistant;
	
	public MuninPlugin () {
		this.name = "unknown";
		this.state = AlertState.UNDEFINED;
		this.isPersistant = false;
		this.category = "";
		this.pluginPageUrl = "";
	}
	public MuninPlugin (String name, MuninServer server) {
		this.name = name;
		this.installedOn = server;
		this.state = AlertState.UNDEFINED;
		this.category = "";
		this.isPersistant = false;
		this.pluginPageUrl = "";
	}
	public MuninPlugin (String name, String fancyName, MuninServer installedOn, String category) {
		this.name = name;
		this.fancyName = fancyName;
		this.installedOn = installedOn;
		this.category = category;
		this.isPersistant = false;
		this.pluginPageUrl = "";
	}
	
	public enum Period {
		DAY("day"), WEEK("week"), MONTH("month"), YEAR("year");
		
		private String name = "";
		Period(String p) { this.name = p; }
		
		public String toString() { return name; }
		
		public static Period get(String name) {
			for (Period p : Period.values())
				if (p.name.equals(name))
					return p;
			return DAY;
		}
	}
	
	public enum AlertState {
		UNDEFINED, OK, WARNING, CRITICAL
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
	
	public String getImgUrl(Period period) {
		return this.getInstalledOn().getGraphURL() + this.getName() + "-" + period.name() + ".png";
	}
	
	public String getPluginUrl() {
		return this.getInstalledOn().getServerUrl() + this.getName() + ".html";
	}
	
	public Bitmap getGraph(Period period) {
		return MuninFoo.grabBitmap(this.installedOn, getImgUrl(period));
	}
	
	public Bitmap getGraph(String url) {
		return MuninFoo.grabBitmap(this.installedOn, url);
	}
	
	public void setPluginPageUrl(String url) {
		this.pluginPageUrl = url;
	}
	
	public String getPluginPageUrl() {
		return this.pluginPageUrl;
	}
	
	public AlertState getState() {
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
	public void setState(AlertState s) {
		this.state = s;
	}
	
	// TODO
	public String getGraphInformationHtml() {
		String html = "";
		// Download graph page html code
		Log.v("", "img url : " + this.getImgUrl(Period.DAY));
		// Get table (id="legend")
		
		// Do things
		
		return html;
	}
	
	public boolean equals(MuninPlugin p) {
		if (p == null)
			return false;
		if (this.name.equals(p.name) && this.fancyName.equals(p.fancyName) && this.installedOn.equalsApprox(p.installedOn))
			return true;
		return false;
	}
	
	public boolean equalsApprox(MuninPlugin p) {
		if (p == null)
			return false;
		if (this.name.equals(p.name) && this.fancyName.equals(p.fancyName))
			return true;
		return false;
	}
}