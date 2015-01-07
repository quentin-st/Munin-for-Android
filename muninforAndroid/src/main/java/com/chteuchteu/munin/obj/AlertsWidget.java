package com.chteuchteu.munin.obj;


import java.util.ArrayList;
import java.util.List;

public class AlertsWidget {
	private long id;
	private boolean wifiOnly;
	private List<MuninServer> servers;
	private int widgetId;

	public AlertsWidget() {
		this.id = -1;
		this.wifiOnly = false;
		this.servers = new ArrayList<>();
		this.widgetId = -1;
	}

	public long getId() { return id; }
	public void setId(long id) { this.id = id; }

	public boolean isWifiOnly() { return wifiOnly; }
	public void setWifiOnly(int b) { this.wifiOnly = b == 1; }

	public List<MuninServer> getServers() { return servers; }
	public void setServers(List<MuninServer> servers) { this.servers = servers; }

	public int getWidgetId() { return widgetId; }
	public void setWidgetId(int widgetId) { this.widgetId = widgetId; }
}
