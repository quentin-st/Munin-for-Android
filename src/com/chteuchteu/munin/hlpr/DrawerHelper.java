package com.chteuchteu.munin.hlpr;

import java.util.ArrayList;
import java.util.List;

import org.taptwo.android.widget.ViewFlow;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Label;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.obj.SearchResult;
import com.chteuchteu.munin.obj.SearchResult.SearchResultType;
import com.chteuchteu.munin.ui.Activity_Alerts;
import com.chteuchteu.munin.ui.Activity_GoPremium;
import com.chteuchteu.munin.ui.Activity_Grids;
import com.chteuchteu.munin.ui.Activity_Labels;
import com.chteuchteu.munin.ui.Activity_Notifications;
import com.chteuchteu.munin.ui.Activity_Plugins;
import com.chteuchteu.munin.ui.Activity_Server;
import com.chteuchteu.munin.ui.Activity_Servers;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;


@SuppressLint("InflateParams")
public class DrawerHelper {
	public int Activity_About = -1;
	public int Activity_Alerts = 2;
	public int Activity_AlertsPluginSelection = 2;
	public int Activity_Label = 9;
	public int Activity_Labels = 9;
	public int Activity_GoPremium = 10;
	public int Activity_GraphView = 3;
	public int Activity_Grid = 11;
	public int Activity_Grids = 11;
	public int Activity_Main = 0;
	public int Activity_Notifications = 4;
	public int Activity_Plugins = 7;
	public int Activity_Server_Add = 1;
	public int Activity_Servers = 5;
	public int Activity_ServersEdit = 5;
	public int Activity_Settings = 6;
	
	private Activity a;
	private Context c;
	private MuninFoo m;
	private int n;
	private SlidingMenu sm;
	
	private EditText search;
	private ListView search_results;
	private SearchAdapter search_results_adapter;
	private ArrayList<SearchResult> search_results_array;
	private List<String> search_cachedGridsList;
	
	// GraphView
	private ViewFlow vf;
	
	public DrawerHelper(Activity a, MuninFoo m) {
		this.a = a;
		this.m = m;
		this.c = a.getApplicationContext();
		initDrawer();
	}
	
	public void reset() {
		initDrawer(false);
		setDrawerActivity(n);
	}
	
	public void setDrawerActivity(int activity) {
		this.n = activity;
		switch (activity) {
			case 0:
				// Accueil: rien
				setSelectedMenuItem("");
				break;
			case 1:
				setSelectedMenuItem("servers");
				initServersList();
				break;
			case 2:
				setSelectedMenuItem("alerts");
				break;
			case 3:
				setSelectedMenuItem("graphs");
				//initPluginsList();
				break;
			case 4:
				setSelectedMenuItem("notifications");
				break;
			case 5:
				setSelectedMenuItem("servers");
				break;
			case 6:
				// Rien (ActionBar)
				setSelectedMenuItem("");
				break;
			case 7:
				setSelectedMenuItem("graphs");
				break;
			case 8:
				setSelectedMenuItem("servers");
				initServersList();
				break;
			case 9:
				setSelectedMenuItem("labels");
				break;
			case 10:
				setSelectedMenuItem("premium");
				break;
			case 11:
				setSelectedMenuItem("grid");
			default:
				setSelectedMenuItem("");
				break;
		}
	}
	
	public void setViewFlow(ViewFlow v) { this.vf = v; }
	
	public SlidingMenu getDrawer() { return this.sm; }
	
