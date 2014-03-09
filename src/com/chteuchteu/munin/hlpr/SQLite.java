package com.chteuchteu.munin.hlpr;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.Widget;

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
	
	public boolean migrateDatabase(Context c) {
		try {
			// Get data
			DatabaseHelper_old dbH_old = new DatabaseHelper_old(c);
			List<MuninServer> servers = dbH_old.getAllMuninServers();
			List<Widget> widgets = dbH_old.getAllWidgets();
			
			// Servers
			for (MuninServer s : servers) {
				long id = dbHlpr.insertMuninServer(s);
				s.setId(id);
				
				// Plugins
				List<MuninPlugin> plugins = s.getPlugins();
				for (MuninPlugin p : plugins) {
					p.setInstalledOn(s);
					p.setId(dbHlpr.insertMuninPlugin(p));
				}
			}
			
			// Widgets
			for (Widget w : widgets) {
				w.setId(dbHlpr.insertWidget(w));
			}
			
			Util.setPref(c, "db_migrated", "true");
			Util.setPref(c, "db_migrated_failed", "false");
			
			// TODO delete database
			// Drop the (data)bas(e)s
			return true;
		} catch (Exception ex) {
			Util.setPref(c, "db_migrated", "true");
			Util.setPref(c, "db_migrated_failed", "true");
			// Data half-migrated : delete all
			try {
				muninFoo.sqlite.dbHlpr.deleteWidgets();
				muninFoo.sqlite.dbHlpr.deleteLabels();
				muninFoo.sqlite.dbHlpr.deleteLabelsRelations();
				muninFoo.sqlite.dbHlpr.deleteMuninPlugins();
				muninFoo.sqlite.dbHlpr.deleteMuninServers();
			} catch (Exception ex2) { }
			return false;
		}
	}
	
	public void saveServers() {
		// Vérification des positions
		for (int i=0; i<muninFoo.getOrderedServers().size(); i++) {
			muninFoo.getOrderedServers().get(i).setPosition(i);
		}
		
		// Suppression des serveurs à supprimer
		List<MuninServer> toBeDeleted = new ArrayList<MuninServer>();
		List<MuninServer> localObj = muninFoo.getServers();
		for (MuninServer dbS : dbHlpr.getServers(muninFoo.masters)) {
			int nb = 0;
			for (MuninServer s : localObj) {
				if (dbS.equalsApprox(s)) {
					nb++; break;
				}
			}
			if (nb == 0)
				toBeDeleted.add(dbS);
		}
		for (MuninServer s : toBeDeleted)
			dbHlpr.deleteServer(s);
		
		List<MuninServer> bdd = dbHlpr.getServers(muninFoo.masters);
		for (MuninServer s : muninFoo.getServers()) {
			dbHlpr.saveMuninMaster(s.master);
			
			MuninServer bddInstance = null;
			
			// Getting BDD Instance
			for (MuninServer se : bdd) {
				if (s.equalsApprox(se)) {
					bddInstance = se; break;
				}
			}
			if (bddInstance != null) {
				bddInstance.importData(s);
				dbHlpr.saveMuninServer(bddInstance);
				
				// Plugins
				List<MuninPlugin> toBeDeleted2 = new ArrayList<MuninPlugin>();
				List<MuninPlugin> localObj2 = bddInstance.getPlugins();
				// Suppression des plugins inutilisés...
				for (MuninPlugin dbS : dbHlpr.getPlugins(bddInstance)) {
					int nb = 0;
					for (MuninPlugin p : localObj2) {
						if (dbS.equalsApprox(p)) {
							nb++; break;
						}
					}
					if (nb == 0)
						toBeDeleted2.add(dbS);
				}
				// ... sauf si widget
				for (MuninPlugin mp : toBeDeleted2) {
					boolean hasWidget = false;
					List<Widget> lw = dbHlpr.getWidgets(bddInstance);
					for (Widget mw : lw) {
						if (mw.getPlugin().equalsApprox(mp)) {
							hasWidget = true; break;
						}
					}
					if (!hasWidget)
						dbHlpr.deletePlugin(mp);
				}
				List<MuninPlugin> toBeAdded = new ArrayList<MuninPlugin>();
				for (MuninPlugin mp : s.getPlugins()) {
					boolean add = true;
					for (MuninPlugin mp2 : bddInstance.getPlugins()) {
						// Vérifie si mp est dans la liste
						if (mp2.equalsApprox(mp))
							add = false;
					}
					if (add)
						toBeAdded.add(mp);
				}
				for (MuninPlugin mp : toBeAdded) {
					mp.setInstalledOn(bddInstance);
					dbHlpr.saveMuninPlugin(mp);
				}
			} else {
				dbHlpr.saveMuninServer(s);
				MuninServer serv = getBDDInstance(s);
				serv.setPluginsList(s.getPlugins());
				for (MuninPlugin mp : serv.getPlugins()) {
					mp.setInstalledOn(serv);
					dbHlpr.saveMuninPlugin(mp);
				}
			}
		}
		
		List<MuninMaster> toBeDeleted2 = new ArrayList<MuninMaster>();
		// Delete masters if necessary
		// First, rebuild children
		List<MuninMaster> toBeDeleted3 = new ArrayList<MuninMaster>();
		for (MuninMaster m : this.muninFoo.masters) {
			if (m.manualRebuildChildren(this.muninFoo))
				toBeDeleted3.add(m);
		}
		for (MuninMaster m : toBeDeleted3) {
			this.muninFoo.masters.remove(m);
			this.dbHlpr.deleteMaster(this.muninFoo, m, false);
		}
		for (MuninMaster m : this.muninFoo.masters) {
			if (m.getChildren().size() == 0) { // If removed and no more children in children list, remove master
				toBeDeleted2.add(m);
				this.dbHlpr.deleteMaster(this.muninFoo, m, false);
			}
		}
		for (MuninMaster m : toBeDeleted2)
			this.muninFoo.masters.remove(m);
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
	
	public void logServersTable() {
		log("");
		logLine(60);
		log("Total servers: " + dbHlpr.getServers(muninFoo.masters).size() + "\t Total plugins: " + dbHlpr.getServers(muninFoo.masters).size());
		log("| name                              | nbPlugins | position |");
		logLine(60);
		for (MuninServer s : dbHlpr.getServers(muninFoo.masters)) {
			String[] fields = {s.getName(), s.getPlugins().size() + "", s.getPosition() + "" };
			int[] space = { 33, 9, 8 };
			
			String l = "|";
			for (int i=0; i<fields.length; i++) {
				l += " ";
				if (fields[i].length() > (space[i]))
					l += fields[i].substring(0, space[i]);
				else {
					int n = space[i] - fields[i].length();
					l += fields[i];
					for (int y=0; y<n; y++)
						l += " ";
				}
				l += " | ";
			}
			log(l + "  " + s.getAuthString());
		}
		logLine(60);
	}
	public void logMuninFooServersTable() {
		log("");
		logLine(60);
		log("Total servers: " + muninFoo.getServers().size());
		log("| name                              | nbPlugins | position |");
		logLine(60);
		for (MuninServer s : muninFoo.getServers()) {
			String[] fields = {s.getName(), s.getPlugins().size() + "", s.getPosition() + "" };
			int[] space = { 33, 9, 8 };
			
			String l = "|";
			for (int i=0; i<fields.length; i++) {
				l += " ";
				if (fields[i].length() > (space[i]))
					l += fields[i].substring(0, space[i]);
				else {
					int n = space[i] - fields[i].length();
					l += fields[i];
					for (int y=0; y<n; y++)
						l += " ";
				}
				l += " |";
			}
			l += s.getId();
			log(l);
		}
		logLine(60);
	}
}