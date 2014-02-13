package com.chteuchteu.munin.hlpr;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;

/*
 * XMLHelper
 * Created 1.6
 * Implemented 1.7 (?)
 * Used for XML Import / Export
 * Important : we could export the database as-is instead of using XML
 */

public class XMLHelper {
	MuninFoo m;
	
	public XMLHelper(MuninFoo m) {
		this.m = m;
	}
	
	public String export() {
		String xml = "";
		
		String servers = "";
		servers += "<servers>";
		for (MuninServer s : m.getServers()) {
			servers += "<server";
			servers += attr("name", s.getName());
			servers += attr("serverUrl", s.getServerUrl());
			if (s.isAuthNeeded()) {
				servers += attr("authLogin", s.getAuthLogin());
				servers += attr("authPassword", s.getAuthPassword());
			}
			servers += attr("graphUrl", s.getSSL() + "");
			servers += attr("position", s.getPosition() + "");
			servers += ">";
			
			for (MuninPlugin p : s.getPlugins()) {
				servers = servers + "<plugin " + attr("name", p.getName()) + attr("fancyName", p.getFancyName()) + " />";
			}
			
			servers += "</server>";
		}
		
		xml += servers;
		
		return xml;
	}
	public String attr(String key, String value) {
		return " " + key + "=\"" + value + "\"";
	}
}