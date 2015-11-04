package com.chteuchteu.munin.obj;


import java.util.ArrayList;
import java.util.List;

public class AlertsWidget extends Entity {
	private boolean wifiOnly;
	private List<MuninNode> nodes;
	private int widgetId;

	public AlertsWidget() {
		super();
		this.wifiOnly = false;
		this.nodes = new ArrayList<>();
		this.widgetId = -1;
	}

	public boolean isWifiOnly() { return wifiOnly; }
	public void setWifiOnly(int b) { this.wifiOnly = b == 1; }

	public List<MuninNode> getNodes() { return nodes; }
	public void setNodes(List<MuninNode> nodes) { this.nodes = nodes; }

	public int getWidgetId() { return widgetId; }
	public void setWidgetId(int widgetId) { this.widgetId = widgetId; }
}
