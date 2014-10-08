package com.chteuchteu.munin.hlpr;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.MuninServer.AuthType;
import com.chteuchteu.munin.obj.MuninServer.HDGraphs;

public class JSONHelper {
	public static String getMastersJSONString(ArrayList<MuninMaster> masters, boolean includePasswords) {
		try {
			JSONObject obj = new JSONObject();
			JSONArray jsonMasters = new JSONArray();
			for (MuninMaster master : masters) {
				if (master.getChildren().size() > 0) {
					JSONObject jsonMaster = new JSONObject();
					jsonMaster.put("id", master.getId());
					jsonMaster.put("name", master.getName());
					jsonMaster.put("url", master.getUrl());
					
					// Put auth information
					MuninServer firstServer = master.getChildAt(0);
					jsonMaster.put("ssl", firstServer.getSSL());
					switch (firstServer.getAuthType()) {
						case NONE: case UNKNOWN:
							jsonMaster.put("authType", "none");
							break;
						case BASIC:
							jsonMaster.put("authType", "basic");
							jsonMaster.put("authLogin", firstServer.getAuthLogin());
							if (includePasswords)
								jsonMaster.put("authPassword", firstServer.getAuthPassword());
							break;
						case DIGEST:
							jsonMaster.put("authType", "digest");
							jsonMaster.put("authLogin", firstServer.getAuthLogin());
							if (includePasswords)
								jsonMaster.put("authPassword", firstServer.getAuthPassword());
							jsonMaster.put("authString", firstServer.getAuthString());
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
						jsonServer.put("hdGraphs", server.getHDGraphs().name());
						
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
		}
	}
	
	public static ArrayList<MuninMaster> getMastersFromJSON(JSONObject obj) {
		try {
			ArrayList<MuninMaster> muninMasters = new ArrayList<MuninMaster>();
			
			JSONArray jsonMasters = obj.getJSONArray("masters");
			for (int i=0; i<jsonMasters.length(); i++) {
				JSONObject jsonMaster = jsonMasters.getJSONObject(i);
				MuninMaster master = new MuninMaster();
				master.setId(jsonMaster.getLong("id"));
				master.setName(jsonMaster.getString("name"));
				master.setUrl(jsonMaster.getString("url"));
				
				JSONArray jsonServers = jsonMaster.getJSONArray("servers");
				for (int y=0; y<jsonServers.length(); y++) {
					JSONObject jsonServer = jsonServers.getJSONObject(y);
					MuninServer server = new MuninServer();
					
					server.setId(jsonServer.getLong("id"));
					server.setName(jsonServer.getString("name"));
					server.setServerUrl(jsonServer.getString("serverUrl"));
					server.setPosition(jsonServer.getInt("position"));
					server.setGraphURL(jsonServer.getString("graphURL"));
					server.setHDGraphs(HDGraphs.get(jsonServer.getString("hdGraphs")));
					
					String authType = jsonMaster.getString("authType");
					if (authType.equals("none")) {
						server.setAuthType(AuthType.NONE);
					} else if (authType.equals("basic")) {
						server.setAuthType(AuthType.BASIC);
						String login = jsonMaster.getString("authLogin");
						String password = "UNKNOWN";
						if (jsonMaster.has("authPassword"))
							password = jsonMaster.getString("authPassword");
						server.setAuthIds(login, password);
					} else if (authType.equals("digest")) {
						server.setAuthType(AuthType.DIGEST);
						String login = jsonMaster.getString("authLogin");
						String password = "UNKNOWN";
						if (jsonMaster.has("authPassword"))
							password = jsonMaster.getString("authPassword");
						server.setAuthIds(login, password);
						server.setAuthString(jsonMaster.getString("authString"));
					}
					
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
					master.addChild(server);
				}
				
				muninMasters.add(master);
			}
			
			return muninMasters;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
}