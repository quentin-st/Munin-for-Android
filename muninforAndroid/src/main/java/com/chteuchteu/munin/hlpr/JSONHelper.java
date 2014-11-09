package com.chteuchteu.munin.hlpr;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninMaster.HDGraphs;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Serialization util class
 * Used for import/export
 */
public class JSONHelper {
	/**
	 * Serializes the Masters objects tree
	 * Encrypts the password using the Util.Encryption util methods
	 * @param masters Masters to be serialized
	 * @param seed Seed used in order to encrypt passwords
	 * @return JSON representation of the masters parameter
	 */
	public static String getMastersJSONString(ArrayList<MuninMaster> masters, String seed) {
		try {
			JSONObject obj = new JSONObject();
			JSONArray jsonMasters = new JSONArray();
			EncryptionHelper encryptionHelper = new EncryptionHelper();

			for (MuninMaster master : masters) {
				if (master.getChildren().size() > 0) {
					JSONObject jsonMaster = new JSONObject();
					jsonMaster.put("id", master.getId());
					jsonMaster.put("name", master.getName());
					jsonMaster.put("url", master.getUrl());
					jsonMaster.put("hdGraphs", master.getHDGraphs().getVal());
					jsonMaster.put("ssl", master.getSSL());
					switch (master.getAuthType()) {
						case NONE: case UNKNOWN:
							jsonMaster.put("authType", "none");
							break;
						case BASIC:
							jsonMaster.put("authType", "basic");
							jsonMaster.put("authLogin", master.getAuthLogin());
							String basicPassword = master.getAuthPassword();
							String encryptedBasicPassword = encryptionHelper.encrypt(seed, basicPassword);
							MuninFoo.log("encryptedPassword = '" + encryptedBasicPassword + "'");
							jsonMaster.put("authPassword", encryptedBasicPassword);
							break;
						case DIGEST:
							jsonMaster.put("authType", "digest");
							jsonMaster.put("authLogin", master.getAuthLogin());
							String digestPassword = master.getAuthPassword();
							String encryptedDigestPassword = encryptionHelper.encrypt(seed, digestPassword);
							jsonMaster.put("authPassword", encryptedDigestPassword);
							jsonMaster.put("authString", master.getAuthString());
							break;
					}
					
					JSONArray jsonServers = new JSONArray();
					for (MuninServer server : master.getChildren()) {
						JSONObject jsonServer = new JSONObject();
						
						jsonServer.put("id", server.getId());
						jsonServer.put("name", server.getName());
						jsonServer.put("serverUrl", server.getServerUrl());
						jsonServer.put("position", server.getPosition());
						jsonServer.put("graphURL", server.getGraphURL());
						
						JSONArray jsonPlugins = new JSONArray();
						for (MuninPlugin plugin : server.getPlugins()) {
							JSONObject jsonPlugin = new JSONObject();
							
							jsonPlugin.put("id", plugin.getId());
							jsonPlugin.put("name", plugin.getName());
							jsonPlugin.put("fancyName", plugin.getFancyName());
							jsonPlugin.put("category", plugin.getCategory());
							jsonPlugin.put("pluginPageUrl", plugin.getPluginPageUrl());
							
							jsonPlugins.put(jsonPlugin);
						}
						jsonServer.put("plugins", jsonPlugins);
						
						jsonServers.put(jsonServer);
					}
					jsonMaster.put("servers", jsonServers);
					
					jsonMasters.put(jsonMaster);
				}
			}
			
			obj.put("masters", jsonMasters);
			return obj.toString();
		} catch (JSONException ex) {
			ex.printStackTrace();
			return "";
		} catch (NullPointerException ex) {
			ex.printStackTrace();
			return "";
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static ArrayList<MuninMaster> getMastersFromJSON(JSONObject obj, String seed) {
		try {
			ArrayList<MuninMaster> muninMasters = new ArrayList<MuninMaster>();
			EncryptionHelper encryptionHelper = new EncryptionHelper();
			
			JSONArray jsonMasters = obj.getJSONArray("masters");
			for (int i=0; i<jsonMasters.length(); i++) {
				JSONObject jsonMaster = jsonMasters.getJSONObject(i);
				MuninMaster master = new MuninMaster();
				master.setId(jsonMaster.getLong("id"));
				master.setName(jsonMaster.getString("name"));
				master.setUrl(jsonMaster.getString("url"));
				master.setHDGraphs(HDGraphs.get(jsonMaster.getString("hdGraphs")));
				String authType = jsonMaster.getString("authType");
				if (authType.equals("none")) {
					master.setAuthType(AuthType.NONE);
				} else if (authType.equals("basic")) {
					master.setAuthType(AuthType.BASIC);
					String login = jsonMaster.getString("authLogin");
					String encryptedBasicPassword = jsonMaster.getString("authPassword");
					MuninFoo.log("EncryptedBasicPassword = '" + encryptedBasicPassword + "'");
					String decryptedBasicPassword = encryptionHelper.decrypt(seed, encryptedBasicPassword);
					master.setAuthIds(login, decryptedBasicPassword);
				} else if (authType.equals("digest")) {
					master.setAuthType(AuthType.DIGEST);
					String login = jsonMaster.getString("authLogin");
					String encryptedDigestPassword = jsonMaster.getString("authPassword");
					String decryptedDigestPassword = encryptionHelper.decrypt(seed, encryptedDigestPassword);
					master.setAuthIds(login, decryptedDigestPassword);
					master.setAuthString(jsonMaster.getString("authString"));
				}
				
				JSONArray jsonServers = jsonMaster.getJSONArray("servers");
				for (int y=0; y<jsonServers.length(); y++) {
					JSONObject jsonServer = jsonServers.getJSONObject(y);
					MuninServer server = new MuninServer();
					
					server.setId(jsonServer.getLong("id"));
					server.setName(jsonServer.getString("name"));
					server.setServerUrl(jsonServer.getString("serverUrl"));
					server.setPosition(jsonServer.getInt("position"));
					server.setGraphURL(jsonServer.getString("graphURL"));
					
					JSONArray jsonPlugins = jsonServer.getJSONArray("plugins");
					for (int z=0; z<jsonPlugins.length(); z++) {
						JSONObject jsonPlugin = jsonPlugins.getJSONObject(z);
						MuninPlugin plugin = new MuninPlugin();
						plugin.setInstalledOn(server);
						plugin.setId(jsonPlugin.getLong("id"));
						plugin.setName(jsonPlugin.getString("name"));
						plugin.setFancyName(jsonPlugin.getString("fancyName"));
						plugin.setCategory(jsonPlugin.getString("category"));
						plugin.setPluginPageUrl(jsonPlugin.getString("pluginPageUrl"));
						server.getPlugins().add(plugin);
					}
					
					server.setParent(master);
				}
				
				muninMasters.add(master);
			}
			
			return muninMasters;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}