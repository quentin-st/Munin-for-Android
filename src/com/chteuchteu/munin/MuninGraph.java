package com.chteuchteu.munin;

import android.graphics.Bitmap;


public class MuninGraph
{
	public static Bitmap bitmap;
	
	private MuninPlugin plugin;
	private MuninServer server;
	
	public MuninGraph (MuninPlugin plugin, MuninServer server) {
		this.plugin = plugin;
		this.server = server;
	}
	
	public String getImgUrl(String period) {
		return this.plugin.getInstalledOn().getGraphURL() + this.plugin.getName() + "-" + period + ".png";
	}
	
	public Bitmap getGraph (String period, MuninServer server) {
		this.server = server;
		return getGraph(getImgUrl(period));
	}
	
	public Bitmap getGraph(String url) {
		return MuninFoo.grabBitmap(this.server, url);
	}
}