	private void initDrawer() { initDrawer(true); }
	private void initDrawer(boolean firstLoad) {
		if (firstLoad)
			sm = new SlidingMenu(a);
		
		sm.setMode(SlidingMenu.LEFT);
		if (a.getClass().getSimpleName().equals("Activity_Main"))
			sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		else
			sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setFadeEnabled(true);
		sm.setSelectorEnabled(true);
		sm.setBehindScrollScale(0.25f);
		//sm.setShadowDrawable(R.drawable.drawer_shadow);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		//sm.attachToActivity(a, SlidingMenu.SLIDING_WINDOW);
		if (firstLoad)
			sm.attachToActivity(a, SlidingMenu.SLIDING_CONTENT);
		
		//sm.setActionBarSlideIcon(new ActionBarSlideIcon(a, R.drawable.ic_navigation_drawer, R.string.text63_1, R.string.text63_2));
		a.getActionBar().setDisplayHomeAsUpEnabled(false);
		
		if (firstLoad)
			sm.setMenu(R.layout.drawer);
		
		// Graphs
		a.findViewById(R.id.drawer_graphs_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(a, Activity_Plugins.class);
				if (n == Activity_Grid)	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				else					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				a.startActivity(i);
				Util.setTransition(a, TransitionStyle.DEEPER);
			}
		});
		a.findViewById(R.id.drawer_grid_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(a, Activity_Grids.class);
				if (n == Activity_Grid)	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				else					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				a.startActivity(i);
				Util.setTransition(a, TransitionStyle.DEEPER);
			}
		});
		// Alerts
		a.findViewById(R.id.drawer_alerts_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(a, Activity_Alerts.class);
				if (n == Activity_Grid)	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				else					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				a.startActivity(i);
				Util.setTransition(a, TransitionStyle.DEEPER);
			}
		});
		// Labels
		a.findViewById(R.id.drawer_labels_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(a, Activity_Labels.class);
				if (n == Activity_Grid)	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				else					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				a.startActivity(i);
				Util.setTransition(a, TransitionStyle.DEEPER);
			}
		});
		// Servers
		a.findViewById(R.id.drawer_servers_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(a, Activity_Servers.class);
				if (n == Activity_Grid)	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				else					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				a.startActivity(i);
				Util.setTransition(a, TransitionStyle.DEEPER);
			}
		});
		// Notifications
		a.findViewById(R.id.drawer_notifications_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(a, Activity_Notifications.class);
				if (n == Activity_Grid)	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				else					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				a.startActivity(i);
				Util.setTransition(a, TransitionStyle.DEEPER);
			}
		});
		// Premium
		a.findViewById(R.id.drawer_premium_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(a, Activity_GoPremium.class);
				if (n == Activity_Grid)	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				else					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				a.startActivity(i);
				Util.setTransition(a, TransitionStyle.DEEPER);
			}
		});
		
		if (!m.premium) {
			a.findViewById(R.id.drawer_notifications_btn).setEnabled(false);
			a.findViewById(R.id.drawer_grid_btn).setEnabled(false);
			a.findViewById(R.id.drawer_notifications_img).setAlpha(0.5f);
			a.findViewById(R.id.drawer_notifications_txt).setAlpha(0.5f);
			a.findViewById(R.id.drawer_grid_img).setAlpha(0.5f);
			a.findViewById(R.id.drawer_grid_txt).setAlpha(0.5f);
			a.findViewById(R.id.drawer_button_premium_ll).setVisibility(View.VISIBLE);
		}
		if (m.getHowManyServers() == 0) {
			a.findViewById(R.id.drawer_graphs_btn).setEnabled(false);
			a.findViewById(R.id.drawer_grid_btn).setEnabled(false);
			a.findViewById(R.id.drawer_alerts_btn).setEnabled(false);
			a.findViewById(R.id.drawer_notifications_btn).setEnabled(false);
			a.findViewById(R.id.drawer_labels_btn).setEnabled(false);
		}
		
		Util.Fonts.setFont(c, (ViewGroup) a.findViewById(R.id.drawer_scrollview), CustomFont.RobotoCondensed_Regular);
		
		// Init search
		search = (EditText) a.findViewById(R.id.drawer_search);
		search_results = (ListView) a.findViewById(R.id.drawer_search_results);
		

		// Cancel button
		//final int DRAWABLE_LEFT = 0;
		//final int DRAWABLE_TOP = 1;
		final int DRAWABLE_RIGHT = 2;
		//final int DRAWABLE_BOTTOM = 3;
		search.getCompoundDrawables()[DRAWABLE_RIGHT].setAlpha(0);
		
		search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH)
					return true;
				return false;
			}
		});
		
		search.addTextChangedListener(new TextWatcher() {
			@SuppressLint("DefaultLocale")
			@Override
			public void afterTextChanged(Editable s) {
				String string = s.toString().toLowerCase();
				
				if (string.length() == 0) {
					a.findViewById(R.id.drawer_scrollview).setVisibility(View.VISIBLE);
					a.findViewById(R.id.drawer_search_results).setVisibility(View.GONE);
					search.getCompoundDrawables()[DRAWABLE_RIGHT].setAlpha(0);
					return;
				} else {
					a.findViewById(R.id.drawer_scrollview).setVisibility(View.GONE);
					a.findViewById(R.id.drawer_search_results).setVisibility(View.VISIBLE);
					search.getCompoundDrawables()[DRAWABLE_RIGHT].setAlpha(255);
				}
				
				if (search_results_adapter != null) {
					search_results_array.clear();
					search_results_adapter.notifyDataSetChanged();
				} else {
					search_results_array = new ArrayList<SearchResult>();
					search_results_adapter = new SearchAdapter(a, search_results_array);
					search_results.setAdapter(search_results_adapter);
				}
				
				// Search in plugins and servers
				for (MuninServer server : MuninFoo.getInstance().getServers()) {
					String serverName = server.getName().toLowerCase();
					String serverUrl = server.getServerUrl().toLowerCase();
					
					if (serverName.contains(string) || serverUrl.contains(string))
						search_results_array.add(new SearchResult(SearchResultType.SERVER, server, c));
					
					
					for (MuninPlugin plugin : server.getPlugins()) {
						if (plugin.getName().toLowerCase().contains(string)
								|| plugin.getFancyName().toLowerCase().contains(string))
							search_results_array.add(new SearchResult(SearchResultType.PLUGIN, plugin, c));
					}
				}
				
				// Search in grids
				if (search_cachedGridsList == null)
					search_cachedGridsList = MuninFoo.getInstance().sqlite.dbHlpr.getGridsNames();
				
				for (String grid : search_cachedGridsList) {
					if (grid.toLowerCase().contains(string))
						search_results_array.add(new SearchResult(SearchResultType.GRID, grid, c));
				}
				
				// Search in labels
				for (Label label : MuninFoo.getInstance().labels) {
					if (label.getName().toLowerCase().contains(string))
						search_results_array.add(new SearchResult(SearchResultType.LABEL, label, c));
				}
				
				search_results_adapter.notifyDataSetChanged();
			}
			
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
		});
		search_results.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
				SearchResult searchResult = (SearchResult) search_results_array.get(position);
				searchResult.onClick(a);
			}
		});
		
		// Cancel button listener
		search.setOnTouchListener(new OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility") @Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (event.getX() >= (search.getRight() - search.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
						search.setText("");
						Util.hideKeyboard(a, search);
					}
				}
				
				return false;
			}
		});
	}
	
	public void closeDrawerIfOpened() {
		if (sm != null && sm.isMenuShowing())
			sm.toggle(true);
	}
	
	private void setSelectedMenuItem(String menuItemName) {
		if (menuItemName.equals("graphs")) {
			a.findViewById(R.id.drawer_button_graphs_ll).setPadding(7, 0, 0, 0);
			((TextView)a.findViewById(R.id.drawer_graphs_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			a.findViewById(R.id.drawer_button_graphs_border2).setVisibility(View.VISIBLE);
		} else if (menuItemName.equals("grid")) {
			a.findViewById(R.id.drawer_button_grid_ll).setPadding(7, 0, 0, 0);
			((TextView)a.findViewById(R.id.drawer_grid_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			a.findViewById(R.id.drawer_button_graphs_border2).setVisibility(View.VISIBLE);
			a.findViewById(R.id.drawer_button_grid_border2).setVisibility(View.VISIBLE);
		} else if (menuItemName.equals("alerts")) {
			a.findViewById(R.id.drawer_button_alerts_ll).setPadding(7, 0, 0, 0);
			((TextView)a.findViewById(R.id.drawer_alerts_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			a.findViewById(R.id.drawer_button_grid_border2).setVisibility(View.VISIBLE);
			a.findViewById(R.id.drawer_button_alerts_border2).setVisibility(View.VISIBLE);
		} else if (menuItemName.equals("labels")) {
			a.findViewById(R.id.drawer_button_labels_ll).setPadding(7, 0, 0, 0);
			((TextView)a.findViewById(R.id.drawer_labels_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			a.findViewById(R.id.drawer_button_alerts_border2).setVisibility(View.VISIBLE);
			a.findViewById(R.id.drawer_button_labels_border2).setVisibility(View.VISIBLE);
		} else if (menuItemName.equals("servers")) {
			a.findViewById(R.id.drawer_button_servers_ll).setPadding(7, 0, 0, 0);
			((TextView)a.findViewById(R.id.drawer_servers_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			a.findViewById(R.id.drawer_button_labels_border2).setVisibility(View.VISIBLE);
			a.findViewById(R.id.drawer_button_servers_border2).setVisibility(View.VISIBLE);
		} else if (menuItemName.equals("notifications")) {
			a.findViewById(R.id.drawer_button_notifications_ll).setPadding(7, 0, 0, 0);
			((TextView)a.findViewById(R.id.drawer_notifications_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			a.findViewById(R.id.drawer_button_servers_border2).setVisibility(View.VISIBLE);
			a.findViewById(R.id.drawer_button_notifications_border2).setVisibility(View.VISIBLE);
		} else if (menuItemName.equals("labels")) {
			a.findViewById(R.id.drawer_button_labels_ll).setPadding(7, 0, 0, 0);
			((TextView)a.findViewById(R.id.drawer_labels_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			a.findViewById(R.id.drawer_button_alerts_border2).setVisibility(View.VISIBLE);
			a.findViewById(R.id.drawer_button_labels_border2).setVisibility(View.VISIBLE);
		} else if (menuItemName.equals("premium")) {
			a.findViewById(R.id.drawer_button_premium_ll).setPadding(7, 0, 0, 0);
			((TextView)a.findViewById(R.id.drawer_premium_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			a.findViewById(R.id.drawer_button_notifications_border2).setVisibility(View.VISIBLE);
			a.findViewById(R.id.drawer_button_premium_border2).setVisibility(View.VISIBLE);
		} else if (menuItemName.equals("")) {
			((TextView)a.findViewById(R.id.drawer_graphs_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_grid_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_alerts_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_labels_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_servers_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_notifications_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_premium_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
		}
	}
	
	private void initServersList() {
		a.findViewById(R.id.drawer_scrollviewServers).setVisibility(View.VISIBLE);
		a.findViewById(R.id.drawer_button_notifications_border1).setVisibility(View.VISIBLE);
		LayoutInflater vi = (LayoutInflater) a.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		for (MuninMaster master : m.masters) {
			for (final MuninServer s : master.getOrderedChildren()) {
				View v = vi.inflate(R.layout.drawer_subbutton, null);
				TextView b = (TextView)v.findViewById(R.id.button);
				b.setText(s.getName());
				
				b.setOnClickListener(new OnClickListener() {
					public void onClick (View v) {
						m.currentServer = s;
						Intent intent = new Intent(a, Activity_Server.class);
						intent.putExtra("contextServerUrl", s.getServerUrl());
						intent.putExtra("action", "edit");
						a.startActivity(intent);
						a.overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
					}
				});
				
				View insertPoint = a.findViewById(R.id.drawer_scrollviewServers);
				((ViewGroup) insertPoint).addView(v);
			}
		}
	}
	
	public void initPluginsList() {
		initPluginsList(-1);
	}
	
	public int getDrawerScrollY() {
		ScrollView v = (ScrollView)a.findViewById(R.id.drawer_scrollview);
		if (v != null)
			return v.getScrollY();
		return 0;
	}
	
	public void initPluginsList(final int scrollY) {
		// Borders
		a.findViewById(R.id.drawer_button_graphs_border2).setVisibility(View.VISIBLE);
		a.findViewById(R.id.drawer_button_grid_border1).setVisibility(View.VISIBLE);
		
		((LinearLayout)a.findViewById(R.id.drawer_containerPlugins)).removeAllViews();
		
		a.findViewById(R.id.drawer_containerPlugins).setVisibility(View.VISIBLE);
		LayoutInflater vi = (LayoutInflater) a.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		int vfpos = vf.getSelectedItemPosition();
		int pos = 0;
		for (final MuninPlugin mp : m.currentServer.getPlugins()) {
			View v = vi.inflate(R.layout.drawer_subbutton, null);
			final TextView b = (TextView)v.findViewById(R.id.button);
			b.setText(mp.getFancyName());
			
			if (vfpos == pos) {
				final int position = pos;
				b.setBackgroundResource(R.drawable.drawer_selectedsubbutton);
				b.setTextColor(c.getResources().getColor(R.color.cffffff));
				
				// setScrollY
				final ViewTreeObserver obs = b.getViewTreeObserver();
				obs.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { // Else getHeight returns 0
					@Override
					public void onGlobalLayout() {
						int scroll = 0;
						if (scrollY != -1)
							scroll = scrollY;
						else
							scroll = (b.getHeight() + 1) * position;
						((ScrollView)a.findViewById(R.id.drawer_scrollview)).setScrollY(scroll);
					}
				});
				
			}
			
			b.setOnClickListener(new OnClickListener() {
				public void onClick (View v) {
					TextView b = (TextView) v;
					int p = 0;
					for (int i=0; i<m.currentServer.getPlugins().size(); i++) {
						if (m.currentServer.getPlugin(i).getFancyName().equals(b.getText().toString())) {
							p = i;
							break;
						}
					}
					vf.setSelection(p);
					initPluginsList(((ScrollView)a.findViewById(R.id.drawer_scrollview)).getScrollY());
					sm.toggle(true);
				}
			});
			
			View insertPoint = a.findViewById(R.id.drawer_containerPlugins);
			((ViewGroup) insertPoint).addView(v);
			pos++;
		}
	}
	
	private class SearchAdapter extends BaseAdapter {
		private ArrayList<SearchResult> searchArrayList;
		private Context context;
		private LayoutInflater mInflater;
		
		private SearchAdapter(Context context, ArrayList<SearchResult> results) {
			this.searchArrayList = results;
			this.mInflater = LayoutInflater.from(context);
			this.context = context;
		}
		
		public int getCount() { return this.searchArrayList.size(); }
		public Object getItem(int position) { return this.searchArrayList.get(position); }
		public long getItemId(int position) { return position; }
		
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = mInflater.inflate(R.layout.twolineslist, null);
			
			TextView ed_line_a = (TextView) convertView.findViewById(R.id.line_a);
			ed_line_a.setText(searchArrayList.get(position).getLine1());
			String line_b = searchArrayList.get(position).getLine2();
			TextView ed_line_b = ((TextView) convertView.findViewById(R.id.line_b));
			if (line_b != null && line_b.equals(""))
				ed_line_b.setVisibility(View.GONE);
			else
				ed_line_b.setText(line_b);
			
			Util.Fonts.setFont(context, ed_line_a, CustomFont.RobotoCondensed_Regular);
			Util.Fonts.setFont(context, ed_line_b, CustomFont.RobotoCondensed_Regular);
			
			return convertView;
		}
	}
}