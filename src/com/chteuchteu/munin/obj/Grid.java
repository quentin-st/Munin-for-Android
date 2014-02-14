package com.chteuchteu.munin.obj;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.GridDownloadHelper;
import com.chteuchteu.munin.hlpr.Util;

public class Grid {
	public long 	id;
	public String 	name;
	public int 		nbColumns;
	public int 		nbLines;
	public List<GridItem> items;
	public MuninFoo	f;
	public GridDownloadHelper dHelper;
	
	private LinearLayout container;
	
	public static int GridItemWidth = 800;
	public static int GridItemHeight = 600;
	
	public MuninPlugin currentlyOpenedPlugin = null;
	
	public Grid(String name, MuninFoo f) {
		this.name = name;
		this.items = new ArrayList<GridItem>();
		this.nbColumns = 2;
		this.nbLines = 2;
		this.f = f;
	}
	
	public void preAdd(GridItem item) {
		// Check if exists
		boolean exists = false;
		for (GridItem i : items) {
			if (i.grid.name.equals(name) && i.plugin.equalsApprox(item.plugin))
				exists = true;
		}
		if (exists)
			return;
		
		// Add columns / lines if necessary
		while (item.X+1 > nbColumns)
			nbColumns++;
		while (item.Y+1 > nbLines)
			nbLines++;
		
		if (get(item.X, item.Y) == null)
			this.items.add(item);
		else
			Log.e("", "This item cannot be placed (" + item.X + "," + item.Y + ")");
	}
	
	public void updateLayoutSizes(Context c) {
		for (int y=0; y<nbLines; y++) {
			for (int x=0; x<nbColumns; x++) {
				if (getViewAt(x, y) != null)
					updateGridSize(getViewAt(x, y), c);
			}
		}
	}
	
	public void add(GridItem item, Context c, MuninFoo f, boolean editView) {
		// Check if exists
		boolean exists = false;
		for (GridItem i : items) {
			if (i.grid.name.equals(name) && i.plugin.equalsApprox(item.plugin))
				exists = true;
		}
		if (exists)
			return;
		
		// Add columns / lines if necessary
		while (item.X+1 >= nbColumns)
			addColumn(c, editView);
		while (item.Y+1 >= nbLines)
			addLine(c, editView);
		
		if (get(item.X, item.Y) == null)
			this.items.add(item);
		else
			Log.e("", "This item cannot be placed (" + item.X + "," + item.Y + ")");
		
		// Update this items size on this line
		for (int x=0; x<nbColumns; x++) {
			LinearLayout ll = getViewAt(x, item.Y);
			if (ll != null)
				updateGridSize(ll, c);
		}
		
		f.sqlite.saveGridItemRelations(this);
	}
	
	public GridItem get(int posX, int posY) {
		for (GridItem i : items) {
			if (i.X == posX && i.Y == posY)
				return i;
		}
		return null;
	}
	
	public LinearLayout buildLayout(Context c) {
		container = new LinearLayout(c);
		container.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		container.setOrientation(LinearLayout.VERTICAL);
		// Line per line
		LinearLayout line;
		for (int y=0; y<nbLines; y++) {
			line = new LinearLayout(c);
			line.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			line.setOrientation(LinearLayout.HORIZONTAL);
			
			// Column per column
			for (int x=0; x<nbColumns; x++) {
				if (get(x, y) == null)
					line.addView(GridItem.getEmptyView(this, c, f, x, y));
				else
					line.addView(get(x, y).getView(c));
			}
			
			container.addView(line);
		}
		return container;
	}
	
	public void edit(Activity a) {
		a.findViewById(R.id.add_line_bottom).setVisibility(View.VISIBLE);
		a.findViewById(R.id.add_column_right).setVisibility(View.VISIBLE);
		reEnablePlusButtons();
	}
	
	public void cancelEdit(Context c) {
		((Activity) c).findViewById(R.id.add_line_bottom).setVisibility(View.GONE);
		((Activity) c).findViewById(R.id.add_column_right).setVisibility(View.GONE);
		for (GridItem i : items) {
			if (i.editing)
				i.cancelEdit();
		}
		disablePlusButtons();
		if (items.size() > 0) {
			removeEmptyColumns(c);
			removeEmptyLines(c);
		}
	}
	
