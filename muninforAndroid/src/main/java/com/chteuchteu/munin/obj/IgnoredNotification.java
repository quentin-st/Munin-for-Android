package com.chteuchteu.munin.obj;

import java.util.Calendar;

public class IgnoredNotification {
	private String group;
	private String host;
	private String plugin;
	private Calendar until;

	public IgnoredNotification(String group, String host, String plugin, Calendar until) {
		this.group = group;
		this.host = host;
		this.plugin = plugin;
		this.until = until;
	}

	public IgnoredNotification(String group, String host, String plugin, long untilMillis) {
		this.group = group;
		this.host = host;
		this.plugin = plugin;
		if (untilMillis == 0)
			this.until = null;
		else {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(untilMillis);
			this.until = calendar;
		}
	}

	public String getGroup() { return this.group; }
	public String getHost() { return this.host; }
	public String getPlugin() { return this.plugin; }
	public Calendar getUntil() { return this.until; }
}
