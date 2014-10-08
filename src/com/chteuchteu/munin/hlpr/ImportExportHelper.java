package com.chteuchteu.munin.hlpr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
import org.json.JSONTokener;

import android.os.AsyncTask;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.exc.ImportExportWebserviceException;
import com.chteuchteu.munin.ui.Activity_Servers;
import com.crashlytics.android.Crashlytics;

public class ImportExportHelper {
	public static String sendExportRequest(String jsonString) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(MuninFoo.IMPORT_EXPORT_URI);
		
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("jsonString", jsonString));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;)
			    builder.append(line).append("\n");
			
			JSONTokener tokener = new JSONTokener(builder.toString());
			JSONObject jsonResult = new JSONObject(tokener);
			
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
		
		public ExportRequestMaker (String jsonString) {
			super();
			this.jsonString = jsonString;
			result = false;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			pswd = sendExportRequest(jsonString);
			result = pswd != null && !pswd.equals("");
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void res) {
			// TODO
			result = true;
			if (result)
				Activity_Servers.onExportSuccess(pswd);
			else
				Activity_Servers.onExportError();
		}
	}
}