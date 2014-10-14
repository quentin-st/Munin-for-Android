package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.util.Log;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;

public class SQLite {
	private MuninFoo muninFoo;
	public DatabaseHelper dbHlpr;
	
	public SQLite(Context c, MuninFoo m) {
		this.muninFoo = m;
		this.dbHlpr = new DatabaseHelper(c);
	}
	
	public MuninServer getBDDInstance(MuninServer s) {
		for (MuninServer serv : dbHlpr.getServers(muninFoo.masters)) {
			if (s.equalsApprox(serv))
				return serv;
		}
		return s;
	}
	
	public void insertMuninMaster(MuninMaster master) {
		// Update servers positions
		for (MuninServer server : master.getChildren())
			server.setPosition(master.getChildren().indexOf(server));
		
		// Insert master
		dbHlpr.insertMuninMaster(master);
		// Insert servers
		for (MuninServer server : master.getChildren()) {
			dbHlpr.insertMuninServer(server);
			for (MuninPlugin plugin : server.getPlugins())
				dbHlpr.insertMuninPlugin(plugin);
		}
	}
	
	public void saveLabels() {
		// Simpler way of doing it!
		dbHlpr.deleteLabels();
		dbHlpr.deleteLabelsRelations();
		for (Label l : muninFoo.labels) {
			dbHlpr.insertLabel(l);
			
			for (MuninPlugin p : l.plugins)
				dbHlpr.insertLabelRelation(p, l);
		}
	}
	
	public void saveGridItemRelations(Grid g) {
		// Simplest way of doing it ;)
		dbHlpr.deleteGridItemRelations(g);
		for (GridItem i : g.items)
			dbHlpr.insertGridItemRelation(i);
	}
	
	
	
	public void logServers() {
		Log.v("SQLite_old", "==========================================");
		if (dbHlpr.getServers(muninFoo.masters).size() > 0) {
			for (MuninServer s : dbHlpr.getServers(muninFoo.masters)) {
				Log.v("SQLite_old", s.getName() + "\t  " + s.getServerUrl());
			}
		} else
			Log.v("SQLite_old", "No servers in the database.");
		Log.v("SQLite_old", "==========================================");
	}
	/*public void logPlugins() {
		Log.v("SQLite_old", "==========================================");
		if (getPlugins().size() > 0) {
			for (MuninPlugin p : getPlugins()) {
				Log.v("SQLite_old", p.getName() + "\t  " + p.getFancyName());
			}
		} else
			Log.v("SQLite_old", "No plugins in the database.");
		Log.v("SQLite_old", "==========================================");
	}*/
	private void logLine(int nb) {
		if (nb == 0)
			logLine(88);
		else {
			String s = "";
			for (int i=0; i<nb; i++)
				s += "=";
			log(s);
		}
	}
	private void log(String txt) {
		Log.v("", txt);
	}
	
	public void logMasters() {
		log("");
		logLine(60);
		for (MuninMaster m : this.muninFoo.masters) {
			log("[" + m.getName() + "]");
			for (MuninServer s : m.getChildren())
				log("  - " + s.getName());
		}
		logLine(60);
	}
}