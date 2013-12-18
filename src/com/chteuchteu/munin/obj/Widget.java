package com.chteuchteu.munin.obj;



public class Widget {
	private long id;
	private String period;
	private boolean wifiOnly;
	private MuninPlugin	plugin;
	private int	 widgetId;
	public boolean isPersistant;

	public Widget() {
		this.period = "day";
		this.wifiOnly = false;
		this.isPersistant = false;
	}

	public Widget (String p, boolean w, MuninPlugin pl) {
		this.setPeriod(p);
		this.setWifiOnly(w);
		this.setPlugin(pl);
		this.isPersistant = false;
	}

	public Widget(String period, boolean wifiOnly, MuninPlugin plugin, int widgetId) {
		this.period = period;
		this.wifiOnly = wifiOnly;
		this.plugin = plugin;
		this.widgetId = widgetId;
		this.isPersistant = false;
	}

	public long getId() {
		return this.id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public boolean isWifiOnly() {
		return wifiOnly;
	}
	
	public void setWifiOnly(int b) {
		if (b == 1)
			this.wifiOnly = true;
		else
			this.wifiOnly = false;
	}

	public void setWifiOnly(boolean wifiOnly) {
		this.wifiOnly = wifiOnly;
	}

	public MuninPlugin getPlugin() {
		return plugin;
	}

	public void setPlugin(MuninPlugin plugin) {
		this.plugin = plugin;
	}

	public int getWidgetId() {
		return widgetId;
	}

	public void setWidgetId(int widgetId) {
		this.widgetId = widgetId;
	}
}