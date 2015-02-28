package com.chteuchteu.munin.hlpr;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;

import java.util.ArrayList;

public class SQLite {
	private MuninFoo muninFoo;
	public DatabaseHelper dbHlpr;
	
	public SQLite(Context c, MuninFoo m) {
		this.muninFoo = m;
		this.dbHlpr = new DatabaseHelper(c);
	}
	
	public void insertMuninMaster(MuninMaster master) {
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
	
	public void migrateTo3() {
		// Here, columns have already been created.
		// Let's check if there are some MuninMaster (_not default_)
		// whose attributes needs to be filled
		
		for (MuninMaster master : muninFoo.getMasters()) {
			// DefaultMaster groups several heterogeneous servers.
			// We can't easily group credentials here
			if (!master.defaultMaster && !master.isEmpty()) {
				// Get the first server
				MuninServer model = master.getChildren().get(0);
				
				getOldAuthInformation(model.getId(), master, true);
			}
		}
		
		// Merge defaultMaster childrens by host name. If necessary get old auth information.
		// Find default master
		MuninMaster defaultMaster = null;
		for (MuninMaster master : muninFoo.getMasters()) {
			if (master.defaultMaster) {
				defaultMaster = master;
				break;
			}
		}
		
		// We don't necessarily have a default master
		if (defaultMaster != null) {
			ArrayList<MuninMaster> newMasters = new ArrayList<>();
			
			for (MuninServer server : defaultMaster.getChildren()) {
				// Check if there already is a master for this server in newMasters
				String masterUrl = Util.URLManipulation.ascendDirectory(2, server.getServerUrl());
				
				MuninMaster parent = null;
				boolean contains = false;
				for (MuninMaster master : newMasters) {
					if (master.getUrl().equals(masterUrl)) {
						contains = true;
						parent = master;
						break;
					}
				}
				
				// Doesn't contains => add
				if (!contains) {
					String masterName = Util.URLManipulation.getHostFromUrl(server.getServerUrl());
					
					parent = new MuninMaster();
					parent.setName(masterName);
					parent.setUrl(masterUrl);
					newMasters.add(parent);
				}
				
				// Attach server to parent
				parent.addChild(server);
				
				if (!contains) {
					// Get auth ids if needed. Only needed to be done the first time
					// (since all the servers should have the same auth ids)
					getOldAuthInformation(server.getId(), parent, false);
				}
			}
			
			// Insert masters and update children
			for (MuninMaster master : newMasters) {
				dbHlpr.insertMuninMaster(master);
				for (MuninServer server : master.getChildren())
					dbHlpr.updateMuninServer(server);
			}
			
			// Delete default master
			dbHlpr.deleteMaster(defaultMaster, false);
		}
	}
	
	/**
	 * Before Munin for Android 3.0, auth ids were attached to MuninServer.
	 * Those auth information weren't deleting during update so they can
	 * be fetched back for migration purposes.
	 * @param muninServerId Information source (MuninServer id)
	 * @param attachTo Information destination
	 * @param saveChanges boolean
	 */
	private void getOldAuthInformation(long muninServerId, MuninMaster attachTo, boolean saveChanges) {
		String KEY_MUNINSERVERS_AUTHLOGIN = "authLogin";
		String KEY_MUNINSERVERS_AUTHPASSWORD = "authPassword";
		String KEY_MUNINSERVERS_SSL = "SSL";
		String KEY_MUNINSERVERS_AUTHTYPE = "authType";
		String KEY_MUNINSERVERS_AUTHSTRING = "authString";
		
		String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_MUNINSERVERS
				+ " WHERE " + DatabaseHelper.KEY_ID + " = " + muninServerId;
		
		SQLiteDatabase db = dbHlpr.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) { // The server has been found (which should always be the case)
			AuthType authType = AuthType.get(c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_AUTHTYPE)));
			
			attachTo.setAuthType(authType);
			if (authType == AuthType.BASIC || authType == AuthType.DIGEST) {
				String authLogin = c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHLOGIN));
				String authPassword = c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHPASSWORD));
				String authString = c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHSTRING));
				
				attachTo.setAuthIds(authLogin, authPassword);
				attachTo.setAuthString(authString);
			}
			
			boolean ssl = c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_SSL)) == 1;
			attachTo.setSSL(ssl);
			
			DatabaseHelper.close(c, db);
			
			if (saveChanges)
				dbHlpr.saveMuninMaster(attachTo);
		}
	}
	
    public void logMasters() {
        MuninFoo.log("");
	    MuninFoo.log("============================================================");
        for (MuninMaster m : this.muninFoo.masters) {
	        MuninFoo.log("[" + m.getName() + "]");
            for (MuninServer s : m.getChildren())
	            MuninFoo.log(" - (" + s.getPosition() + ") " + s.getName());
        }
	    MuninFoo.log("============================================================");
    }
}
