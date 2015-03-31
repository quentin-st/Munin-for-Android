package com.chteuchteu.munin.hlpr;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.exc.ImportExportWebserviceException;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_Servers;
import com.crashlytics.android.Crashlytics;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ImportExportHelper {
	public static final String ENCRYPTION_SEED = "786547E9431EE";

    /**
     * Default import/export target URI. Can be overriden by user.
     */
    public static final String IMPORT_EXPORT_URI = "http://www.munin-for-android.com/ws/importExport.php";
    public static final int IMPORT_EXPORT_VERSION = 1;
	
	public static class Export {
		private static String sendExportRequest(Context context, String jsonString) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(getImportExportServerUrl(context) + "?export");
			
			try {
				List<NameValuePair> nameValuePairs = new ArrayList<>(2);
				nameValuePairs.add(new BasicNameValuePair("dataString", jsonString));
				nameValuePairs.add(new BasicNameValuePair("version", String.valueOf(IMPORT_EXPORT_VERSION)));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				
				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				StringBuilder builder = new StringBuilder();
				for (String line; (line = reader.readLine()) != null;)
				    builder.append(line).append("\n");
				
				String body = builder.toString();
				
				JSONObject jsonResult = new JSONObject(body);

				boolean success = jsonResult.getBoolean("success");

				if (success) {
					return jsonResult.getString("password");
				} else {
					String error = jsonResult.getString("error");
					Crashlytics.logException(new ImportExportWebserviceException("Error is " + error));
				}
				
				return null;
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		public static class ExportRequestMaker extends AsyncTask<Void, Integer, Void> {
			private String jsonString;
			private boolean result;
			private String pswd;
			private ProgressDialog progressDialog;
			private Activity_Servers activity_servers;
			private Context context;
			
			public ExportRequestMaker (String jsonString, Activity_Servers activity) {
				super();
				this.jsonString = jsonString;
				this.result = false;
				this.activity_servers = activity;
				this.context = activity;
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				
				this.progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
			}
			
			@Override
			protected Void doInBackground(Void... arg0) {
                try {
                    pswd = sendExportRequest(context, jsonString);
                    result = pswd != null && !pswd.equals("");
                } catch (IllegalStateException ex) {
                    // Thrown when the URL isn't valid
                    result = false;
                }
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res) {
				this.progressDialog.dismiss();
				
				if (result)
					activity_servers.onExportSuccess(pswd);
				else
					activity_servers.onExportError();
			}
		}
	}
	
	public static class Import {
		public static void applyImportation(Context context, JSONObject jsonObject, String code) {
			List<MuninMaster> newMasters = JSONHelper.getMastersFromJSON(jsonObject, code);
			removeIds(newMasters);

			MuninFoo muninFooRef = MuninFoo.getInstance(context);
			
			// Add masters
			for (MuninMaster newMaster : newMasters) {
				// Check if master already added
				if (!muninFooRef.contains(newMaster)) {
					muninFooRef.getMasters().add(newMaster);
					for (MuninServer server : newMaster.getChildren())
						muninFooRef.addServer(server);

					muninFooRef.sqlite.insertMuninMaster(newMaster);
				}
			}
		}
		
		private static JSONObject sendImportRequest(Context context, String code) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(getImportExportServerUrl(context)+"?import");

			try {
				List<NameValuePair> nameValuePairs = new ArrayList<>(2);
				nameValuePairs.add(new BasicNameValuePair("pswd", code));
				nameValuePairs.add(new BasicNameValuePair("version", String.valueOf(IMPORT_EXPORT_VERSION)));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				
				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				StringBuilder builder = new StringBuilder();
				for (String line; (line = reader.readLine()) != null;)
				    builder.append(line).append("\n");
				
				JSONObject jsonResult = new JSONObject(builder.toString());
				
				boolean success = jsonResult.getBoolean("success");
				
				if (success) {
					return jsonResult.getJSONArray("data").getJSONObject(0);
				} else {
					String error = jsonResult.getString("error");
					if (!error.equals("006")) // Wrong password
						Crashlytics.logException(new ImportExportWebserviceException("Error is " + error));
				}
				
				return null;
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		public static class ImportRequestMaker extends AsyncTask<Void, Integer, Void> {
			private JSONObject jsonObject;
			private String code;
			private boolean result;
			private Activity_Servers activity_servers;
			private Context context;
			private ProgressDialog progressDialog;
			
			public ImportRequestMaker (String code, Activity_Servers activity) {
				super();
				this.code = code;
				this.result = false;
				this.activity_servers = activity;
				this.context = activity;
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				
				this.progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
			}
			
			@Override
			protected Void doInBackground(Void... arg0) {
                try {
                    jsonObject = sendImportRequest(context, code);
                    result = jsonObject != null;
                } catch (IllegalStateException ex) {
                    // Thrown when the URL isn't valid
                    result = false;
                }
				
				if (result)
					applyImportation(context, jsonObject, ENCRYPTION_SEED);
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res) {
				this.progressDialog.dismiss();
				
				if (result)
					activity_servers.onImportSuccess();
				else
					activity_servers.onImportError();
			}
		}
	}
	
	/**
	 * Removes the ids contained in the structure given as parameter
	 */
	private static void removeIds(List<MuninMaster> masters) {
		for (MuninMaster master : masters) {
			master.setId(-1);
			for (MuninServer server : master.getChildren()) {
				server.setId(-1);
				server.isPersistant = false;
				for (MuninPlugin plugin : server.getPlugins())
					plugin.setId(-1);
			}
		}
	}

    public static String getImportExportServerUrl(Context context) {
        return Util.getPref(context, Util.PrefKeys.ImportExportServer, IMPORT_EXPORT_URI);
    }
}
