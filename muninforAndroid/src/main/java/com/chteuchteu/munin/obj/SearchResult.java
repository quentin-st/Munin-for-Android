package com.chteuchteu.munin.obj;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.ui.Activity_GraphView;
import com.chteuchteu.munin.ui.Activity_Grid;
import com.chteuchteu.munin.ui.Activity_Label;
import com.chteuchteu.munin.ui.Activity_Plugins;


public class SearchResult {
	private String line_1;
	private String line_2;
	private SearchResultType searchResultType;
	private Object object;
	
	public enum SearchResultType { PLUGIN, SERVER, GRID, LABEL }
	
	public SearchResult(SearchResultType resultType, Object object, Context context) {
		this.searchResultType = resultType;
		this.object = object;
		
		switch (searchResultType) {
			case GRID:
				String grid = (String) object;
				line_1 = context.getText(R.string.text75) + " " + grid;
				break;
			case LABEL:
				Label label = (Label) object;
				line_1 = label.getName();
				break;
			case PLUGIN:
				MuninPlugin plugin = (MuninPlugin) object;
				line_1 = plugin.getFancyName();
				line_2 = plugin.getInstalledOn().getName();
				break;
			case SERVER:
				MuninServer server = (MuninServer) object;
				line_1 = server.getName();
				line_2 = server.getServerUrl();
				break;
			default:
				break;
			
		}
	}
	
	public String getLine1() { return this.line_1; }
	public String getLine2() { return this.line_2; }
	
	public void onClick(Activity activity) {
		Intent intent;
		switch (searchResultType) {
			case GRID:
				String grid = (String) object;
				
				intent = new Intent(activity, Activity_Grid.class);
				intent.putExtra("gridName", grid);
				activity.startActivity(intent);
				Util.setTransition(activity, TransitionStyle.DEEPER);
				
				break;
			case LABEL:
				Label label = (Label) object;
				
				intent = new Intent(activity, Activity_Label.class);
				intent.putExtra("label", label.getName());
				activity.startActivity(intent);
				Util.setTransition(activity, TransitionStyle.DEEPER);
				
				break;
			case PLUGIN:
				MuninPlugin plugin = (MuninPlugin) object;
				MuninFoo.getInstance().currentServer = plugin.getInstalledOn();
				
				intent = new Intent(activity, Activity_GraphView.class);
				intent.putExtra("position", plugin.getIndex());
				activity.startActivity(intent);
				Util.setTransition(activity, TransitionStyle.DEEPER);
				
				break;
			case SERVER:
				MuninFoo.getInstance().currentServer = (MuninServer) object;
				
				activity.startActivity(new Intent(activity, Activity_Plugins.class));
				Util.setTransition(activity, TransitionStyle.DEEPER);
				
				break;
			default:
				break;
		}
	}
}