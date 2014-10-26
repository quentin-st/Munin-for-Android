package com.chteuchteu.munin.hlpr;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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
	public static final String ENCRYPTION_SEED = "$$MFA!!";
	
	public static class Export {
		private static String sendExportRequest(String jsonString) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(MuninFoo.IMPORT_EXPORT_URI+"?export");
			
			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				nameValuePairs.add(new BasicNameValuePair("dataString", jsonString));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				
				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				StringBuilder builder = new StringBuilder();
				for (String line = null; (line = reader.readLine()) != null;)
				    builder.append(line).append("\n");
				
				String body = builder.toString();
				
				JSONObject jsonResult = new JSONObject(body);
				Log.v("", "jsonString="+jsonString);
				boolean success = jsonResult.getBoolean("success");
				Log.v("", "result = " + jsonResult.toString());
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
			private Context context;
			
			public ExportRequestMaker (String jsonString, Context context) {
				super();
				this.jsonString = jsonString;
				this.result = false;
				this.context = context;
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				
				this.progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
			}
			
			@Override
			protected Void doInBackground(Void... arg0) {
				pswd = sendExportRequest(jsonString);
				result = pswd != null && !pswd.equals("");
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res) {
				this.progressDialog.dismiss();
				
				if (result)
					Activity_Servers.onExportSuccess(pswd);
				else
					Activity_Servers.onExportError();
			}
		}
	}
	
	public static class Import {
		public static void applyImportation(Context context, JSONObject jsonObject, String code) {
			ArrayList<MuninMaster> newMasters = JSONHelper.getMastersFromJSON(jsonObject, code);
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
		
		private static JSONObject sendImportRequest(String code) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(MuninFoo.IMPORT_EXPORT_URI+"?import");
			
			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				nameValuePairs.add(new BasicNameValuePair("pswd", code));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				
				// Execute HTTP Post Request
				HttpResponse response = httpClient.execute(httpPost);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				StringBuilder builder = new StringBuilder();
				for (String line = null; (line = reader.readLine()) != null;)
				    builder.append(line).append("\n");
				
				JSONObject jsonResult = new JSONObject(builder.toString());
				
				boolean success = jsonResult.getBoolean("success");
				
				if (success) {
					return jsonResult.getJSONArray("data").getJSONObject(0);
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
		
		public static class ImportRequestMaker extends AsyncTask<Void, Integer, Void> {
			private JSONObject jsonObject;
			private String code;
			private boolean result;
			private Context context;
			private ProgressDialog progressDialog;
			
			public ImportRequestMaker (String code, Context context) {
				super();
				this.code = code;
				this.result = false;
				this.context = context;
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				
				this.progressDialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
			}
			
			@Override
			protected Void doInBackground(Void... arg0) {
				jsonObject = sendImportRequest(code);
				result = jsonObject != null;
				
				if (result)
					applyImportation(context, jsonObject, ENCRYPTION_SEED);
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res) {
				this.progressDialog.dismiss();
				
				if (result)
					Activity_Servers.onImportSuccess();
				else
					Activity_Servers.onImportError();
			}
		}
	}
	
	/**
	 * Removes the ids contained in the structure given as parameter
	 */
	private static void removeIds(ArrayList<MuninMaster> masters) {
		for (MuninMaster master : masters) {
			master.setId(-1);
			for (MuninServer server : master.getChildren()) {
				server.setId(-1);
				server.isPersistant = false;
				for (MuninPlugin plugin : server.getPlugins()) {
					plugin.setId(-1);
					plugin.isPersistant = false;
				}
			}
		}
	}
}