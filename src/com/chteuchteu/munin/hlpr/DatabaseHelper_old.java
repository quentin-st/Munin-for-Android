package com.chteuchteu.munin.hlpr;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.chteuchteu.munin.obj.LabelRelation;
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
	private static final String TABLE_LABELSRELATIONS = "MuninLabelRelations";
	private static final String TABLE_WIDGETS = "MuninWidgets";
	
	// Fields
	private static final String KEY_ID = "id";
	
	private static final String KEY_MUNINSERVERS_SERVERURL = "serverUrl";
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
	private static final String KEY_MUNINPLUGINS_CATEGORY = "category";
	
	private static final String KEY_LABELSRELATIONS_PLUGIN = "Plugin";
	private static final String KEY_LABELSRELATIONS_LABELNAME = "LabelName";
	private static final String KEY_LABELSRELATIONS_SERVER = "Server";
	
	private static final String KEY_WIDGETS_SERVER = "server";
	private static final String KEY_WIDGETS_PLUGIN = "plugin";
	private static final String KEY_WIDGETS_PERIOD = "period";
	private static final String KEY_WIDGETS_WIFIONLY = "wifiOnly";
	private static final String KEY_WIDGETS_WIDGETID = "widgetId";
	
	public DatabaseHelper_old(Context c) {
		super(c, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	// TODO :
	public MuninServer getMuninServer(long id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		String selectQuery = "SELECT * FROM " + TABLE_MUNINSERVERS + " WHERE "
				+ KEY_ID + " = " + id;
		
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null)
			c.moveToFirst();
		
		MuninServer s = new MuninServer();
		s.setBddId(c.getInt(c.getColumnIndex(KEY_ID)));
		s.setServerUrl(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_SERVERURL)));
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
		return s;
	}
	
	public MuninPlugin getMuninPlugin(long id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		String selectQuery = "SELECT * FROM " + TABLE_MUNINPLUGINS + " WHERE "
				+ KEY_ID + " = " + id;
		
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null)
			c.moveToFirst();
		
		MuninPlugin p = new MuninPlugin();
		p.setBddId(c.getInt(c.getColumnIndex(KEY_ID)));
		p.setName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_NAME)));
		p.setFancyName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_FANCYNAME)));
		p.setCategory(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_NAME)));
		// TODO : server ?
		return p;
	}
	
	public LabelRelation getLabelRelation(long id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		String selectQuery = "SELECT * FROM " + TABLE_LABELSRELATIONS + " WHERE "
				+ KEY_ID + " = " + id;
		
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null)
			c.moveToFirst();
		
		LabelRelation r = new LabelRelation();
		r.setBddId(c.getInt(c.getColumnIndex(KEY_ID)));
		// TODO setLabel
		// TODO setPlugin
		return r;
	}
	
	public Widget getWidget(long id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		String selectQuery = "SELECT * FROM " + TABLE_WIDGETS + " WHERE "
				+ KEY_ID + " = " + id;
		
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null)
			c.moveToFirst();
		
		Widget w = new Widget();
		// setPlugin
		// setPeriod
		w.setPeriod(c.getString(c.getColumnIndex(KEY_WIDGETS_PERIOD)));
		if (c.getInt(c.getColumnIndex(KEY_WIDGETS_WIFIONLY)) == 1)
			w.setWifiOnly(true);
		else
			w.setWifiOnly(false);
		return w;
	}
	
	public List<MuninServer> getAllServers() {
		List<MuninServer> auteurs = new ArrayList<MuninServer>();
		String selectQuery = "SELECT * FROM " + TABLE_MUNINSERVERS;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		// looping through all rows and adding to list
		if (c != null && c.moveToFirst()) {
			do {
				Log.v("server url : ", c.getString(c.getColumnIndex(KEY_MUNINSERVERS_SERVERURL)));
				/*Auteur auteur = new Auteur();
				auteur.setId(c.getInt(c.getColumnIndex(KEY_ID)));
				auteur.setNom((c.getString(c.getColumnIndex(KEY_AUTEURS_NOM))));
				
				auteurs.add(auteur);*/
			} while (c.moveToNext());
		} else
			Log.v("", "Pas de serveurs :s");
		c.close();
		
		return auteurs;
	}
}