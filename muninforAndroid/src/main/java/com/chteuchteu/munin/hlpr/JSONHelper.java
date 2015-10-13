package com.chteuchteu.munin.hlpr;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.Grid;
import com.chteuchteu.munin.obj.GridItem;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninMaster.DynazoomAvailability;
import com.chteuchteu.munin.obj.MuninNode;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninMaster.AuthType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Serialization util class
 * Used for import/export
 *
 * JSON keys are "obfuscated" to reduce message length.
 *  This is not an issue since output json string isn't made to be human-readable
 *  Separate JSON keys of the same group ("MASTER", "NODE", "PLUGIN") must be unique.
 */
public class JSONHelper {
    private static final String MASTER_ID = "i";
    private static final String MASTER_NAME = "n";
    private static final String MASTER_URL = "u";
    private static final String MASTER_HDGRAPHS = "h";
    private static final String MASTER_SSL = "s";
    private static final String MASTER_AUTHTYPE = "aT";
    private static final String MASTER_AUTHLOGIN = "aL";
    private static final String MASTER_AUTHPASSWORD = "aP";
    private static final String MASTER_AUTHSTRING = "aS";

    private static final String NODE_ID = "i";
    private static final String NODE_NAME = "n";
    private static final String NODE_NODEURL = "s";
    private static final String NODE_GRAPHURL = "g";
    private static final String NODE_HDGRAPHURL = "hdG";
    private static final String NODE_POSITION = "p";

    private static final String PLUGIN_ID = "i";
    private static final String PLUGIN_NAME = "n";
    private static final String PLUGIN_FANCYNAME = "f";
    private static final String PLUGIN_CATEGORY = "c";
    private static final String PLUGIN_PLUGINPAGEURL = "p";

    private static final String PLUGINS = "ps";
    private static final String NODES = "srvss";
    private static final String MASTERS = "ms";

	private static final String GRIDS = "gs";
	private static final String GRID_ID = "i";
	private static final String GRID_NAME = "n";
	private static final String GRID_ITEMS = "is";

	private static final String GRIDITEM_ID = "i";
	private static final String GRIDITEM_X = "x";
	private static final String GRIDITEM_Y = "y";
	private static final String GRIDITEM_PLUGINPAGEURL = "ppu";

