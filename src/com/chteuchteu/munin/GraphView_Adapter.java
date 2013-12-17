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

import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.ui.Activity_GraphView;

public class GraphView_Adapter extends BaseAdapter implements TitleProvider {
	private MuninFoo		muninFoo;
	
	private LayoutInflater	mInflater;
	private int 			position;
	
	public GraphView_Adapter(Context context) {
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		muninFoo = MuninFoo.getInstance();
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
		// Chargement de -1, 0 et +1
		if (convertView == null)
			convertView = mInflater.inflate(R.layout.fragment_graphview, null);
		//((ImageView) convertView.findViewById(R.id.tiv)).setImageBitmap(null);
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
			
			// Nettoyage du tableau des bitmaps
			/*if (position > 0 && position < Activity_GraphView.bitmaps.length - 1) {
    			for (int i = 0; i<Activity_GraphView.bitmaps.length; i++) {
    				if (i != position-1 && i != position && i != position+1 && Activity_GraphView.bitmaps[i] != null)
    					Activity_GraphView.bitmaps[i] = null;
    			}
    		}*/
			
			if (Activity_GraphView.bitmaps[position] == null) {
				loading_spin.setIndeterminate(true);
				loading_spin.setVisibility(View.VISIBLE);
			}
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			int pos = Activity_GraphView.viewFlow.getSelectedItemPosition();
			
			if (Activity_GraphView.load_period != null && muninFoo.currentServer != null && Activity_GraphView.bitmaps != null) {
				if (Activity_GraphView.bitmaps[pos] == null)
					Activity_GraphView.bitmaps[pos] = Util.removeBitmapBorder(muninFoo.currentServer.getPlugin(pos).getGraph(Activity_GraphView.load_period, muninFoo.currentServer));
				
				if (pos != 0 && Activity_GraphView.bitmaps[pos-1] == null)
					Activity_GraphView.bitmaps[pos-1] = Util.removeBitmapBorder(muninFoo.currentServer.getPlugin(pos-1).getGraph(Activity_GraphView.load_period, muninFoo.currentServer));
				
				if (pos != Activity_GraphView.bitmaps.length-1 && Activity_GraphView.bitmaps[pos+1] == null)
					Activity_GraphView.bitmaps[pos+1] = Util.removeBitmapBorder(muninFoo.currentServer.getPlugin(pos+1).getGraph(Activity_GraphView.load_period, muninFoo.currentServer));
				
				// Nettoyage du tableau
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
	
	@Override
	public String getTitle(int position) {
		if (position >= 0 && position < muninFoo.currentServer.getPlugins().size())
			return muninFoo.currentServer.getPlugin(position).getFancyName();
		return "";
	}
}
