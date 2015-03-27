package com.chteuchteu.munin.hlpr;

import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninMaster.DynazoomAvailability;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninMaster.AuthType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Serialization util class
 * Used for import/export
 *
 * JSON keys are "obfuscated" to reduce message length.
 *  This is not an issue since output json string isn't made to be human-readable
 *  Separate JSON keys of the same group ("MASTER", "SERVER", "PLUGIN") must be unique.
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

    private static final String SERVER_ID = "i";
    private static final String SERVER_NAME = "n";
    private static final String SERVER_SERVERURL = "s";
    private static final String SERVER_GRAPHURL = "g";
    private static final String SERVER_HDGRAPHURL = "hdG";
    private static final String SERVER_POSITION = "p";

    private static final String PLUGIN_ID = "i";
    private static final String PLUGIN_NAME = "n";
    private static final String PLUGIN_FANCYNAME = "f";
    private static final String PLUGIN_CATEGORY = "c";
    private static final String PLUGIN_PLUGINPAGEURL = "p";

    private static final String PLUGINS = "ps";
    private static final String SERVERS = "srvss";
    private static final String MASTERS = "ms";

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
					
					JSONArray jsonServers = new JSONArray();
					for (MuninServer server : master.getChildren()) {
						JSONObject jsonServer = new JSONObject();
						
						jsonServer.put(SERVER_ID, server.getId());
						jsonServer.put(SERVER_NAME, server.getName());
						jsonServer.put(SERVER_SERVERURL, server.getServerUrl());
						jsonServer.put(SERVER_GRAPHURL, server.getGraphURL());
						jsonServer.put(SERVER_HDGRAPHURL, server.getHdGraphURL());
						jsonServer.put(SERVER_POSITION, server.getPosition());

						JSONArray jsonPlugins = new JSONArray();
						for (MuninPlugin plugin : server.getPlugins()) {
							JSONObject jsonPlugin = new JSONObject();
							
							jsonPlugin.put(PLUGIN_ID, plugin.getId());
							jsonPlugin.put(PLUGIN_NAME, plugin.getName());
							jsonPlugin.put(PLUGIN_FANCYNAME, plugin.getFancyName());
							jsonPlugin.put(PLUGIN_CATEGORY, plugin.getCategory());
							jsonPlugin.put(PLUGIN_PLUGINPAGEURL, plugin.getPluginPageUrl());
							
							jsonPlugins.put(jsonPlugin);
						}
						jsonServer.put(PLUGINS, jsonPlugins);
						
						jsonServers.put(jsonServer);
					}
					jsonMaster.put(SERVERS, jsonServers);
					
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
				
				JSONArray jsonServers = jsonMaster.getJSONArray(SERVERS);
				for (int y=0; y<jsonServers.length(); y++) {
					JSONObject jsonServer = jsonServers.getJSONObject(y);
					MuninServer server = new MuninServer();
					
					server.setId(jsonServer.getLong(SERVER_ID));
					server.setName(jsonServer.getString(SERVER_NAME));
					server.setServerUrl(jsonServer.getString(SERVER_SERVERURL));
					server.setGraphURL(jsonServer.getString(SERVER_GRAPHURL));
					server.setHdGraphURL(jsonServer.getString(SERVER_HDGRAPHURL));
					server.setPosition(jsonServer.getInt(SERVER_POSITION));

					JSONArray jsonPlugins = jsonServer.getJSONArray(PLUGINS);
					for (int z=0; z<jsonPlugins.length(); z++) {
						JSONObject jsonPlugin = jsonPlugins.getJSONObject(z);
						MuninPlugin plugin = new MuninPlugin();
						plugin.setInstalledOn(server);
						plugin.setId(jsonPlugin.getLong(PLUGIN_ID));
						plugin.setName(jsonPlugin.getString(PLUGIN_NAME));
						plugin.setFancyName(jsonPlugin.getString(PLUGIN_FANCYNAME));
						plugin.setCategory(jsonPlugin.getString(PLUGIN_CATEGORY));
						plugin.setPluginPageUrl(jsonPlugin.getString(PLUGIN_PLUGINPAGEURL));
						server.getPlugins().add(plugin);
					}
					
					server.setParent(master);
				}
				
				muninMasters.add(master);
			}
			
			return muninMasters;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