	@SuppressLint("NewApi")
	public void cancelAlpha(Context c) {
		for (GridItem i : items) {
			if (i.iv != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && i.iv.getAlpha() != 1.0f)
				i.iv.setAlpha(1f);
		}
	}
	
	public void removeEmptyColumns(Context c) {
		for (int x=nbColumns-1; x>=0; x--) {
			if (isColumnEmpty(x))
				removeEmptyColumn(c, x);
			else // Stop removing columns when the target column isn't empty
				return;
		}
	}
	
	public void removeEmptyLines(Context c) {
		for (int y=nbLines-1; y>=0; y--) {
			if (isLineEmpty(y))
				removeEmptyLine(c, y);
			else // Stop removing lines when the target line isn't empty
				return;
		}
	}
	
	/*public int getGridItemWidth(Context c, int nbCol) {
		int deviceWidth = Util.getDeviceSize(c)[0];
		int gridItemWidth = deviceWidth / nbCol;
		Log.v("", "deviceWidth:" + deviceWidth + " , gridItemWidth:" + gridItemWidth);
		return gridItemWidth;
	}*/
	public int getGridItemHeight(Context c, int nbCol) {
		float ratio = (float) (800.0 / 600.0);
		int deviceWidth = Util.getDeviceSize(c)[0];
		int gridItemWidth = deviceWidth / nbCol;
		int gridItemHeight = Math.round(gridItemWidth / ratio);
		return gridItemHeight;
	}
	
	
	public void setupLayout(Context c) {
		List<GridItem> l = items;
		items = new ArrayList<GridItem>();
		for (GridItem i : l)
			this.preAdd(i);
	}
	
	public void addLine(Context c, boolean editView) {
		this.nbLines++;
		
		if (editView) {
			LinearLayout line = new LinearLayout(c);
			line.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			line.setOrientation(LinearLayout.HORIZONTAL);
			for (int i=0; i<nbColumns; i++)
				line.addView(GridItem.getEmptyView(this, c, f, i, nbLines-1));
			
			container.addView(line);
			
			for (GridItem i : items) {
				if (i.editing)
					i.updateActionButtons();
			}
		}
	}
	
	public void addColumn(Context c, boolean editView) {
		this.nbColumns++;
		
		if (editView) {
			for (int i=0; i<nbLines; i++) {
				// Get the layout to add the view
				LinearLayout row = (LinearLayout) container.getChildAt(i);
				row.addView(GridItem.getEmptyView(this, c, f, nbColumns-1, i));
			}
			
			// Update each view
			updateAllGridSizes(c);
			
			for (GridItem i : items) {
				if (i.editing)
					i.updateActionButtonsAfterAddingColumn();
			}
		}
	}
	
	public boolean removeEmptyColumn(Context c, int x) {
		if (x >= nbColumns)
			return false;
		
		boolean canRemove = true;
		
		for (int y=0; y<nbLines; y++) {
			if (get(x, y) != null)
				canRemove = false;
		}
		
		if (canRemove) {
			for (int y=0; y<nbLines; y++) {
				LinearLayout toRemove = (LinearLayout) getViewAt(x, y);
				LinearLayout line = (LinearLayout) toRemove.getParent();
				line.removeView(toRemove);
			}
			nbColumns--;
			updateAllGridSizes(c);
			return true;
		}
		return false;
	}
	
	public boolean removeEmptyLine(Context c, int y) {
		if (y >= nbLines)
			return false;
		
		boolean canRemove = true;
		
		for (int x=0; x<nbColumns; x++) {
			if (get(x, y) != null)
				canRemove = false;
		}
		
		if (canRemove) {
			View v = getViewAt(0, y);
			if (v != null) {
				LinearLayout line = (LinearLayout) getViewAt(0, y).getParent();
				if (line != null) {
					container.removeView(line);
					nbLines--;
				}
			}
			return true;
		}
		return false;
	}
	
	public void remove(int x, int y) {
		GridItem i = get(x, y);
		if (i != null)
			items.remove(i);
	}
	
