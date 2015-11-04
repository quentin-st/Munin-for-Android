package com.chteuchteu.munin.db;

import android.database.Cursor;

import com.chteuchteu.munin.obj.Entity;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;

public class MuninNodeRepository extends BaseRepository<MuninNode> {
    private static final String TABLE_NAME = "muninServers";

    public static final String COLUMN_NODEURL = "serverUrl";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_GRAPHURL = "graphURL";
    public static final String COLUMN_HDGRAPHURL = "hdGraphURL";
    public static final String COLUMN_POSITION = "position";
    public static final String COLUMN_MASTER = "master";

    public static final String CREATE_TABLE
            = "CREATE TABLE " + TABLE_NAME + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_NODEURL + " TEXT,"
            + COLUMN_NAME + " TEXT,"
            + COLUMN_GRAPHURL + " TEXT,"
            + COLUMN_HDGRAPHURL + " TEXT,"
            + COLUMN_POSITION + " INTEGER,"
            + COLUMN_MASTER + " INTEGER)";


    public MuninNodeRepository(Database database) {
        super(database);
    }


    @Override
    public long save(MuninNode node) {
        return save(
                node.getId(),
                new String[] {
                        COLUMN_NODEURL,
                        COLUMN_NAME,
                        COLUMN_GRAPHURL,
                        COLUMN_HDGRAPHURL,
                        COLUMN_POSITION,
                        COLUMN_MASTER
                },
                new Object[] {
                        node.getUrl(),
                        node.getName(),
                        node.getGraphURL(),
                        node.getHdGraphURL(),
                        node.getPosition(),
                        node.master.getId()
                }
        );
    }

    @Override
    public MuninNode cursorToT(Cursor cursor, Entity parent) {
        MuninNode node = new MuninNode();
        node.setId(getLong(cursor, COLUMN_ID));
        node.setUrl(getString(cursor, COLUMN_NODEURL));
        node.setName(getString(cursor, COLUMN_NAME));
        node.setGraphURL(getString(cursor, COLUMN_GRAPHURL));
        node.setHdGraphURL(getString(cursor, COLUMN_HDGRAPHURL));
        node.setParent((MuninMaster) parent);
        node.setPosition(getInteger(cursor, COLUMN_POSITION));

        // Get plugins
        MuninPluginRepository muninPluginRepos = (MuninPluginRepository) this.database.getRepository(MuninPlugin.class);
        node.setPluginsList(muninPluginRepos.findByParent(node));

        return node;
    }

    @Override
    public String[] getAllColumns() {
        return new String[] {
                COLUMN_ID,
                COLUMN_NODEURL,
                COLUMN_NAME,
                COLUMN_GRAPHURL,
                COLUMN_HDGRAPHURL,
                COLUMN_POSITION,
                COLUMN_MASTER
        };
    }

    @Override
    public String getTable() {
        return TABLE_NAME;
    }
}
