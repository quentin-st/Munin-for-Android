package com.chteuchteu.munin.obj;

import android.app.Activity;
import android.content.Intent;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.ui.Activity_GraphView;
import com.chteuchteu.munin.ui.Activity_Grid;
import com.chteuchteu.munin.ui.Activity_Labels;
import com.chteuchteu.munin.ui.Activity_Plugins;

public class SearchResult {
	private String line_1;
	private String line_2;
	private SearchResultType searchResultType;
	private ISearchable object;
	
	public enum SearchResultType { PLUGIN, NODE, GRID, LABEL }
	
	public SearchResult(ISearchable object) {
		this.searchResultType = object.getSearchResultType();
		this.object = object;
        String[] searchResult = object.getSearchResult();
        if (searchResult.length >= 1)
            this.line_1 = searchResult[0];
        if (searchResult.length >= 2)
            this.line_2 = searchResult[1];
	}
	
	public String getLine1() { return this.line_1; }
	public String getLine2() { return this.line_2; }
	
	public void onClick(Activity activity) {
		Intent intent;
		switch (searchResultType) {
			case GRID:
				Grid grid = (Grid) object;
				
				intent = new Intent(activity, Activity_Grid.class);
				intent.putExtra("gridName", grid.getName());
				activity.startActivity(intent);
				Util.setTransition(activity, TransitionStyle.DEEPER);
				
				break;
			case LABEL:
				Label label = (Label) object;
				
				intent = new Intent(activity, Activity_Labels.class);
				intent.putExtra("labelId", label.getId());
				activity.startActivity(intent);
				Util.setTransition(activity, TransitionStyle.DEEPER);
				
				break;
			case PLUGIN:
				MuninPlugin plugin = (MuninPlugin) object;
				MuninFoo.getInstance().setCurrentNode(plugin.getInstalledOn());
				
				intent = new Intent(activity, Activity_GraphView.class);
				intent.putExtra("position", plugin.getIndex());
				activity.startActivity(intent);
				Util.setTransition(activity, TransitionStyle.DEEPER);
				
				break;
			case NODE:
				MuninFoo.getInstance().setCurrentNode((MuninNode) object);
				
				activity.startActivity(new Intent(activity, Activity_Plugins.class));
				Util.setTransition(activity, TransitionStyle.DEEPER);
				
				break;
			default:
				break;
		}
	}
}
