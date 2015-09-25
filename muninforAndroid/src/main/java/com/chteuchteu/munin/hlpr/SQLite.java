package com.chteuchteu.munin.hlpr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;

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
		// Insert nodes
		for (MuninNode node : master.getChildren()) {
			dbHlpr.insertMuninNode(node);
			for (MuninPlugin plugin : node.getPlugins())
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

	/**
	 * From db version 7, we began to store plugin page URL in order to
	 * 	retrieve it whenever the plugin gets deleted. Here, we save this
	 * 	information since we don't have it in dbs v6.
	 */
	public static void migrateFrom6To7(SQLiteDatabase db) {
		String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_GRIDITEMRELATIONS;

		Cursor c = db.rawQuery(selectQuery, null);

		// For each GridItem
		if (c.moveToFirst()) {
			do {
				try {
					int gridItemid = c.getInt(c.getColumnIndex(DatabaseHelper.KEY_ID));
					int pluginId = c.getInt(c.getColumnIndex(DatabaseHelper.KEY_GRIDITEMRELATIONS_PLUGIN));

					// Find plugin name
					String pluginpageUrlQuery = "SELECT " + DatabaseHelper.KEY_MUNINPLUGINS_PLUGINPAGEURL
							+ " FROM " + DatabaseHelper.TABLE_MUNINPLUGINS
							+ " WHERE " + DatabaseHelper.KEY_ID + " = " + pluginId;

					Cursor c2 = db.rawQuery(pluginpageUrlQuery, null);

					if (c2.moveToFirst()) {
						String pluginpageUrl = c2.getString(c2.getColumnIndex(DatabaseHelper.KEY_MUNINPLUGINS_PLUGINPAGEURL));

						ContentValues values = new ContentValues();
						values.put(DatabaseHelper.KEY_GRIDITEMRELATIONS_PLUGINPAGEURL, pluginpageUrl);

						db.update(DatabaseHelper.TABLE_GRIDITEMRELATIONS, values, DatabaseHelper.KEY_ID + " = ?", new String[]{String.valueOf(gridItemid)});
					}

					c2.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} while (c.moveToNext());
		}

		c.close();
	}
	
    public void logMasters() {
	    MuninFoo.log("============================================================");
        for (MuninMaster m : this.muninFoo.masters) {
	        MuninFoo.log("[" + m.getName() + "]");
            for (MuninNode s : m.getChildren())
	            MuninFoo.log(" - (" + s.getPosition() + ") " + s.getName());
        }
	    MuninFoo.log("============================================================");
    }
}