	public void move(int x, int y, int newX, int newY) {
		LinearLayout curView = (LinearLayout) getViewAt(x, y);
		LinearLayout destView = (LinearLayout) getViewAt(newX, newY);
		GridItem curItem = get(x, y);
		GridItem destItem = get(newX, newY);
		if (destItem != null) {
			destItem.X = x;
			destItem.Y = y;
		}
		curItem.X = newX;
		curItem.Y = newY;
		curItem.updateActionButtons();
		
		swapViews(curView, destView);
	}
	
	public void swapViews(LinearLayout view1, LinearLayout view2) {
		RelativeLayout content1 = (RelativeLayout) ((LinearLayout)view1).getChildAt(0);
		RelativeLayout content2 = (RelativeLayout) ((LinearLayout)view2).getChildAt(0);
		
		view1.removeView(content1);
		view2.removeView(content2);
		
		view1.addView(content2);
		view2.addView(content1);
	}
	
	public void disablePlusButtons() {
		for (int x=0; x<nbColumns; x++) {
			for (int y=0; y<nbLines; y++) {
				if (getViewAt(x, y) != null && get(x, y) == null) { // Empty view
					LinearLayout outerContainer = getViewAt(x, y);
					outerContainer.setVisibility(View.INVISIBLE);
				}
			}
		}
	}
	
	public void reEnablePlusButtons() {
		for (int x=0; x<nbColumns; x++) {
			for (int y=0; y<nbLines; y++) {
				if (getViewAt(x, y) != null && get(x, y) == null) { // Empty view
					LinearLayout outerContainer = getViewAt(x, y);
					outerContainer.setVisibility(View.VISIBLE);
				}
			}
		}
	}
	
	public void updateAllGridSizes(Context c) {
		for (int y=0; y<nbLines; y++) {
			for (int x=0; x<nbColumns; x++) {
				if (getViewAt(x, y) != null)
					updateGridSize(getViewAt(x, y), c);
			}
		}
	}
	
	public void updateGridSize(LinearLayout v, Context c) {
		if (v != null)
			v.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, getGridItemHeight(c, nbColumns), 1.0f));
	}
	
	public LinearLayout getViewAt(int x, int y) {
		LinearLayout row = (LinearLayout) container.getChildAt(y);
		if (row != null)
			return (LinearLayout) row.getChildAt(x);
		return null;
	}
	
	public int getGridWidth() {
		int curWidth = 0;
		for (GridItem i : items) {
			if (i.X > curWidth)
				curWidth = i.X;
		}
		return curWidth+1;
	}
	
	// Returns the width using getWidth,
	// less the empty columns at the end
	public int getFullWidth() {
		int w = getGridWidth()-1;
		int lastFullCol = w;
		for (int i=w;i>=0;i--) {
			if (isColumnEmpty(lastFullCol))
				lastFullCol--;
		}
		return lastFullCol + 1;
	}
	
	public int getGridHeight() {
		int curHeight = 0;
		for (GridItem i : items) {
			if (i.Y > curHeight)
				curHeight = i.Y;
		}
		return curHeight+1;
	}
	
	public int getFullHeight() {
		int h = getGridHeight()-1;
		int lastFullRow = h;
		for (int i=h; i>=0;i--) {
			if (isLineEmpty(lastFullRow))
				lastFullRow--;
		}
		return lastFullRow + 1;
	}
	
	public boolean isColumnEmpty(int x) {
		for (int y=0; y<nbLines; y++) {
			if (get(x, y) != null)
				return false;
		}
		return true;
	}
	
	public boolean isLineEmpty(int y) {
		for (int x=0; x<nbColumns; x++) {
			if (get(x, y) != null)
				return false;
		}
		return true;
	}
	
	public int[] getNextAvailable(int beginX, int beginY, int maxWidth, Context c) {
		int[] r = new int[2]; r[0] = beginX; r[1] = beginY;
		boolean available = get(beginX, beginY) == null;
		
		if(!available) {
			int curX = beginX;
			int curY = beginY;
			
			while (!available) {
				curX++;
				if (curX > maxWidth-1) {
					curX = 0;
					curY++;
					if (nbLines <= curY)
						addLine(c, true);
				}
				available = get(curX, curY) == null;
				if (available) {
					r[0] = curX; r[1] = curY;
					return r;
				}
			}
		}
		
		return r;
	}
}