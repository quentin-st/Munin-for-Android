package com.chteuchteu.munin;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;


@Table(name = "MuninWidgets")
public class MuninWidget extends Model {
	// champs
								@Column(name = "server")
	private MuninServer server;
								@Column(name = "period")
	private String		period;
								@Column(name = "wifiOnly")
	private boolean		wifiOnly;
								@Column(name = "plugin")
	private MuninPlugin	plugin;
								@Column(name = "widgetId")
	private int			widgetId;
	
	// constructeur:
	public MuninWidget() {
		super();
		this.period = "day";
		this.wifiOnly = false;
	}

	public MuninWidget (MuninServer s, String p, boolean w, MuninPlugin pl) {
		super();
		this.setServer(s);
		this.setPeriod(p);
		this.setWifiOnly(w);
		this.setPlugin(pl);
	}
	
	public MuninWidget(MuninServer server, String period, boolean wifiOnly, MuninPlugin plugin, int widgetId) {
		super();
		this.server = server;
		this.period = period;
		this.wifiOnly = wifiOnly;
		this.plugin = plugin;
		this.widgetId = widgetId;
	}

	public MuninServer getServer() {
		return server;
	}

	public void setServer(MuninServer server) {
		this.server = server;
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