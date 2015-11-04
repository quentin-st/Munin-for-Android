package com.chteuchteu.munin.db;

import android.database.Cursor;

import com.chteuchteu.munin.obj.Entity;
import com.chteuchteu.munin.obj.Label;

public class LabelRepository extends BaseRepository<Label> {
	public static final String TABLE_NAME = "labels";

	public static final String COLUMN_NAME = "name";

	public static final String CREATE_TABLE
			= "CREATE TABLE " + TABLE_NAME + " ("
			+ COLUMN_ID + " INTEGER PRIMARY KEY,"
			+ COLUMN_NAME + " TEXT)";


	public LabelRepository(Database database) {
		super(database);
	}

	@Override
	public long save(Label label) {
		return save(
				label.getId(),
				new String[] {
						COLUMN_NAME
				},
				new Object[] {
						label.getName()
				}
		);
	}

	@Override
	public Label cursorToT(Cursor cursor, Entity parent) {
		Label label = new Label();
		label.setId(getLong(cursor, COLUMN_ID));
		label.setName(getString(cursor, COLUMN_NAME));
		return label;
	}

	@Override
	public String[] getAllColumns() {
		return new String[] {
				COLUMN_ID,
				COLUMN_NAME
		};
	}

	@Override
	public String getTable() { return TABLE_NAME; }
}
