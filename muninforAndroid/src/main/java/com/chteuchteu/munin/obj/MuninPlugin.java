package com.chteuchteu.munin.obj;

import android.content.Context;
import android.graphics.Bitmap;

import com.chteuchteu.munin.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Calendar;

public class MuninPlugin {
	private long 		id;
	private String 		name;
	private String 		fancyName;
	private MuninServer installedOn;
	private String 		category;
	private AlertState 	state;
	private String 		pluginPageUrl;
	
	public MuninPlugin () {
		this.name = "unknown";
		this.state = AlertState.UNDEFINED;
		this.category = "";
		this.pluginPageUrl = "";
	}
	public MuninPlugin (String name, MuninServer server) {
		this.name = name;
		this.installedOn = server;
		this.state = AlertState.UNDEFINED;
		this.category = "";
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
		
		public String getLabel(Context context) {
			switch (this) {
				case DAY:	return context.getString(R.string.text47_1);
				case WEEK:	return context.getString(R.string.text47_2);
				case MONTH:	return context.getString(R.string.text47_3);
				case YEAR:	return context.getString(R.string.text47_4);
			}
			return "";
		}
	}
	
	public enum AlertState {
		UNDEFINED, OK, WARNING, CRITICAL
	}
	
	public void setId(long id) { this.id = id; }
	public long getId() { return this.id; }
	
	public void setName(String name) { this.name = name; }
	public String getName() { return this.name; }
	
	public void setCategory(String category) { this.category = category; }
	public String getCategory() { return this.category; }
	
	public void setFancyName(String fName) { this.fancyName = fName; }
	public String getFancyName() { return this.fancyName; }
	
	public void setInstalledOn(MuninServer s) { this.installedOn = s; }
	public MuninServer getInstalledOn() { return this.installedOn; }
	
	public boolean hasPluginPageUrl() {
		return this.pluginPageUrl != null && !this.pluginPageUrl.equals("");
	}
	public void setPluginPageUrl(String url) {
		this.pluginPageUrl = url!=null?url:"";
	}
	public String getPluginPageUrl() { return this.pluginPageUrl; }
	
	public void setState(AlertState s) { this.state = s; }
	public AlertState getState() { return this.state; }
	
	
	
	public String getImgUrl(String period) {
		return this.getInstalledOn().getGraphURL() + this.getName() + "-" + period + ".png";
	}
	
	public String getImgUrl(Period period) {
		return this.getInstalledOn().getGraphURL() + this.getName() + "-" + period + ".png";
	}
	
	public String getHDImgUrl(Period period) {
		return getHDImgUrl(period, false, 0, 0);
	}
	
	public String getHDImgUrl(Period period,
			boolean forceSize, int size_x, int size_y) {
		// From
		Calendar cal = Calendar.getInstance();
		switch (period) {
			case DAY:
				cal.add(Calendar.HOUR, -24);
				break;
			case WEEK:
				cal.add(Calendar.DAY_OF_MONTH, -7);
				break;
			case MONTH:
				cal.add(Calendar.DAY_OF_MONTH, -30);
				break;
			case YEAR:
				cal.add(Calendar.YEAR, -1);
				break;
		}
		long pinPoint1 = cal.getTime().getTime() / 1000;
		long pinPoint2 = Calendar.getInstance().getTime().getTime() / 1000;
		
		
		String url = this.getInstalledOn().getGraphURL() + this.getName()
				+ "-pinpoint=" + pinPoint1 + "," + pinPoint2 + ".png";
		if (forceSize)
			url += "?size_x=" + size_x + "&size_y=" + size_y;
		
		return url;
	}
	
	public Bitmap getGraph(String url) {
		return this.installedOn.getParent().grabBitmap(url);
	}
	
	
	public String getFieldsDescriptionHtml() {
		if (this.pluginPageUrl.equals(""))
			return null;
		
		String html = this.installedOn.getParent().grabUrl(this.pluginPageUrl).html;
		if (html.equals(""))
			return null;
		
		try {
			// Get <table id="legend">
			Document doc = Jsoup.parse(html, this.pluginPageUrl);
			Element table = doc.select("table#legend").first();
			table.select("a").unwrap();
			
			return table.outerHtml();
		} catch (Exception ex) { return ""; }
	}
	
	public boolean equals(MuninPlugin p) {
		return p != null
				&& this.name.equals(p.name)
				&& this.fancyName.equals(p.fancyName)
				&& this.installedOn.equalsApprox(p.installedOn);
	}
	
	public boolean equalsApprox(MuninPlugin p) {
		return p != null &&
				this.name.equals(p.name)
				&& this.fancyName.equals(p.fancyName);
	}
	
	public int getIndex() {
		if (this.installedOn == null)
			return -1;
		
		int i=0;
		for (MuninPlugin plugin : this.installedOn.getPlugins()) {
			if (plugin.equals(this))
				return i;
			i++;
		}
		
		return -1;
	}
}