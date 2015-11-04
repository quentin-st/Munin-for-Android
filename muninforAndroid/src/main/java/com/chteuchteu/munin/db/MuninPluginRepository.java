package com.chteuchteu.munin.db;

import android.database.Cursor;

import com.chteuchteu.munin.obj.Entity;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;

public class MuninPluginRepository extends BaseRepository<MuninPlugin> {
	private static final String TABLE_NAME = "muninPlugins";

	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_FANCYNAME = "fancyName";
	public static final String COLUMN_NODE = "server";
	public static final String COLUMN_CATEGORY = "category";
	public static final String COLUMN_PLUGINPAGEURL = "pluginPageUrl";

	public static final String CREATE_TABLE
			= "CREATE TABLE " + TABLE_NAME + " ("
			+ COLUMN_ID + " INTEGER PRIMARY KEY,"
			+ COLUMN_NAME + " TEXT,"
			+ COLUMN_FANCYNAME + " TEXT,"
			+ COLUMN_NODE + " INTEGER,"
			+ COLUMN_CATEGORY + " TEXT,"
			+ COLUMN_PLUGINPAGEURL + " TEXT)";


	public MuninPluginRepository(Database database) {
		super(database);
	}


	@Override
	public long save(MuninPlugin plugin) {
		return save(
				plugin.getId(),
				new String[] {
						COLUMN_NAME,
						COLUMN_FANCYNAME,
						COLUMN_NODE,
						COLUMN_CATEGORY,
						COLUMN_PLUGINPAGEURL
				},
				new Object[] {
						plugin.getName(),
						plugin.getFancyName(),
						plugin.getInstalledOn().getId(),
						plugin.getCategory(),
						plugin.getPluginPageUrl()
				}
		);
	}

	@Override
	public MuninPlugin cursorToT(Cursor cursor, Entity parent) {
		MuninPlugin plugin = new MuninPlugin();
		plugin.setId(getLong(cursor, COLUMN_ID));
		plugin.setName(getString(cursor, COLUMN_NAME));
		plugin.setFancyName(getString(cursor, COLUMN_FANCYNAME));
		plugin.setCategory(getString(cursor, COLUMN_CATEGORY));
		plugin.setPluginPageUrl(getString(cursor, COLUMN_PLUGINPAGEURL));
		plugin.setInstalledOn((MuninNode) parent); // TODO
		return plugin;
	}

	@Override
	public String[] getAllColumns() {
		return new String[] {
				COLUMN_ID,
				COLUMN_NAME,
				COLUMN_FANCYNAME,
				COLUMN_NODE,
				COLUMN_CATEGORY,
				COLUMN_PLUGINPAGEURL
		};
	}

	@Override
	public String getTable() {
		return TABLE_NAME;
	}
}
