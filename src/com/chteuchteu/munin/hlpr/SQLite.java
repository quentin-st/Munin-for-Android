package com.chteuchteu.munin.hlpr;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Label;
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
		for (MuninServer serv : dbHlpr.getServers()) {
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
		for (MuninServer dbS : dbHlpr.getServers()) {
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
		
		List<MuninServer> bdd = dbHlpr.getServers();
		for (MuninServer s : muninFoo.getServers()) {
			MuninServer bddInstance = null;
			
			// Recherche si serveur présent en bdd
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
	
	/*public void saveLabels() {
		// Check if we have to add / delete Labels
		List<Label> toBeDeleted = new ArrayList<Label>();
		
		// Delete labels if necessary
		for (Label dbL : dbHlpr.getLabels()) {
			int nb = 0;
			for (Label l : muninFoo.labels) {
				if (l.equals(dbL)) {
					nb++; break;
				}
			}
			if (nb == 0) {
				toBeDeleted.add(dbL);
			}
		}
		for (Label l : toBeDeleted)
			dbHlpr.deleteLabel(l);
		
		// Add labels if necessary
		List<MuninPlugin> toBeDeleted2 = new ArrayList<MuninPlugin>();
		for (Label localL : muninFoo.labels) {
			Log.v("LABEL", localL.getName());
			Label dbL = dbHlpr.getLabel(localL.getName());
			if (dbL != null) {
				// Label already exists in BDD. Let's update relations if necessary
				// Delete old relations
				for (MuninPlugin dbP : dbHlpr.getPlugins(dbL)) {
					int nb = 0;
					for (MuninPlugin p : dbL.plugins) {
						if (p.equalsApprox(dbP)) {
							nb++; break;
						}
					}
					if (nb == 0)
						toBeDeleted2.add(dbP);
				}
				for (MuninPlugin p : toBeDeleted2) {
					dbHlpr.deleteLabelsRelation(p, dbL);
				}
				
				// Create new relations
				for (MuninPlugin p : dbL.plugins) {
					if (dbHlpr.getPlugins(dbL) == null)
						dbHlpr.insertLabelRelation(p, dbL);
				}
			} else {
				// Create label and labelrelations
				dbHlpr.insertLabel(localL);
				for (MuninPlugin p : localL.plugins) {
					dbHlpr.insertLabelRelation(p, localL);
				}
			}
		}
	}*/
	
	// Logs
	/*public void logWidgets() {
		Log.v("SQLite_old", "==========================================");
		if (getWidgets().size() > 0) {
			for (Widget w : getWidgets()) {
				if (w != null) {
					String s = "";
					if (w.getPeriod() != null)
						s = s + w.getPeriod() + " ";
					else
						s = s + "{period} ";
					if (w.getPlugin() != null)
						s = s + w.getPlugin().getName() + " ";
					else
						s = s + "{plugin} ";
					if (w.getServer() != null)
						s = s + w.getServer().getName() + " ";
					else
						s = s + "{server} ";
					Log.v("SQLite_old", s);
				}
				else
					Log.v("SQLite_old", "Something's null");
			}
		} else
			Log.v("SQLite_old", "No widgets in the database.");
		Log.v("SQLite_old", "==========================================");
	}*/
	public void logServers() {
		Log.v("SQLite_old", "==========================================");
		if (dbHlpr.getServers().size() > 0) {
			for (MuninServer s : dbHlpr.getServers()) {
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
	public void logLine(int nb) {
		if (nb == 0)
			logLine(88);
		else {
			String s = "";
			for (int i=0; i<nb; i++)
				s += "=";
			log(s);
		}
	}
	public void log(String txt) {
		Log.v("", txt);
	}
	public void logServersTable() {
		log("");
		logLine(60);
		log("Total servers: " + dbHlpr.getServers().size() + "\t Total plugins: " + dbHlpr.getServers().size());
		log("| name                              | nbPlugins | position |");
		logLine(60);
		for (MuninServer s : dbHlpr.getServers()) {
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