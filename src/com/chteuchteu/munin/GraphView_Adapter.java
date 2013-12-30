package com.chteuchteu.munin;

import org.taptwo.android.widget.TitleProvider;

import uk.co.senab.photoview.PhotoViewAttacher;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.android.volley.toolbox.ImageRequest;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.ui.Activity_GraphView;

public class GraphView_Adapter extends BaseAdapter implements TitleProvider {
	private MuninFoo		muninFoo;
	
	private LayoutInflater	mInflater;
	private int 			position;
	private Context			c;
	
	private ImageRequest r;
	
	public GraphView_Adapter(Context context) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		muninFoo = MuninFoo.getInstance(context);
		c = context;
	}
	
	@Override
	public int getCount() {
		if (muninFoo == null)
			muninFoo = MuninFoo.getInstance();
		return muninFoo.currentServer.getPlugins().size();
	}
	
	@Override
	public Object getItem(int position) {
		return position;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Loading pos -1, 0 and +1
		if (convertView == null)
			convertView = mInflater.inflate(R.layout.fragment_graphview, null);
		this.position = position;
		
		((ImageView) convertView.findViewById(R.id.tiv)).setTag(position);
		
		boolean downloaded = false;
		if ((this.position > 0 && Activity_GraphView.bitmaps[this.position-1] == null) // -1 == null
				|| (Activity_GraphView.bitmaps[this.position] == null) // 0 == null
				|| (this.position < Activity_GraphView.bitmaps.length-1 && Activity_GraphView.bitmaps[this.position+1] == null)) { // +1 == null
			applyBitmap task = new applyBitmap((ImageView) convertView.findViewById(R.id.tiv), (ProgressBar) convertView.findViewById(R.id.loading_spin));
			task.execute();
			downloaded = true;
		}
		
		if (!downloaded) { // Le téléchargement n'a pas été effectué, donc les bitmaps n'ont pas été placées
			ImageView iv = (ImageView) convertView.findViewById(R.id.tiv);
			Integer nbr = (Integer)iv.getTag();
			if (nbr >= 0 && nbr < Activity_GraphView.bitmaps.length) {
				if (Activity_GraphView.bitmaps[nbr] != null) {
					iv.setImageBitmap(Activity_GraphView.bitmaps[nbr]);
					new PhotoViewAttacher(iv);
				}
				else
					iv.setImageResource(R.drawable.download_error);
			}
		}
		
		return convertView;
	}
	
	public class applyBitmap extends AsyncTask<Void, Integer, Void> {
		private ImageView 	tiv;
		private ProgressBar loading_spin;
		
		public applyBitmap (ImageView iv, ProgressBar ls) {
			super();
			this.tiv = iv;
			this.loading_spin = ls;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			tiv.setImageBitmap(null);
			
			if (Activity_GraphView.bitmaps[position] == null) {
				loading_spin.setIndeterminate(true);
				loading_spin.setVisibility(View.VISIBLE);
			}
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			int pos = Activity_GraphView.viewFlow.getSelectedItemPosition();
			
			if (Activity_GraphView.load_period != null && muninFoo.currentServer != null && Activity_GraphView.bitmaps != null) {
				// Fetch pos 0
				if (Activity_GraphView.bitmaps[pos] == null)
					Activity_GraphView.bitmaps[pos] = Util.removeBitmapBorder(MuninFoo.grabBitmap(muninFoo.currentServer, muninFoo.currentServer.getPlugin(pos).getImgUrl(Activity_GraphView.load_period)));
				
				// Fetch pos -1
				if (pos != 0 && Activity_GraphView.bitmaps[pos-1] == null)
					Activity_GraphView.bitmaps[pos-1] = Util.removeBitmapBorder(MuninFoo.grabBitmap(muninFoo.currentServer, muninFoo.currentServer.getPlugin(pos-1).getImgUrl(Activity_GraphView.load_period)));
				
				// Fetch pos +1
				if (pos != Activity_GraphView.bitmaps.length-1 && Activity_GraphView.bitmaps[pos+1] == null)
					Activity_GraphView.bitmaps[pos+1] = Util.removeBitmapBorder(MuninFoo.grabBitmap(muninFoo.currentServer, muninFoo.currentServer.getPlugin(pos+1).getImgUrl(Activity_GraphView.load_period)));
				
				// Clean array
				/*for (int i=0; i<Activity_GraphView.bitmaps.length; i++) {
					if (i != Activity_GraphView.position-1 && i != Activity_GraphView.position && i != Activity_GraphView.position+1 && Activity_GraphView.bitmaps[i] != null)
    					Activity_GraphView.bitmaps[i] = null;
    			}*/
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			loading_spin.setVisibility(View.GONE);
			
			Integer nbr = (Integer)tiv.getTag();
			if (nbr >= 0 && nbr < Activity_GraphView.bitmaps.length) {
				if (Activity_GraphView.bitmaps[nbr] != null) {
					tiv.setImageBitmap(Activity_GraphView.bitmaps[nbr]);
					PhotoViewAttacher mAttacher = new PhotoViewAttacher(tiv);
					if (mAttacher.getMidScale() < 2f)
						mAttacher.setMaxScale(2f);
				}
				else
					tiv.setImageResource(R.drawable.download_error);
			}
		}
	}
	
	/*public void fetchBitmap(final int pos, final boolean retried) {
		final String url = muninFoo.currentServer.getPlugin(pos).getImgUrl(Activity_GraphView.load_period);
		Log.v("", "fetching bitmap " + pos + " (" + url + ")");
		final RequestFuture<Bitmap> future = RequestFuture.newFuture();
		Activity_GraphView.bitmaps[pos] = null;
		
		if (r != null)
			Log.v("", "r was not null");
		else
			Log.v("", "r was null");
		
		r = new ImageRequest(url, future, 0, 0, null,
				new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				Log.v("", "error " + pos + " : " + error.toString());
				Activity_GraphView.bitmaps[pos] = null;
				// Error while trying to get a response : may be digest auth
				if (muninFoo.currentServer.getAuthType() == MuninServer.AUTH_DIGEST) {
					if (error == null)
						Log.v("", "error is null");
					else {
						if (error.networkResponse == null)
							Log.v("", "error.networkRresponse");
						else {
							if (error.networkResponse.headers == null)
								Log.v("", "headers");
							else {
								if (error.networkResponse.headers.get("WWW-Authenticate") == null)
									Log.v("", "www-auth is null");
							}
						}
					}
					if (error != null && error.networkResponse != null && error.networkResponse.headers != null
							&& error.networkResponse.headers.get("WWW-Authenticate") != null) {
						Log.v("", "");
						muninFoo.currentServer.setAuthString(error.networkResponse.headers.get("WWW-Authenticate"));
						// Let's retry with this new auth string
						if (!retried) {
							Log.v("", "retrying " + pos);
							future.cancel(true);
							r.cancel();
							fetchBitmap(pos, true);
						}
						else
							Log.v("", "won't retry");
					} else
						Log.v("", "something's null");
				}
				else
					Log.v("", "not digest");
			}
		}) {
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				Map<String, String> headers = new HashMap<String, String>();
				if (muninFoo.currentServer.getAuthType() == MuninServer.AUTH_BASIC)
					headers.put("Authorization", "Basic " + Base64.encodeToString((muninFoo.currentServer.getAuthLogin() + ":" + muninFoo.currentServer.getAuthPassword()).getBytes(), Base64.NO_WRAP));
				else if (muninFoo.currentServer.getAuthType() == MuninServer.AUTH_DIGEST)
					headers.put("Authorization", DigestUtils.getDigestAuthHeader(muninFoo.currentServer, url));
				return headers;
			}
		};
		muninFoo.requestQueue.add(r);
		try {
			Log.v("", "trying to get");
			Activity_GraphView.bitmaps[pos] = future.get();
		} catch (InterruptedException e) { e.printStackTrace(); }
		catch (Exception e) { e.printStackTrace(); }
		Log.v("", "finished " + pos);
	}*/
	
	@Override
	public String getTitle(int position) {
		if (position >= 0 && position < muninFoo.currentServer.getPlugins().size())
			return muninFoo.currentServer.getPlugin(position).getFancyName();
		return "";
	}
}
