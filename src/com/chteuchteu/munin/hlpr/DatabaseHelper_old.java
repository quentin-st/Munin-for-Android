package com.chteuchteu.munin.hlpr;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.Widget;

/*
 * Created to access ActiveAndroid database.
 * Used to migrate data from AA's DB to new one
 * Only the read functions are implemented
 * 		(we won't edit any data)
 */
public class DatabaseHelper_old extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 10;
	private static final String DATABASE_NAME = "MuninforAndroid.db";
	
	// Table names
	private static final String TABLE_MUNINSERVERS = "MuninServers";
	private static final String TABLE_MUNINPLUGINS = "MuninPlugins";
	private static final String TABLE_WIDGETS = "MuninWidgets";
	
	// Fields
	private static final String KEY_ID = "Id";
	
	private static final String KEY_MUNINSERVERS_SERVERURL = "serverUrl";
	private static final String KEY_MUNINSERVERS_NAME = "name";
	private static final String KEY_MUNINSERVERS_AUTHLOGIN = "authLogin";
	private static final String KEY_MUNINSERVERS_AUTHPASSWORD = "authPassword";
	private static final String KEY_MUNINSERVERS_GRAPHURL = "graphURL";
	private static final String KEY_MUNINSERVERS_SSL = "SSL";
	private static final String KEY_MUNINSERVERS_POSITION = "position";
	private static final String KEY_MUNINSERVERS_AUTHTYPE = "authType";
	private static final String KEY_MUNINSERVERS_AUTHSTRING = "authString";
	
	private static final String KEY_MUNINPLUGINS_NAME = "name";
	private static final String KEY_MUNINPLUGINS_FANCYNAME = "fancyName";
	private static final String KEY_MUNINPLUGINS_SERVER = "installedOn";
	
	private static final String KEY_WIDGETS_SERVER = "server";
	private static final String KEY_WIDGETS_PLUGIN = "plugin";
	private static final String KEY_WIDGETS_PERIOD = "period";
	private static final String KEY_WIDGETS_WIFIONLY = "wifiOnly";
	private static final String KEY_WIDGETS_WIDGETID = "widgetId";
	
	public DatabaseHelper_old(Context c) {
		super(c, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public boolean exists() {
		return false;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) { }
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
	
	public List<MuninServer> getAllMuninServers() {
		List<MuninServer> l = new ArrayList<MuninServer>();
		String selectQuery = "SELECT * FROM " + TABLE_MUNINSERVERS;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) {
			do {
				MuninServer s = new MuninServer();
				s.setId(c.getInt(c.getColumnIndex(KEY_ID)));
				s.setServerUrl(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_SERVERURL)));
				s.setName(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_NAME)));
				s.setAuthIds(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHLOGIN)),
						c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHPASSWORD)),
						c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_AUTHTYPE)));
				s.setAuthString(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHSTRING)));
				if (c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_SSL)) == 1)
					s.setSSL(true);
				else
					s.setSSL(false);
				s.setGraphURL(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_GRAPHURL)));
				s.setPosition(c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_POSITION)));
				s.setPluginsList(getAllPlugins(s));
				l.add(s);
			} while (c.moveToNext());
		}
		
		return l;
	}
	
	public List<MuninPlugin> getAllPlugins(MuninServer s) {
		List<MuninPlugin> l = new ArrayList<MuninPlugin>();
		String selectQuery = "SELECT * FROM " + TABLE_MUNINPLUGINS 
				+ " WHERE " + KEY_MUNINPLUGINS_SERVER + " = " + s.getId();
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) {
			do {
				MuninPlugin p = new MuninPlugin();
				p.setId(c.getInt(c.getColumnIndex(KEY_ID)));
				p.setName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_NAME)));
				p.setFancyName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_FANCYNAME)));
				p.setCategory(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_NAME)));
				p.setInstalledOn(s);
				l.add(p);
			} while (c.moveToNext());
		}
		
		return l;
	}
	
	public MuninPlugin getMuninPlugin(int id) {
		String selectQuery = "SELECT * FROM " + TABLE_MUNINPLUGINS 
				+ " WHERE " + KEY_ID + " = " + id;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null)
			c.moveToFirst();
		
		MuninPlugin p = new MuninPlugin();
		p.setId(c.getInt(c.getColumnIndex(KEY_ID)));
		p.setName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_NAME)));
		p.setFancyName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_FANCYNAME)));
		p.setCategory(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_NAME)));
		p.setInstalledOn(getMuninServer(c.getInt(c.getColumnIndex(KEY_MUNINPLUGINS_SERVER))));
		return p;
	}
	
	public MuninServer getMuninServer(int id) {
		String selectQuery = "SELECT * FROM " + TABLE_MUNINPLUGINS 
				+ " WHERE " + KEY_ID + " = " + id;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null)
			c.moveToFirst();
		
		MuninServer s = new MuninServer();
		s.setId(c.getInt(c.getColumnIndex(KEY_ID)));
		s.setServerUrl(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_SERVERURL)));
		s.setName(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_NAME)));
		s.setAuthIds(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHLOGIN)),
				c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHPASSWORD)),
				c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_AUTHTYPE)));
		s.setAuthString(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_AUTHSTRING)));
		if (c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_SSL)) == 1)
			s.setSSL(true);
		else
			s.setSSL(false);
		s.setGraphURL(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_GRAPHURL)));
		s.setPosition(c.getInt(c.getColumnIndex(KEY_MUNINSERVERS_POSITION)));
		s.setPluginsList(getAllPlugins(s));
		return s;
	}
	
	public List<Widget> getAllWidgets() {
		List<Widget> l = new ArrayList<Widget>();
		String selectQuery = "SELECT * FROM " + TABLE_WIDGETS;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) {
			do {
				Widget w = new Widget();
				w.setId(c.getInt(c.getColumnIndex(KEY_ID)));
				w.setPeriod(c.getString(c.getColumnIndex(KEY_WIDGETS_PERIOD)));
				w.setWidgetId(c.getInt(c.getColumnIndex(KEY_WIDGETS_WIDGETID)));
				MuninPlugin pl = getMuninPlugin(c.getInt(c.getColumnIndex(KEY_WIDGETS_PLUGIN)));
				pl.setInstalledOn(getMuninServer(c.getInt(c.getColumnIndex(KEY_WIDGETS_SERVER))));
				w.setPlugin(pl);
				w.setWifiOnly(c.getInt(c.getColumnIndex(KEY_WIDGETS_WIFIONLY)));
				l.add(w);
			} while (c.moveToNext());
		}
		
		return l;
	}
	
	public long insertMuninServer(MuninServer s) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_MUNINSERVERS_SERVERURL, s.getServerUrl());
		values.put(KEY_MUNINSERVERS_NAME, s.getName());
		values.put(KEY_MUNINSERVERS_AUTHLOGIN, s.getAuthLogin());
		values.put(KEY_MUNINSERVERS_AUTHPASSWORD, s.getAuthPassword());
		values.put(KEY_MUNINSERVERS_GRAPHURL, s.getGraphURL());
		values.put(KEY_MUNINSERVERS_SSL, s.getSSL());
		values.put(KEY_MUNINSERVERS_POSITION, s.getPosition());
		values.put(KEY_MUNINSERVERS_AUTHTYPE, s.getAuthType());
		values.put(KEY_MUNINSERVERS_AUTHSTRING, s.getAuthString());
		
		long id = db.insert(TABLE_MUNINSERVERS, null, values);
		s.setId(id);
		s.isPersistant = true;
		return id;
	}
	
	public long insertMuninPlugin(MuninPlugin p) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_MUNINPLUGINS_NAME, p.getName());
		values.put(KEY_MUNINPLUGINS_FANCYNAME, p.getFancyName());
		values.put(KEY_MUNINPLUGINS_SERVER, p.getInstalledOn().getId());
		
		long id = db.insert(TABLE_MUNINPLUGINS, null, values);
		p.setId(id);
		return id;
	}
	
	public long insertWidget(Widget w) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_WIDGETS_PLUGIN, w.getPlugin().getId());
		values.put(KEY_WIDGETS_PERIOD, w.getPeriod());
		values.put(KEY_WIDGETS_WIFIONLY, w.isWifiOnly());
		values.put(KEY_WIDGETS_WIDGETID, w.getWidgetId());
		
		long id = db.insert(TABLE_WIDGETS, null, values);
		w.setId(id);
		return id;
	}
}