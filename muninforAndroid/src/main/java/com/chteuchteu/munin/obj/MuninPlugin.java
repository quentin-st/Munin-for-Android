package com.chteuchteu.munin.obj;

import android.content.Context;
import android.graphics.Bitmap;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DynazoomHelper;
import com.chteuchteu.munin.hlpr.Util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;

public class MuninPlugin implements ISearchable {
	private long 		id;
	private String 		name;
	private String 		fancyName;
	private MuninNode installedOn;
	private String 		category;
	private AlertState 	state;
	private String 		pluginPageUrl;
	private Util.SpecialBool isDocumentationAvailable;
	
	public MuninPlugin () {
		this.name = "unknown";
		this.state = AlertState.UNDEFINED;
		this.category = "";
		this.pluginPageUrl = "";
		this.isDocumentationAvailable = Util.SpecialBool.UNKNOWN;
	}
	public MuninPlugin (String name, MuninNode node) {
		this.name = name;
		this.installedOn = node;
		this.state = AlertState.UNDEFINED;
		this.category = "";
		this.pluginPageUrl = "";
		this.isDocumentationAvailable = Util.SpecialBool.UNKNOWN;
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
	
	public void setInstalledOn(MuninNode s) { this.installedOn = s; }
	public MuninNode getInstalledOn() { return this.installedOn; }
	
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

	public String getHDImgUrl(Period period, boolean forceSize, int size_x, int size_y) {
		// From
		long pinPoint1 = DynazoomHelper.getFromPinPoint(period);

		// To = now
		long pinPoint2 = DynazoomHelper.getToPinPoint();

		return getHDImgUrl(pinPoint1, pinPoint2, forceSize, size_x, size_y);
	}

	public String getHDImgUrl(long pinPoint1, long pinPoint2, boolean forceSize, int size_x, int size_y) {
		String url = this.getInstalledOn().getHdGraphURL() + this.getName()
				+ "-pinpoint=" + pinPoint1 + "," + pinPoint2 + ".png";
		if (forceSize)
			url += "?size_x=" + size_x + "&size_y=" + size_y;

		return url;
	}

	/**
	 * Returns the HD graph URL usable on Chromecast
	 * {pinpoint1}, {pinpoint2}, {size_x} and {size_y} will be replaced at runtime
	 */
	public String getHDImgUrlWithPlaceholders() {
		return this.getInstalledOn().getHdGraphURL() + this.getName() + "-pinpoint={pinpoint1},{pinpoint2}.png?size_x={size_x}&size_y={size_y}";
	}
	
	public Bitmap getGraph(String url, String userAgent) {
		return this.installedOn.getParent().grabBitmap(url, userAgent).getBitmap();
	}
	
	
	public String getFieldsDescriptionHtml(String userAgent) {
		if (this.pluginPageUrl.equals(""))
			return null;
		
		String html = this.installedOn.getParent().grabUrl(this.pluginPageUrl, userAgent).html;
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

	public static MuninPlugin findFromMastersList(long pluginId, List<MuninMaster> masters) {
		for (MuninMaster master : masters) {
			for (MuninNode node : master.getChildren()) {
				for (MuninPlugin plugin : node.getPlugins()) {
					if (plugin.getId() == pluginId)
						return plugin;
				}
			}
		}
		return null;
	}

	public Util.SpecialBool isDocumentationAvailable() { return this.isDocumentationAvailable; }
	public void setDocumentationAvailability(boolean val) { this.isDocumentationAvailable = val ? Util.SpecialBool.TRUE : Util.SpecialBool.FALSE; }

    /* ISearchable */
    @Override
    public boolean matches(String expr) {
        return this.getName().toLowerCase().contains(expr)
                || this.getFancyName().toLowerCase().contains(expr);
    }

    @Override
    public String[] getSearchResult() {
        return new String[] {
                this.getFancyName(),
                this.getInstalledOn().getName()
        };
    }

    @Override
    public SearchResult.SearchResultType getSearchResultType() {
        return SearchResult.SearchResultType.PLUGIN;
    }
}
