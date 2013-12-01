package com.chteuchteu.munin;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

public class SQLite {
	MuninFoo muninFoo;
	
	public SQLite(MuninFoo m) {
		this.muninFoo = m;
	}
	
	//	SERVERS
	public List<MuninServer> getServers() {
		return new Select().from(MuninServer.class).execute();
	}
	public void saveServers() {
		// Vérification des positions
		for (int i=0; i<muninFoo.getOrderedServers().size(); i++) {
			muninFoo.getOrderedServers().get(i).setPosition(i);
		}
		
		muninFoo.unLinkAll();
		
		// Suppression des serveurs à supprimer
		List<MuninServer> toBeDeleted = new ArrayList<MuninServer>();
		List<MuninServer> localObj = muninFoo.getServers();
		for (MuninServer dbS : getServers()) {
			int nb = 0;
			for (MuninServer s : localObj) {
				if (dbS.equalsApprox(s)) {
					nb++; break;
				}
			}
			if (nb == 0)
				toBeDeleted.add(dbS);
		}
		for (MuninServer s : toBeDeleted) {
			deleteWidgets(s);
			deletePlugins(s);
			s.delete();
		}
		
		List<MuninServer> bdd = getServers();
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
				bddInstance.save();
				
				// Plugins
				List<MuninPlugin> toBeDeleted2 = new ArrayList<MuninPlugin>();
				List<MuninPlugin> localObj2 = bddInstance.getPlugins();
				// Suppression des plugins inutilisés...
				for (MuninPlugin dbS : getPlugins(bddInstance)) {
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
					List<MuninWidget> lw = getWidgets(bddInstance);
					for (MuninWidget mw : lw) {
						if (mw.getPlugin().equalsApprox(mp)) {
							hasWidget = true; break;
						}
					}
					if (!hasWidget)
						mp.delete();
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
					mp.save();
				}
			} else {
				s.save();
				MuninServer serv = getBDDInstance(s);
				serv.setPluginsList(s.getPlugins());
				for (MuninPlugin mp : serv.getPlugins()) {
					mp.setInstalledOn(serv);
					mp.save();
				}
			}
		}
		muninFoo.resetInstance();
	}
	public void deleteServer(MuninServer s) {
		deleteLabels(s);
		new Delete().from(MuninWidget.class).where("server = ?", s.getId()).execute();
		new Delete().from(MuninPlugin.class).where("installedOn = ?", s.getId()).execute();
		new Delete().from(MuninServer.class).where("id = ?", s.getId()).execute();
	}
	public void deleteServer(String url) {
		new Delete().from(MuninServer.class).where("serverUrl = ?", url).execute();
	}
	public void deleteServers() {
		new Delete().from(MuninServer.class).execute();
	}
	public MuninServer getBDDInstance(MuninServer s) {
		for (MuninServer serv : getServers()) {
			if (serv.equalsApprox(s.getServerUrl()))
				return serv;
		}
		return s;
	}
	public void fetchMuninLabels() {
		muninFoo.labels = new ArrayList<MuninLabel>();
		muninFoo.labels_relations = new ArrayList<MuninLabelRelation>();
		List<MuninLabelRelation> l = new Select().from(MuninLabelRelation.class).execute();
		for (MuninLabelRelation rel : l) {
			muninFoo.labels_relations.add(rel);
			if (rel != null && rel.getLabelName() != null && rel.getPlugin() != null) {
				if (!muninFoo.containsLabel(rel.getLabelName()))
					muninFoo.labels.add(new MuninLabel(rel.getLabelName()));
				muninFoo.getLabel(rel.getLabelName()).addPlugin(rel.getPlugin().setInstalledOn(rel.getInstalledOn()));
			} else {
				try {
					rel.delete();
				} catch (Exception e) {}
			}
		}
	}
	public void saveMuninLabels() {
		deleteLabels();
		// For each label relation : create one MuninLabelRelation object, then call obj.save()
		Log.v("", "=====================================");
		for (MuninLabel l : muninFoo.labels) {
			for (MuninPlugin p : l.plugins) {
				MuninLabelRelation rel = new MuninLabelRelation(getBDDInstance(p, p.getInstalledOn()), l.getName(), p.getInstalledOn());
				Log.v("", "Saving label " + l.getName() + " \t " + p.getName() + "\t" + p.getInstalledOn().getName());
				rel.save();
			}
		}
		Log.v("", "=====================================");
	}
	public void deleteLabels() {
		new Delete().from(MuninLabelRelation.class).execute();
	}
	public void deleteLabels(MuninServer s) {
		for (MuninLabelRelation l : muninFoo.labels_relations) {
			if (l.getPlugin().getInstalledOn().equalsApprox(s))
				l.delete();
		}
	}
	
	
	
	//	PLUGINS
	public List<MuninPlugin> getPlugins() {
		return new Select().from(MuninPlugin.class).execute();
	}
	public List<MuninPlugin> getPlugins(MuninServer s) {
		return new Select().from(MuninPlugin.class).where("installedOn = ?", s.getId()).execute();
	}
	public void deletePlugins(MuninServer s) {
		new Delete().from(MuninPlugin.class).where("installedOn = ?", s.getId()).execute();
	}
	public void deletePlugins() {
		new Delete().from(MuninPlugin.class).execute();
	}
	public MuninPlugin getBDDInstance(MuninPlugin p, MuninServer s) {
		MuninServer server = getBDDInstance(s);
		for (MuninPlugin pl : getPlugins(server)) {
			if (pl.equalsApprox(p))
				return pl;
		}
		return null;
	}
	
	
	
	//	WIDGETS
	public MuninWidget getWidget(int widgetId) {
		return new Select().from(MuninWidget.class).where("widgetId = ?", widgetId).executeSingle();
	}
	public List<MuninWidget> getWidgets() {
		return new Select().from(MuninWidget.class).execute();
	}
	public List<MuninWidget> getWidgets(MuninServer s) {
		return new Select().from(MuninWidget.class).where("server = ?", s.getId()).execute();
	}
	public void deleteWidget(int widgetId) {
		new Delete().from(MuninWidget.class).where("widgetId = ?", widgetId).execute();
	}
	public void deleteWidgets(MuninServer s) {
		new Delete().from(MuninWidget.class).where("server = ?", s.getId()).execute();
	}
	public void deleteWidgets(MuninPlugin p) {
		new Delete().from(MuninWidget.class).where("plugin = ?", p.getId()).execute();
	}
	public void deleteWidgets(MuninPlugin p, MuninServer s) {
		new Delete().from(MuninWidget.class).where("plugin = ?", p.getId()).where("installedOn = ?", s.getId()).execute();
	}
	public void deleteWidgets() {
		new Delete().from(MuninWidget.class).execute();
	}
	
	
	
	
	
	
	
	// Logs
	public void logWidgets() {
		Log.v("SQLite", "==========================================");
		if (getWidgets().size() > 0) {
			for (MuninWidget w : getWidgets()) {
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
					Log.v("SQLite", s);
				}
				else
					Log.v("SQLite", "Something's null");
			}
		} else
			Log.v("SQLite", "No widgets in the database.");
		Log.v("SQLite", "==========================================");
	}
	public void logServers() {
		Log.v("SQLite", "==========================================");
		if (getServers().size() > 0) {
			for (MuninServer s : getServers()) {
				Log.v("SQLite", s.getName() + "\t  " + s.getServerUrl());
			}
		} else
			Log.v("SQLite", "No servers in the database.");
		Log.v("SQLite", "==========================================");
	}
	public void logPlugins() {
		Log.v("SQLite", "==========================================");
		if (getPlugins().size() > 0) {
			for (MuninPlugin p : getPlugins()) {
				Log.v("SQLite", p.getName() + "\t  " + p.getFancyName());
			}
		} else
			Log.v("SQLite", "No plugins in the database.");
		Log.v("SQLite", "==========================================");
	}
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
		log("Total servers: " + getServers().size() + "\t Total plugins: " + getPlugins().size());
		log("| name                              | nbPlugins | position |");
		logLine(60);
		for (MuninServer s : getServers()) {
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
	public void logServersPosition() {
		for (MuninServer s : getServers()) {
			Log.v("", s.getId() + "\t" + s.getPosition() + "\t" + s.getName());
		}
	}
	public void logLocalServersPosition() {
		for (MuninServer s : muninFoo.getServers()) {
			Log.v("", s.getId() + "\t" + s.getPosition() + "\t" + s.getName());
		}
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