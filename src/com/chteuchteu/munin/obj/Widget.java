package com.chteuchteu.munin.obj;



public class Widget {
	private long id;
	private String period;
	private boolean wifiOnly;
	private boolean hideServerName;
	private MuninPlugin	plugin;
	private int	 widgetId;
	public boolean isPersistant;
	
	public Widget() {
		this.period = "day";
		this.wifiOnly = false;
		this.isPersistant = false;
		this.hideServerName = false;
	}
	
	public Widget (String p, boolean w, MuninPlugin pl) {
		this.setPeriod(p);
		this.setWifiOnly(w);
		this.setPlugin(pl);
		this.isPersistant = false;
		this.hideServerName = false;
	}
	
	public Widget(String period, boolean wifiOnly, boolean hideServerTitle, MuninPlugin plugin, int widgetId) {
		this.period = period;
		this.wifiOnly = wifiOnly;
		this.plugin = plugin;
		this.widgetId = widgetId;
		this.isPersistant = false;
		this.hideServerName = hideServerTitle;
	}
	
	public long getId() { return this.id; }
	public void setId(long id) { this.id = id; }
	
	public String getPeriod() { return period; }
	public void setPeriod(String period) { this.period = period; }
	
	public boolean isWifiOnly() { return wifiOnly; }
	public void setWifiOnly(int b) { this.wifiOnly = b == 1; }
	public void setWifiOnly(boolean wifiOnly) { this.wifiOnly = wifiOnly; }
	
	public boolean getHideServerName() { return this.hideServerName; }
	public void setHideServerName(int b) { this.hideServerName = b == 1; }
	public void setHideServerName(boolean val) { this.hideServerName = val; }
	
	public MuninPlugin getPlugin() { return plugin; }
	public void setPlugin(MuninPlugin plugin) { this.plugin = plugin; }
	
	public int getWidgetId() { return widgetId; }
	public void setWidgetId(int widgetId) { this.widgetId = widgetId; }
}