    /**
	 * Serializes the Masters objects tree
	 * Encrypts the password using the Util.Encryption util methods
	 * @param masters Masters to be serialized
	 * @param seed Seed used in order to encrypt passwords
	 * @return JSON representation of the masters parameter
	 */
	public static String getMastersJSONString(List<MuninMaster> masters, String seed) {
		try {
			JSONObject obj = new JSONObject();
			JSONArray jsonMasters = new JSONArray();

			for (MuninMaster master : masters) {
				if (master.getChildren().size() > 0) {
					JSONObject jsonMaster = new JSONObject();
					jsonMaster.put(MASTER_ID, master.getId());
					jsonMaster.put(MASTER_NAME, master.getName());
					jsonMaster.put(MASTER_URL, master.getUrl());
					jsonMaster.put(MASTER_HDGRAPHS, master.isDynazoomAvailable().getVal());
					jsonMaster.put(MASTER_SSL, master.getSSL());
					switch (master.getAuthType()) {
						case NONE: case UNKNOWN:
							jsonMaster.put(MASTER_AUTHTYPE, "none");
							break;
						case BASIC:
							jsonMaster.put(MASTER_AUTHTYPE, "basic");
							jsonMaster.put(MASTER_AUTHLOGIN, master.getAuthLogin());
							String basicPassword = master.getAuthPassword();
							String encryptedBasicPassword = EncryptionHelper.encrypt(seed, basicPassword);
							jsonMaster.put(MASTER_AUTHPASSWORD, encryptedBasicPassword);
							break;
						case DIGEST:
							jsonMaster.put(MASTER_AUTHTYPE, "digest");
							jsonMaster.put(MASTER_AUTHLOGIN, master.getAuthLogin());
							String digestPassword = master.getAuthPassword();
							String encryptedDigestPassword = EncryptionHelper.encrypt(seed, digestPassword);
							jsonMaster.put(MASTER_AUTHPASSWORD, encryptedDigestPassword);
							jsonMaster.put(MASTER_AUTHSTRING, master.getAuthString());
							break;
					}
					
					JSONArray jsonNodes = new JSONArray();
					for (MuninNode node : master.getChildren()) {
						JSONObject jsonNode = new JSONObject();
						
						jsonNode.put(NODE_ID, node.getId());
						jsonNode.put(NODE_NAME, node.getName());
						jsonNode.put(NODE_NODEURL, node.getUrl());
						jsonNode.put(NODE_GRAPHURL, node.getGraphURL());
						jsonNode.put(NODE_HDGRAPHURL, node.getHdGraphURL());
						jsonNode.put(NODE_POSITION, node.getPosition());

						JSONArray jsonPlugins = new JSONArray();
						for (MuninPlugin plugin : node.getPlugins()) {
							JSONObject jsonPlugin = new JSONObject();
							
							jsonPlugin.put(PLUGIN_ID, plugin.getId());
							jsonPlugin.put(PLUGIN_NAME, plugin.getName());
							jsonPlugin.put(PLUGIN_FANCYNAME, plugin.getFancyName());
							jsonPlugin.put(PLUGIN_CATEGORY, plugin.getCategory());
							jsonPlugin.put(PLUGIN_PLUGINPAGEURL, plugin.getPluginPageUrl());
							
							jsonPlugins.put(jsonPlugin);
						}
						jsonNode.put(PLUGINS, jsonPlugins);
						
						jsonNodes.put(jsonNode);
					}
					jsonMaster.put(NODES, jsonNodes);
					
					jsonMasters.put(jsonMaster);
				}
			}
			
			obj.put(MASTERS, jsonMasters);
			return obj.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}
	
	public static List<MuninMaster> getMastersFromJSON(JSONObject obj, String seed) {
		try {
			List<MuninMaster> muninMasters = new ArrayList<>();
			
			JSONArray jsonMasters = obj.getJSONArray(MASTERS);
			for (int i=0; i<jsonMasters.length(); i++) {
				JSONObject jsonMaster = jsonMasters.getJSONObject(i);
				MuninMaster master = new MuninMaster();
				master.setId(jsonMaster.getLong(MASTER_ID));
				master.setName(jsonMaster.getString(MASTER_NAME));
				master.setUrl(jsonMaster.getString(MASTER_URL));
				master.setDynazoomAvailable(DynazoomAvailability.get(jsonMaster.getString(MASTER_HDGRAPHS)));
				String authType = jsonMaster.getString(MASTER_AUTHTYPE);
				switch (authType) {
					case "none":
						master.setAuthType(AuthType.NONE);
						break;
					case "basic": {
						master.setAuthType(AuthType.BASIC);
						String login = jsonMaster.getString(MASTER_AUTHLOGIN);
						String encryptedBasicPassword = jsonMaster.getString(MASTER_AUTHPASSWORD);
						String decryptedBasicPassword = EncryptionHelper.decrypt(seed, encryptedBasicPassword);
						master.setAuthIds(login, decryptedBasicPassword);
						break;
					}
					case "digest": {
						master.setAuthType(AuthType.DIGEST);
						String login = jsonMaster.getString(MASTER_AUTHLOGIN);
						String encryptedDigestPassword = jsonMaster.getString(MASTER_AUTHPASSWORD);
						String decryptedDigestPassword = EncryptionHelper.decrypt(seed, encryptedDigestPassword);
						master.setAuthIds(login, decryptedDigestPassword);
						master.setAuthString(jsonMaster.getString(MASTER_AUTHSTRING));
						break;
					}
				}
				
				JSONArray jsonNodes = jsonMaster.getJSONArray(NODES);
				for (int y=0; y<jsonNodes.length(); y++) {
					JSONObject jsonNode = jsonNodes.getJSONObject(y);
					MuninNode node = new MuninNode();
					
					node.setId(jsonNode.getLong(NODE_ID));
					node.setName(jsonNode.getString(NODE_NAME));
					node.setUrl(jsonNode.getString(NODE_NODEURL));
					node.setGraphURL(jsonNode.getString(NODE_GRAPHURL));
					node.setHdGraphURL(jsonNode.getString(NODE_HDGRAPHURL));
					node.setPosition(jsonNode.getInt(NODE_POSITION));

					JSONArray jsonPlugins = jsonNode.getJSONArray(PLUGINS);
					for (int z=0; z<jsonPlugins.length(); z++) {
						JSONObject jsonPlugin = jsonPlugins.getJSONObject(z);
						MuninPlugin plugin = new MuninPlugin();
						plugin.setInstalledOn(node);
						plugin.setId(jsonPlugin.getLong(PLUGIN_ID));
						plugin.setName(jsonPlugin.getString(PLUGIN_NAME));
						plugin.setFancyName(jsonPlugin.getString(PLUGIN_FANCYNAME));
						plugin.setCategory(jsonPlugin.getString(PLUGIN_CATEGORY));
						plugin.setPluginPageUrl(jsonPlugin.getString(PLUGIN_PLUGINPAGEURL));
						node.getPlugins().add(plugin);
					}
					
					node.setParent(master);
				}
				
				muninMasters.add(master);
			}
			
			return muninMasters;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getGridsJSONString(List<Grid> grids) {
		try {
			JSONObject obj = new JSONObject();
			JSONArray jsonGrids = new JSONArray();

			for (Grid grid : grids) {
				JSONObject jsonGrid = new JSONObject();

				jsonGrid.put(GRID_ID, grid.getId());
				jsonGrid.put(GRID_NAME, grid.getName());

				JSONArray jsonItems = new JSONArray();
				for (GridItem item : grid.getItems()) {
					JSONObject jsonItem = new JSONObject();
					jsonItem.put(GRIDITEM_ID, item.getId());
					jsonItem.put(GRIDITEM_X, item.getX());
					jsonItem.put(GRIDITEM_Y, item.getY());
					jsonItem.put(GRIDITEM_PLUGINPAGEURL, item.getPluginPageUrl());

					jsonItems.put(jsonItem);
				}

				jsonGrid.put(GRID_ITEMS, jsonItems);
				jsonGrids.put(jsonGrid);
			}

			obj.put(GRIDS, jsonGrids);
			return obj.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}

	public static List<Grid> getGridsFromJSON(JSONObject obj, MuninFoo muninFoo) {
		try {
			List<Grid> grids = new ArrayList<>();

			JSONArray jsonGrids = obj.getJSONArray(GRIDS);
			for (int i=0; i<jsonGrids.length(); i++) {
				JSONObject jsonGrid = jsonGrids.getJSONObject(i);
				Grid grid = new Grid(jsonGrid.getString(GRID_NAME));

				List<GridItem> gridItems = new ArrayList<>();
				JSONArray jsonGridItems = jsonGrid.getJSONArray(GRID_ITEMS);
				for (int y=0; y<jsonGridItems.length(); y++) {
					JSONObject jsonGridItem = jsonGridItems.getJSONObject(y);
					GridItem item = new GridItem(grid, null);

					// Try to find plugin from its PluginPageUrl attribute
					String pluginPageUrl = jsonGridItem.getString(GRIDITEM_PLUGINPAGEURL);
					item.setPluginPageUrl(pluginPageUrl);

					MuninPlugin plugin = muninFoo.getPlugin(pluginPageUrl);

					if (plugin != null) {
						item.setPlugin(plugin);
					} else {
						item.setPlugin(null);
						item.setDetached(true);
					}

					item.setId(jsonGridItem.getLong(GRIDITEM_ID));
					item.setX(jsonGridItem.getInt(GRIDITEM_X));
					item.setY(jsonGridItem.getInt(GRIDITEM_Y));

					gridItems.add(item);
				}

				grid.setItems(gridItems);
				grids.add(grid);
			}

			return grids;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
