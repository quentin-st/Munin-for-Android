package com.chteuchteu.munin.db;

import android.database.Cursor;

import com.chteuchteu.munin.obj.Entity;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;

public class MuninMasterRepository extends BaseRepository<MuninMaster> {
    private static final String TABLE_NAME = "muninMasters";

    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_AUTHLOGIN = "authLogin";
    public static final String COLUMN_AUTHPASSWORD = "authPassword";
    public static final String COLUMN_SSL = "SSL";
    public static final String COLUMN_AUTHTYPE = "authType";
    public static final String COLUMN_AUTHSTRING = "authString";
    public static final String COLUMN_HDGRAPHS = "hdGraphs";

    public static final String CREATE_TABLE
            = "CREATE TABLE " + TABLE_NAME + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_NAME + " TEXT,"
            + COLUMN_URL + " TEXT,"
            + COLUMN_AUTHLOGIN + " TEXT,"
            + COLUMN_AUTHPASSWORD + " TEXT,"
            + COLUMN_AUTHTYPE + " INTEGER,"
            + COLUMN_AUTHSTRING + " TEXT,"
            + COLUMN_SSL + " INTEGER,"
            + COLUMN_HDGRAPHS + " TEXT)";


    public MuninMasterRepository(Database database) {
        super(database);
    }


    @Override
    public long save(MuninMaster master) {
        return save(
                master.getId(),
                new String[] {
                        COLUMN_NAME,
                        COLUMN_URL,
                        COLUMN_AUTHLOGIN,
                        COLUMN_AUTHPASSWORD,
                        COLUMN_SSL,
                        COLUMN_AUTHTYPE,
                        COLUMN_AUTHSTRING,
                        COLUMN_HDGRAPHS
                },
                new Object[] {
                        master.getName(),
                        master.getUrl(),
                        master.getAuthLogin(),
                        master.getAuthPassword(),
                        master.getSSL(),
                        master.getAuthType().getVal(),
                        master.getAuthString(),
                        master.isDynazoomAvailable().getVal()
                }
        );
    }

    @Override
    public MuninMaster cursorToT(Cursor cursor, Entity parent) {
        MuninMaster master = new MuninMaster();
        master.setId(getLong(cursor, COLUMN_ID));
        master.setName(getString(cursor, COLUMN_NAME));
        master.setUrl(getString(cursor, COLUMN_URL));
        master.setAuthIds(
                getString(cursor, COLUMN_AUTHLOGIN),
                getString(cursor, COLUMN_AUTHPASSWORD),
                MuninMaster.AuthType.get(getInteger(cursor, COLUMN_AUTHTYPE))
        );
        master.setAuthString(getString(cursor, COLUMN_AUTHSTRING));
        master.setSSL(getBoolean(cursor, COLUMN_SSL));
        master.setDynazoomAvailable(MuninMaster.DynazoomAvailability.get(getString(cursor, COLUMN_HDGRAPHS)));

        // Get children
        MuninNodeRepository muninNodeRepo = (MuninNodeRepository) this.database.getRepository(MuninNode.class);
        for (MuninNode node : muninNodeRepo.findByParent(master))
            master.addChild(node);

        return master;
    }

    @Override
    public String[] getAllColumns() {
        return new String[] {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_URL,
                COLUMN_AUTHLOGIN,
                COLUMN_AUTHPASSWORD,
                COLUMN_SSL,
                COLUMN_AUTHTYPE,
                COLUMN_AUTHSTRING,
                COLUMN_HDGRAPHS
        };
    }

    @Override
    public String getTable() {
        return TABLE_NAME;
    }
}
