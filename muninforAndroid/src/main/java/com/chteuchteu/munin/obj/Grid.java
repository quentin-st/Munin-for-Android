package com.chteuchteu.munin.obj;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.GridDownloadHelper;
import com.chteuchteu.munin.hlpr.Util;

import java.util.ArrayList;
import java.util.List;

public class Grid {
	public long id;
	public String name;
	public int nbColumns;
	public int nbLines;
	public List<GridItem> items;
	public MuninFoo f;
	public GridDownloadHelper dHelper;
	
	private LinearLayout container;
	
	public MuninPlugin currentlyOpenedPlugin = null;
	
	public Grid(String name, MuninFoo f) {
		this.name = name;
		this.items = new ArrayList<>();
		this.nbColumns = 2;
		this.nbLines = 2;
		this.f = f;
	}
	
	private void preAdd(GridItem item) {
		// Check if exists
		boolean exists = false;
		for (GridItem i : items) {
			if (i.grid.name.equals(this.name) && i.plugin != null && i.plugin.equals(item.plugin))
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
			MuninFoo.logE("This item cannot be placed (" + item.X + "," + item.Y + ")");
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
			if (i.grid.name.equals(name) && i.plugin.equals(item.plugin))
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
			MuninFoo.logE("", "This item cannot be placed (" + item.X + "," + item.Y + ")");
		
		// Update this items size on this line
		for (int x=0; x<nbColumns; x++) {
			LinearLayout ll = getViewAt(x, item.Y);
			if (ll != null)
				updateGridSize(ll, c);
		}
		
		f.sqlite.saveGridItemRelations(this);
	}
	
	private GridItem get(int posX, int posY) {
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
			removeEmptyLines();
		}
	}
	
	public void cancelAlpha() {
		for (GridItem i : items) {
			if (i.iv != null && i.iv.getAlpha() != 1.0f)
				i.iv.setAlpha(1f);
		}
	}

	public void toggleFootersVisibility(boolean visible) {
		for (GridItem i : items)
			i.footer.setVisibility(visible ? View.VISIBLE : View.GONE);
	}
	
	private void removeEmptyColumns(Context c) {
		for (int x=nbColumns-1; x>=0; x--) {
			if (isColumnEmpty(x))
				removeEmptyColumn(c, x);
			else // Stop removing columns when the target column isn't empty
				return;
		}
	}
	
	private void removeEmptyLines() {
		for (int y=nbLines-1; y>=0; y--) {
			if (isLineEmpty(y))
				removeEmptyLine(y);
			else // Stop removing lines when the target line isn't empty
				return;
		}
	}
	
	public int getGridItemHeight(Context c, int nbCol) {
		float ratio = (float) (800.0 / 600.0);
		int deviceWidth = Util.getDeviceSize(c)[0];
		int gridItemWidth = deviceWidth / nbCol;
		int gridItemHeight = Math.round(gridItemWidth / ratio);
		return gridItemHeight;
	}
	
	
	public void setupLayout() {
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
	
	private boolean removeEmptyColumn(Context c, int x) {
		if (x >= nbColumns)
			return false;
		
		boolean canRemove = true;
		
		for (int y=0; y<nbLines; y++) {
			if (get(x, y) != null)
				canRemove = false;
		}
		
		if (canRemove) {
			for (int y=0; y<nbLines; y++) {
				LinearLayout toRemove = getViewAt(x, y);
				LinearLayout line = (LinearLayout) toRemove.getParent();
				line.removeView(toRemove);
			}
			nbColumns--;
			updateAllGridSizes(c);
			return true;
		}
		return false;
	}
	
	private boolean removeEmptyLine(int y) {
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
		items.remove(get(x, y));
	}
	
	public void move(int x, int y, int newX, int newY) {
		LinearLayout curView = getViewAt(x, y);
		LinearLayout destView = getViewAt(newX, newY);
		GridItem curItem = get(x, y);
		if (curItem != null) {
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
	}
	
	public void swapViews(LinearLayout view1, LinearLayout view2) {
		RelativeLayout content1 = (RelativeLayout) view1.getChildAt(0);
		RelativeLayout content2 = (RelativeLayout) view2.getChildAt(0);
		
		view1.removeView(content1);
		view2.removeView(content2);
		
		view1.addView(content2);
		view2.addView(content1);
	}
	
	private void disablePlusButtons() {
		for (int x=0; x<nbColumns; x++) {
			for (int y=0; y<nbLines; y++) {
				if (getViewAt(x, y) != null && get(x, y) == null) { // Empty view
					LinearLayout outerContainer = getViewAt(x, y);
					outerContainer.setVisibility(View.INVISIBLE);
				}
			}
		}
	}
	
	private void reEnablePlusButtons() {
		for (int x=0; x<nbColumns; x++) {
			for (int y=0; y<nbLines; y++) {
				if (getViewAt(x, y) != null && get(x, y) == null) { // Empty view
					LinearLayout outerContainer = getViewAt(x, y);
					outerContainer.setVisibility(View.VISIBLE);
				}
			}
		}
	}
	
	private void updateAllGridSizes(Context c) {
		for (int y=0; y<nbLines; y++) {
			for (int x=0; x<nbColumns; x++) {
				if (getViewAt(x, y) != null)
					updateGridSize(getViewAt(x, y), c);
			}
		}
	}
	
	private void updateGridSize(LinearLayout v, Context c) {
		if (v != null)
			v.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, getGridItemHeight(c, nbColumns), 1.0f));
	}
	
	public LinearLayout getViewAt(int x, int y) {
		LinearLayout row = (LinearLayout) container.getChildAt(y);
		if (row != null)
			return (LinearLayout) row.getChildAt(x);
		return null;
	}
	
	private int getGridWidth() {
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
	
	private int getGridHeight() {
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
	
	private boolean isColumnEmpty(int x) {
		for (int y=0; y<nbLines; y++) {
			if (get(x, y) != null)
				return false;
		}
		return true;
	}
	
	private boolean isLineEmpty(int y) {
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