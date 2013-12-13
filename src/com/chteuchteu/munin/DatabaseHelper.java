package com.chteuchteu.munin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "muninForAndroid";
	
	// Table names
	private static final String TABLE_MUNINSERVERS = "muninServers";
	private static final String TABLE_MUNINPLUGINS = "muninPlugins";
	private static final String TABLE_LABELS = "labels";
	private static final String TABLE_LABELSRELATIONS = "labelsRelations";
	private static final String TABLE_WIDGETS = "widgets";
	
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
	private static final String KEY_MUNINSERVERS_PARENT = "parent";
	
	private static final String KEY_MUNINPLUGINS_NAME = "name";
	private static final String KEY_MUNINPLUGINS_FANCYNAME = "fancyName";
	private static final String KEY_MUNINPLUGINS_SERVER = "server";
	private static final String KEY_MUNINPLUGINS_CATEGORY = "category";
	
	private static final String KEY_LABELS_NAME = "name";
	
	private static final String KEY_LABELSRELATIONS_PLUGIN = "plugin";
	private static final String KEY_LABELSRELATIONS_LABEL = "label";
	
	private static final String KEY_WIDGETS_SERVER = "server";
	private static final String KEY_WIDGETS_PLUGIN = "plugin";
	private static final String KEY_WIDGETS_PERIOD = "period";
	private static final String KEY_WIDGETS_WIFIONLY = "wifiOnly";
	
	private static final String CREATE_TABLE_MUNINSERVER = "CREATE TABLE " + TABLE_MUNINSERVERS + " ("
			+ KEY_ID + " INTEGER PRIMARY KEY,"
			+ KEY_MUNINSERVERS_SERVERURL + " TEXT,"
			+ KEY_MUNINSERVERS_AUTHLOGIN + " TEXT,"
			+ KEY_MUNINSERVERS_AUTHPASSWORD + " TEXT,"
			+ KEY_MUNINSERVERS_GRAPHURL + " TEXT,"
			+ KEY_MUNINSERVERS_SSL + " INTEGER,"
			+ KEY_MUNINSERVERS_POSITION + " INTEGER,"
			+ KEY_MUNINSERVERS_AUTHTYPE + " INTEGER,"
			+ KEY_MUNINSERVERS_AUTHSTRING + " TEXT,"
			+ KEY_MUNINSERVERS_PARENT + " TEXT)";
	
	private static final String CREATE_TABLE_MUNINPLUGINS = "CREATE TABLE " + TABLE_MUNINPLUGINS + " ("
			+ KEY_ID + " INTEGER PRIMARY KEY,"
			+ KEY_MUNINPLUGINS_NAME + " TEXT,"
			+ KEY_MUNINPLUGINS_FANCYNAME + " TEXT,"
			+ KEY_MUNINPLUGINS_SERVER + " INTEGER,"
			+ KEY_MUNINPLUGINS_CATEGORY + " TEXT)";
	
	private static final String CREATE_TABLE_LABELS = "CREATE TABLE " + TABLE_LABELS + " ("
			+ KEY_ID + " INTEGER PRIMARY KEY,"
			+ KEY_LABELS_NAME + " TEXT)";
	
	private static final String CREATE_TABLE_LABELSRELATIONS = "CREATE TABLE " + TABLE_LABELSRELATIONS + " ("
			+ KEY_ID + " INTEGER PRIMARY KEY,"
			+ KEY_LABELSRELATIONS_LABEL + " INTEGER,"
			+ KEY_LABELSRELATIONS_PLUGIN + " INTEGER)";
	
	private static final String CREATE_TABLE_WIDGETS = "CREATE TABLE " + TABLE_WIDGETS + " ("
			+ KEY_ID + " INTEGER PRIMARY KEY,"
			+ KEY_WIDGETS_SERVER + " INTEGER,"
			+ KEY_WIDGETS_PLUGIN + " INTEGER,"
			+ KEY_WIDGETS_PERIOD + " TEXT,"
			+ KEY_WIDGETS_WIFIONLY + " INTEGER)";
	
	public DatabaseHelper(Context c) {
		super(c, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_MUNINSERVER);
		db.execSQL(CREATE_TABLE_MUNINPLUGINS);
		db.execSQL(CREATE_TABLE_LABELS);
		db.execSQL(CREATE_TABLE_LABELSRELATIONS);
		db.execSQL(CREATE_TABLE_WIDGETS);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO
	}
	
	public long insertMuninServer(MuninServer s) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_MUNINSERVERS_SERVERURL, s.getServerUrl());
		values.put(KEY_MUNINSERVERS_AUTHLOGIN, s.getAuthLogin());
		values.put(KEY_MUNINSERVERS_AUTHPASSWORD, s.getAuthPassword());
		values.put(KEY_MUNINSERVERS_GRAPHURL, s.getGraphURL());
		values.put(KEY_MUNINSERVERS_SSL, s.getSSL());
		values.put(KEY_MUNINSERVERS_POSITION, s.getPosition());
		values.put(KEY_MUNINSERVERS_AUTHTYPE, s.getAuthType());
		values.put(KEY_MUNINSERVERS_AUTHSTRING, s.getAuthString());
		values.put(KEY_MUNINSERVERS_PARENT, s.getParent());
		
		long id = db.insert(TABLE_MUNINSERVERS, null, values);
		s.setBddId(id);
		return id;
	}
	
	public long insertMuninPlugin(MuninPlugin p) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_MUNINPLUGINS_NAME, p.getName());
		values.put(KEY_MUNINPLUGINS_FANCYNAME, p.getFancyName());
		values.put(KEY_MUNINPLUGINS_SERVER, p.getInstalledOn().getId());
		values.put(KEY_MUNINPLUGINS_CATEGORY, p.getCategory());
		
		long id = db.insert(TABLE_MUNINPLUGINS, null, values);
		p.setBddId(id);
		return id;
	}
	
	public long insertLabel(Label l) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_LABELS_NAME, l.getName());
		
		long id = db.insert(TABLE_LABELS, null, values);
		l.setBddId(id);
		return id;
	}
	
	public long insertLabelRelation(MuninPlugin p, Label l) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_LABELSRELATIONS_LABEL, l.getBddId());
		values.put(KEY_LABELSRELATIONS_PLUGIN, p.getBddId());
		
		return db.insert(TABLE_LABELSRELATIONS, null, values);
	}
	
	public long insertWidget(Widget w) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_WIDGETS_PLUGIN, w.getPlugin().getId());
		values.put(KEY_WIDGETS_SERVER, w.getServer().getId());
		values.put(KEY_WIDGETS_PERIOD, w.getPeriod());
		values.put(KEY_WIDGETS_WIFIONLY, w.isWifiOnly());
		
		long id = db.insert(TABLE_WIDGETS, null, values);
		w.setBddId(id);
		return id;
	}
	
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
		s.setParent(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_PARENT)));
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
		p.setName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_NAME)));
		p.setFancyName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_FANCYNAME)));
		p.setCategory(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_NAME)));
		// TODO : server ?
		return p;
	}
	
}