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
import com.chteuchteu.munin.ui.Fragment_Grid;
import com.chteuchteu.munin.ui.IGridActivity;

import java.util.ArrayList;
import java.util.List;

public class Grid {
	private long id;
	private String name;
	private int nbColumns;
	private int nbLines;
	private List<GridItem> items;

	public MuninFoo f;
	public GridDownloadHelper dHelper;
	private LinearLayout container;
	public GridItem currentlyOpenedGridItem = null;
	private IGridActivity activity;
	private Fragment_Grid fragment;
	
	public Grid(String name) {
		this.name = name;
		this.items = new ArrayList<>();
		this.nbColumns = 2;
		this.nbLines = 2;
	}

	public void setActivityReferences(Context context, MuninFoo muninFoo, IGridActivity activity, Fragment_Grid fragment) {
		this.f = muninFoo;
		this.activity = activity;
		this.fragment = fragment;

		// Set references for children
		for (GridItem item : items)
			item.setActivityReferences(context, activity, fragment);
	}
	
	private void preAdd(GridItem item) {
		// Check if exists
		boolean exists = false;
		for (GridItem i : items) {
			if (i.getGrid().name.equals(this.name) && i.getPlugin() != null && i.getPlugin().equals(item.getPlugin()))
				exists = true;
		}
		if (exists)
			return;
		
		// Add columns / lines if necessary
		while (item.getX()+1 > nbColumns)
			nbColumns++;
		while (item.getY()+1 > nbLines)
			nbLines++;
		
		if (get(item.getX(), item.getY()) == null)
			this.items.add(item);
		else
			MuninFoo.logE("This item cannot be placed (" + item.getX() + "," + item.getY() + ")");
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
			if (i.getGrid().name.equals(name) && i.getPlugin().equals(item.getPlugin()))
				exists = true;
		}
		if (exists)
			return;
		
		// Add columns / lines if necessary
		while (item.getX()+1 >= nbColumns)
			addColumn(c, editView);
		while (item.getY()+1 >= nbLines)
			addLine(c, editView);
		
		if (get(item.getX(), item.getY()) == null)
			this.items.add(item);
		else
			MuninFoo.logE("", "This item cannot be placed (" + item.getX() + "," + item.getY() + ")");
		
		// Update this items size on this line
		for (int x=0; x<nbColumns; x++) {
			LinearLayout ll = getViewAt(x, item.getY());
			if (ll != null)
				updateGridSize(ll, c);
		}
		
		f.sqlite.saveGridItemRelations(this);
	}
	
	private GridItem get(int posX, int posY) {
		for (GridItem i : items) {
			if (i.getX() == posX && i.getY() == posY)
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
					line.addView(GridItem.getEmptyView(this, c, f, activity, fragment, x, y, line));
				else
					line.addView(get(x, y).getView(line));
			}
			
			container.addView(line);
		}
		return container;
	}
	
	public void edit(View gridView) {
		gridView.findViewById(R.id.add_line_bottom).setVisibility(View.VISIBLE);
		gridView.findViewById(R.id.add_column_right).setVisibility(View.VISIBLE);
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
	
	public int getGridItemHeight(Context context) {
		float ratio = 1.7f;
		int deviceWidth = Util.getDeviceSize(context)[0];
		int gridItemWidth = deviceWidth / this.nbColumns;
		return Math.round(gridItemWidth / ratio);
	}
	
	
	public void setupLayout() {
		List<GridItem> l = items;
		items = new ArrayList<>();
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
				line.addView(GridItem.getEmptyView(this, c, f, activity, fragment, i, nbLines-1, line));
			
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
				row.addView(GridItem.getEmptyView(this, c, f, activity, fragment, nbColumns-1, i, row));
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
				destItem.setX(x);
				destItem.setY(y);
			}
			curItem.setX(newX);
			curItem.setY(newY);
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
			v.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, getGridItemHeight(c), 1.0f));
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
			if (i.getX() > curWidth)
				curWidth = i.getX();
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
			if (i.getY() > curHeight)
				curHeight = i.getY();
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

	public long getId() { return id; }
	public void setId(long id) { this.id = id; }

	public String getName() { return name; }

	public int getNbColumns() { return nbColumns; }
	public int getNbLines() { return nbLines; }

	public List<GridItem> getItems() { return items; }
	public void setItems(List<GridItem> items) { this.items = items; }
}
