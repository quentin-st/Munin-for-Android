package com.chteuchteu.munin.hlpr;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.Widget;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "muninForAndroid2.db";
	
	// Table names
	private static final String TABLE_MUNINSERVERS = "muninServers";
	private static final String TABLE_MUNINPLUGINS = "muninPlugins";
	private static final String TABLE_LABELS = "labels";
	private static final String TABLE_LABELSRELATIONS = "labelsRelations";
	private static final String TABLE_WIDGETS = "widgets";
	
	// Fields
	private static final String KEY_ID = "id";
	
	private static final String KEY_MUNINSERVERS_SERVERURL = "serverUrl";
	private static final String KEY_MUNINSERVERS_NAME = "name";
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
	
	private static final String KEY_WIDGETS_PLUGIN = "plugin";
	private static final String KEY_WIDGETS_PERIOD = "period";
	private static final String KEY_WIDGETS_WIFIONLY = "wifiOnly";
	private static final String KEY_WIDGETS_WIDGETID = "widgetId";
	
	private static final String CREATE_TABLE_MUNINSERVERS = "CREATE TABLE " + TABLE_MUNINSERVERS + " ("
			+ KEY_ID + " INTEGER PRIMARY KEY,"
			+ KEY_MUNINSERVERS_SERVERURL + " TEXT,"
			+ KEY_MUNINSERVERS_NAME + " TEXT,"
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
			+ KEY_WIDGETS_WIDGETID + " INTEGER,"
			+ KEY_WIDGETS_PLUGIN + " INTEGER,"
			+ KEY_WIDGETS_PERIOD + " TEXT,"
			+ KEY_WIDGETS_WIFIONLY + " INTEGER)";
	
	public DatabaseHelper(Context c) {
		super(c, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_MUNINSERVERS);
		db.execSQL(CREATE_TABLE_MUNINPLUGINS);
		db.execSQL(CREATE_TABLE_LABELS);
		db.execSQL(CREATE_TABLE_LABELSRELATIONS);
		db.execSQL(CREATE_TABLE_WIDGETS);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (newVersion == 2)
			db.execSQL("ALTER TABLE " + TABLE_MUNINSERVERS + " ADD COLUMN " + KEY_MUNINSERVERS_NAME + " TEXT");
	}
	
	private void close(Cursor c, SQLiteDatabase db) {
		if (c != null)	c.close();
		if (db != null) db.close();
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
		values.put(KEY_MUNINSERVERS_PARENT, s.getParent());
		
		long id = db.insert(TABLE_MUNINSERVERS, null, values);
		s.setId(id);
		s.isPersistant = true;
		
		close(null, db);
		return id;
	}
	
	public long saveMuninServer(MuninServer s) {
		if (s.isPersistant)
			return updateMuninServer(s);
		else
			return insertMuninServer(s);
	}
	
	public long insertMuninPlugin(MuninPlugin p) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_MUNINPLUGINS_NAME, p.getName());
		values.put(KEY_MUNINPLUGINS_FANCYNAME, p.getFancyName());
		values.put(KEY_MUNINPLUGINS_SERVER, p.getInstalledOn().getId());
		values.put(KEY_MUNINPLUGINS_CATEGORY, p.getCategory());
		
		long id = db.insert(TABLE_MUNINPLUGINS, null, values);
		p.setId(id);
		close(null, db);
		return id;
	}
	
	public long saveMuninPlugin(MuninPlugin p) {
		if (p.isPersistant)
			return updateMuninPlugin(p);
		else
			return insertMuninPlugin(p);
	}
	
	
	
	public long insertLabel(Label l) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_LABELS_NAME, l.getName());
		
		long id = db.insert(TABLE_LABELS, null, values);
		l.setId(id);
		close(null, db);
		return id;
	}
	
	public long insertLabelRelation(MuninPlugin p, Label l) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_LABELSRELATIONS_LABEL, l.getId());
		values.put(KEY_LABELSRELATIONS_PLUGIN, p.getId());
		
		close(null, db);
		return db.insert(TABLE_LABELSRELATIONS, null, values);
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
		close(null, db);
		return id;
	}
	
	public int updateMuninServer(MuninServer s) {
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
		values.put(KEY_MUNINSERVERS_PARENT, s.getParent());
		
		int nbRows = db.update(TABLE_MUNINSERVERS, values, KEY_ID + " = ?", new String[] { String.valueOf(s.getId()) });
		close(null, db);
		return nbRows;
	}
	
	public int updateMuninPlugin(MuninPlugin p) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(KEY_MUNINPLUGINS_NAME, p.getName());
		values.put(KEY_MUNINPLUGINS_FANCYNAME, p.getFancyName());
		values.put(KEY_MUNINPLUGINS_SERVER, p.getInstalledOn().getId());
		values.put(KEY_MUNINPLUGINS_CATEGORY, p.getCategory());
		
		int nbRows = db.update(TABLE_MUNINPLUGINS, values, KEY_ID + " = ?", new String[] { String.valueOf(p.getId()) });
		close(null, db);
		return nbRows;
	}
	
	public List<MuninServer> getServers() {
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
				s.setParent(c.getString(c.getColumnIndex(KEY_MUNINSERVERS_PARENT)));
				s.setPluginsList(getPlugins(s));
				s.isPersistant = true;
				l.add(s);
			} while (c.moveToNext());
		}
		
		close(c, db);
		return l;
	}
	
	public List<MuninPlugin> getPlugins(MuninServer s) {
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
				p.setCategory(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_CATEGORY)));
				p.setInstalledOn(s);
				p.isPersistant = true;
				l.add(p);
			} while (c.moveToNext());
		}
		
		close(c, db);
		return l;
	}
	
	public MuninPlugin getPlugin(int id) {
		String selectQuery = "SELECT * FROM " + TABLE_MUNINPLUGINS 
				+ " WHERE " + KEY_ID + " = " + id;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) {
			MuninPlugin p = new MuninPlugin();
			p.setId(c.getInt(c.getColumnIndex(KEY_ID)));
			p.setName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_NAME)));
			p.setFancyName(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_FANCYNAME)));
			p.setCategory(c.getString(c.getColumnIndex(KEY_MUNINPLUGINS_CATEGORY)));
			p.setInstalledOn(getServer(c.getInt(c.getColumnIndex(KEY_MUNINPLUGINS_SERVER))));
			p.isPersistant = true;
			close(c, db);
			return p;
		}
		return null;
	}
	
	public MuninServer getServer(int id) {
		String selectQuery = "SELECT * FROM " + TABLE_MUNINSERVERS
				+ " WHERE " + KEY_ID + " = " + id;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) {
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
			s.setPluginsList(getPlugins(s));
			s.isPersistant = true;
			close(c, db);
			return s;
		}
		return null;
	}
	
	public List<Widget> getWidgets() {
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
				w.setPlugin(getPlugin(c.getInt(c.getColumnIndex(KEY_WIDGETS_PLUGIN))));
				w.setWifiOnly(c.getInt(c.getColumnIndex(KEY_WIDGETS_WIFIONLY)));
				w.isPersistant = true;
				l.add(w);
			} while (c.moveToNext());
		}
		
		close(c, db);
		return l;
	}
	
	public Widget getWidget(int widgetId) {
		String selectQuery = "SELECT * FROM " + TABLE_WIDGETS
				+ " WHERE " + KEY_WIDGETS_WIDGETID + " = " + widgetId;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) {
			Widget w = new Widget();
			w.setId(c.getInt(c.getColumnIndex(KEY_ID)));
			w.setPeriod(c.getString(c.getColumnIndex(KEY_WIDGETS_PERIOD)));
			w.setWidgetId(c.getInt(c.getColumnIndex(KEY_WIDGETS_WIDGETID)));
			w.setPlugin(getPlugin(c.getInt(c.getColumnIndex(KEY_WIDGETS_PLUGIN))));
			w.setWifiOnly(c.getInt(c.getColumnIndex(KEY_WIDGETS_WIFIONLY)));
			w.isPersistant = true;
			close(c, db);
			return w;
		}
		return null;
	}
	
	public List<Widget> getWidgets(MuninServer s) {
		List<Widget> l = new ArrayList<Widget>();
		for (Widget w : getWidgets()) {
			if (w.getPlugin().getInstalledOn().equalsApprox(s))
				l.add(w);
		}
		return l;
	}
	
	public List<Label> getLabels() {
		List<Label> list = new ArrayList<Label>();
		String selectQuery = "SELECT * FROM " + TABLE_LABELS;
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) {
			do {
				Label l = new Label();
				l.setId(c.getInt(c.getColumnIndex(KEY_ID)));
				l.setName(c.getString(c.getColumnIndex(KEY_LABELS_NAME)));
				l.setPlugins(getPlugins(l));
				list.add(l);
			} while (c.moveToNext());
		}
		close(c, db);
		return list;
	}
	
	public Label getLabel(String labelName) {
		String selectQuery = "SELECT * FROM " + TABLE_LABELS
				+ " WHERE " + KEY_LABELS_NAME + " = '" + labelName + "'";
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) {
			Label l = new Label();
			l.setId(c.getInt(c.getColumnIndex(KEY_ID)));
			l.setName(c.getString(c.getColumnIndex(KEY_LABELS_NAME)));
			l.setPlugins(getPlugins(l));
			l.isPersistant = true;
			close(c, db);
			return l;
		}
		return null;
	}
	
	public List<MuninPlugin> getPlugins(Label l) {
		List<MuninPlugin> list = new ArrayList<MuninPlugin>();
		String selectQuery = "SELECT * FROM " + TABLE_LABELSRELATIONS
				+ " WHERE " + KEY_LABELSRELATIONS_LABEL + " = " + l.getId();
		
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c != null && c.moveToFirst()) {
			do {
				MuninPlugin p = getPlugin(c.getInt(c.getColumnIndex(KEY_LABELSRELATIONS_PLUGIN)));
				list.add(p);
			} while (c.moveToNext());
		}
		close(c, db);
		return list;
	}
	
	public void deleteServer(MuninServer s) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_MUNINSERVERS, KEY_ID + " = ?", new String[] { String.valueOf(s.getId()) });
		close(null, db);
		deletePlugins(s);
	}
	
	public void deletePlugin(MuninPlugin p) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_MUNINPLUGINS, KEY_ID + " = ?", new String[] { String.valueOf(p.getId()) });
	}
	
	public void deletePlugins(MuninServer s) {
		List<MuninPlugin> l = getPlugins(s);
		for (MuninPlugin p : l) {
			deleteWidgets(p);
			deleteLabelsRelations(p);
		}
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_MUNINPLUGINS, KEY_MUNINPLUGINS_SERVER + " = ?", new String[] { String.valueOf(s.getId()) });
		close(null, db);
	}
	
	public void deleteWidgets(MuninPlugin p) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_WIDGETS, KEY_WIDGETS_PLUGIN + " = ?", new String[] { String.valueOf(p.getId()) });
		close(null, db);
	}
	
	public void deleteWidget(int appWidgetId) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_WIDGETS, KEY_WIDGETS_WIDGETID + " = ?", new String[] { String.valueOf(appWidgetId) });
		close(null, db);
	}
	
	public void deleteLabelsRelations(MuninPlugin p) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_LABELSRELATIONS, KEY_LABELSRELATIONS_PLUGIN + " = ?", new String[] { String.valueOf(p.getId()) });
		close(null, db);
	}
	
	public void deleteLabelsRelations(Label l) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_LABELSRELATIONS, KEY_LABELSRELATIONS_LABEL + " = ?", new String[] { String.valueOf(l.getId()) });
		close(null, db);
	}
	
	public void deleteLabelsRelation(MuninPlugin p, Label l) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_LABELSRELATIONS, KEY_LABELSRELATIONS_LABEL + " = ? AND " + KEY_LABELSRELATIONS_PLUGIN + " = ?", new String[] { String.valueOf(l.getId()), String.valueOf(p.getId()) });
		close(null, db);
	}
	
	public void deleteLabel(Label l) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_LABELS, KEY_ID + " = ?", new String[] { String.valueOf(l.getId()) });
		close(null, db);
		deleteLabelsRelations(l);
	}
	
	// DROP
	public void deleteLabelsRelations() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LABELSRELATIONS);
		db.execSQL(CREATE_TABLE_LABELSRELATIONS);
		close(null, db);
	}
	public void deleteLabels() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LABELS);
		db.execSQL(CREATE_TABLE_LABELS);
		close(null, db);
	}
	public void deleteWidgets() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIDGETS);
		db.execSQL(CREATE_TABLE_WIDGETS);
		close(null, db);
	}
	public void deleteMuninPlugins() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_MUNINPLUGINS);
		db.execSQL(CREATE_TABLE_MUNINPLUGINS);
		close(null, db);
	}
	public void deleteMuninServers() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_MUNINSERVERS);
		db.execSQL(CREATE_TABLE_MUNINSERVERS);
		close(null, db);
	}
}