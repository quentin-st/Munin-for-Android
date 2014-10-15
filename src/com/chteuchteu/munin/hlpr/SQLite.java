package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;

public class SQLite {
	private MuninFoo muninFoo;
	public DatabaseHelper dbHlpr;
	
	public SQLite(Context c, MuninFoo m) {
		this.muninFoo = m;
		this.dbHlpr = new DatabaseHelper(c);
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
	
	private void l(String str) { Log.v("migrateTo3", str); }
	public void migrateTo3() {
		// TODO WIP
		l("Launching migrateTo3");
		String KEY_MUNINSERVERS_AUTHLOGIN = "authLogin";
		String KEY_MUNINSERVERS_AUTHPASSWORD = "authPassword";
		String KEY_MUNINSERVERS_SSL = "SSL";
		String KEY_MUNINSERVERS_AUTHTYPE = "authType";
		String KEY_MUNINSERVERS_AUTHSTRING = "authString";
		
		// Here, columns have already been created.
		// Let's check if there are some MuninMaster (_not default_)
		// whose attributes needs to be filled
		
		for (MuninMaster master : muninFoo.getMasters()) {
			l("Loop : master " + master.getName());
			// DefaultMaster groups several heterogeneous servers.
			// We can't easily group credentials here
			if (!master.defaultMaster && !master.isEmpty()) {
				l("=> not default master :)");
				// Get the first server
				MuninServer model = master.getChildAt(0);
				l("Getting auth information from model " + model.getName());
				
				// Get the auth information from db directly
				String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_MUNINSERVERS
						+ " WHERE " + DatabaseHelper.KEY_ID + " = " + model.getId();
				
				SQLiteDatabase db = dbHlpr.getReadableDatabase();
				Cursor c = db.rawQuery(selectQuery, null);
				
				if (c != null && c.moveToFirst()) { // The server has been found (which should always be the case)
					AuthType authType = AuthType.get(c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_AUTHTYPE)));
					
					master.setAuthType(authType);
					l("    authType : " + authType.name());
					if (authType == AuthType.BASIC || authType == AuthType.DIGEST) {
						String authLogin = c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHLOGIN));
						String authPassword = c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHPASSWORD));
						String authString = c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHSTRING));
						
						master.setAuthIds(authLogin, authPassword);
						master.setAuthString(authString);
						
						l("    authLogin : " + authLogin);
						l("    authPassword : " + authPassword);
						l("    authString : " + authString);
					}
					
					boolean ssl = c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_SSL)) == 1;
					master.setSSL(ssl);
					
					DatabaseHelper.close(c, db);
					
					dbHlpr.saveMuninMaster(master);
				}
			}
		}
	}
	
    public void logMasters() {
        log("");
        logLine(60);
        for (MuninMaster m : this.muninFoo.masters) {
            log("[" + m.getName() + "] - " + m.getUrl());
            for (MuninServer s : m.getChildren())
                log("  - " + s.getName() + " - " + s.getServerUrl());
        }
        logLine(60);
    }
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
}