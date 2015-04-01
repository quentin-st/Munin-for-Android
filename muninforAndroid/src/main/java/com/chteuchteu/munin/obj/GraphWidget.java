package com.chteuchteu.munin.obj;


public class GraphWidget {
	private long id;
	private String period;
	private boolean wifiOnly;
	private boolean hideNodeName;
	private MuninPlugin plugin;
	private int widgetId;
	
	public GraphWidget() {
		this.period = "day";
		this.wifiOnly = false;
		this.hideNodeName = false;
	}

	public void setId(long id) { this.id = id; }
	
	public String getPeriod() { return period; }
	public void setPeriod(String period) { this.period = period; }
	
	public boolean isWifiOnly() { return wifiOnly; }
	public void setWifiOnly(int b) { this.wifiOnly = b == 1; }
	public void setWifiOnly(boolean wifiOnly) { this.wifiOnly = wifiOnly; }
	
	public boolean getHideNodeName() { return this.hideNodeName; }
	public void setHideNodeName(int b) { this.hideNodeName = b == 1; }
	public void setHideNodeName(boolean val) { this.hideNodeName = val; }
	
	public MuninPlugin getPlugin() { return plugin; }
	public void setPlugin(MuninPlugin plugin) { this.plugin = plugin; }
	
	public int getWidgetId() { return widgetId; }
	public void setWidgetId(int widgetId) { this.widgetId = widgetId; }
}