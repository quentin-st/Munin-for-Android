package com.chteuchteu.munin.hlpr;

import org.taptwo.android.widget.ViewFlow;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_AddServer;
import com.chteuchteu.munin.ui.Activity_Alerts;
import com.chteuchteu.munin.ui.Activity_GoPremium;
import com.chteuchteu.munin.ui.Activity_Labels;
import com.chteuchteu.munin.ui.Activity_Notifications;
import com.chteuchteu.munin.ui.Activity_PluginSelection;
import com.chteuchteu.munin.ui.Activity_Servers;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.actionbar.ActionBarSlideIcon;


public class DrawerHelper {
	public int Activity_About = -1;
	public int Activity_AddServer_Add = 1;
	public int Activity_AddServer_Edit = 8;
	public int Activity_Alerts = 2;
	public int Activity_AlertsPluginSelection = 2;
	public int Activity_Labels = 9;
	public int Activity_LabelsPluginSelection = 9;
	public int Activity_GoPremium = 10;
	public int Activity_GraphView = 3;
	public int Activity_Main = 0;
	public int Activity_Notifications = 4;
	public int Activity_PluginSelection = 7;
	public int Activity_Servers = 5;
	public int Activity_ServersEdit = 5;
	public int Activity_Settings = 6;
	public int Activity_Settings_Comp = 6;
	public int Activity_Splash = -1;
	
	private Activity a;
	private Context c;
	private MuninFoo m;
	private int n;
	private SlidingMenu sm;
	
	// GraphView
	private ViewFlow vf;
	
	public DrawerHelper(Activity a, MuninFoo m) {
		this.a = a;
		this.m = m;
		this.c = a.getApplicationContext();
		initDrawer();
	}
	
	public SlidingMenu getDrawer() {
		return this.sm;
	}
	
	public void setDrawerActivity(int act) {
		this.n = act;
		switch (act) {
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
			default:
				setSelectedMenuItem("");
				break;
		}
	}
	
	public void setViewFlow(ViewFlow v) {
		this.vf = v;
	}
	
	public void reInitDrawer() {
		initDrawer();
	}
	
	@SuppressLint("NewApi")
	private void initDrawer() {
		sm = new SlidingMenu(a);
		sm.setMode(SlidingMenu.LEFT);
		if (a.getClass().getSimpleName().equals("Activity_Main"))
			sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		else
			sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setFadeEnabled(false);
		sm.setSelectorEnabled(true);
		sm.setBehindScrollScale(0.25f);
		//sm.setShadowDrawable(R.drawable.drawer_shadow);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		//sm.attachToActivity(a, SlidingMenu.SLIDING_WINDOW);
		sm.attachToActivity(a, SlidingMenu.SLIDING_CONTENT);
		if (Util.deviceHasBackKey(c));
			sm.setActionBarSlideIcon(new ActionBarSlideIcon(a, R.drawable.ic_navigation_drawer, R.string.text63_1, R.string.text63_2));
		
		sm.setMenu(R.layout.drawer);
		
		// Graphs
		a.findViewById(R.id.drawer_graphs_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				a.startActivity(new Intent(a, Activity_PluginSelection.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				setTransition("deeper");
			}
		});
		// Alerts
		a.findViewById(R.id.drawer_alerts_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				a.startActivity(new Intent(a, Activity_Alerts.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				setTransition("deeper");
			}
		});
		// Labels
		a.findViewById(R.id.drawer_labels_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				a.startActivity(new Intent(a, Activity_Labels.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				setTransition("deeper");
			}
		});
		// Servers
		a.findViewById(R.id.drawer_servers_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				a.startActivity(new Intent(a, Activity_Servers.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				setTransition("deeper");
			}
		});
		// Notifications
		a.findViewById(R.id.drawer_notifications_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				a.startActivity(new Intent(a, Activity_Notifications.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				setTransition("deeper");
			}
		});
		// Premium
		a.findViewById(R.id.drawer_premium_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				a.startActivity(new Intent(a, Activity_GoPremium.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				setTransition("deeper");
			}
		});
		
		if (!m.premium) {
			a.findViewById(R.id.drawer_notifications_btn).setEnabled(false);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				a.findViewById(R.id.drawer_notifications_img).setAlpha(0.5f);
				a.findViewById(R.id.drawer_notifications_txt).setAlpha(0.5f);
			}
			a.findViewById(R.id.drawer_labels_btn).setEnabled(false);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				a.findViewById(R.id.drawer_labels_img).setAlpha(0.6f);
				a.findViewById(R.id.drawer_labels_txt).setAlpha(0.6f);
			}
			a.findViewById(R.id.drawer_button_premium_ll).setVisibility(View.VISIBLE);
		}
		if (m.getHowManyServers() == 0) {
			a.findViewById(R.id.drawer_graphs_btn).setEnabled(false);
			a.findViewById(R.id.drawer_alerts_btn).setEnabled(false);
			a.findViewById(R.id.drawer_notifications_btn).setEnabled(false);
			a.findViewById(R.id.drawer_labels_btn).setEnabled(false);
		}
		
		Util.setFont(c, (ViewGroup) a.findViewById(R.id.drawer_scrollview), Util.FONT_RobotoCondensed_Regular);
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
		} else if (menuItemName.equals("alerts")) {
			a.findViewById(R.id.drawer_button_alerts_ll).setPadding(7, 0, 0, 0);
			((TextView)a.findViewById(R.id.drawer_alerts_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			a.findViewById(R.id.drawer_button_graphs_border2).setVisibility(View.VISIBLE);
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
			((TextView)a.findViewById(R.id.drawer_alerts_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_labels_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_servers_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_notifications_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
			((TextView)a.findViewById(R.id.drawer_premium_txt)).setTextColor(c.getResources().getColor(R.color.cffffff));
		}
	}
	
	private void initServersList() {
		a.findViewById(R.id.drawer_scrollviewServers).setVisibility(View.VISIBLE);
		LayoutInflater vi = (LayoutInflater) a.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		for (final MuninServer s : m.getOrderedServers()) {
			View v = vi.inflate(R.layout.drawer_subbutton, null);
			LinearLayout l = (LinearLayout)v.findViewById(R.id.button_container);
			TextView b = (TextView)v.findViewById(R.id.button);
			b.setText(s.getName());
			
			if (n == Activity_AddServer_Edit && s.equalsApprox(m.currentServer)) {
				l.setPadding(4, 0, 0, 0);
				b.setTextColor(c.getResources().getColor(R.color.cffffff));
			}
			
			b.setOnClickListener(new OnClickListener() {
				public void onClick (View v) {
					m.currentServer = s;
					Intent intent = new Intent(a, Activity_AddServer.class);
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
	
	public void initPluginsList() {
		initPluginsList(-1);
	}
	
	public int getDrawerScrollY() {
		ScrollView v = (ScrollView)a.findViewById(R.id.drawer_scrollview);
		if (v != null)
			return v.getScrollY();
		return 0;
	}
	
	@SuppressLint("NewApi")
	public void initPluginsList(final int scrollY) {
		// Borders
		a.findViewById(R.id.drawer_button_graphs_border2).setVisibility(View.VISIBLE);
		a.findViewById(R.id.drawer_button_alerts_border1).setVisibility(View.VISIBLE);
		
		((LinearLayout)a.findViewById(R.id.drawer_containerPlugins)).removeAllViews();
		
		a.findViewById(R.id.drawer_containerPlugins).setVisibility(View.VISIBLE);
		LayoutInflater vi = (LayoutInflater) a.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		int pos = 0;
		for (final MuninPlugin mp : m.currentServer.getPlugins()) {
			View v = vi.inflate(R.layout.drawer_subbutton, null);
			LinearLayout l = (LinearLayout)v.findViewById(R.id.button_container);
			final TextView b = (TextView)v.findViewById(R.id.button);
			b.setText(mp.getFancyName());
			
			int vfpos = vf.getSelectedItemPosition();
			if (vfpos == pos) {
				final int position = pos;
				l.setPadding(4, 0, 0, 0);
				b.setTextColor(c.getResources().getColor(R.color.cffffff));
				// setScrollY
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
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
	
	
	private void setTransition(String level) {
		if (getPref("transitions").equals("true")) {
			if (level.equals("deeper"))
				a.overridePendingTransition(R.anim.deeper_in, R.anim.deeper_out);
			else if (level.equals("shallower"))
				a.overridePendingTransition(R.anim.shallower_in, R.anim.shallower_out);
		}
	}
	private String getPref(String key) {
		return a.getSharedPreferences("user_pref", Context.MODE_PRIVATE).getString(key, "");
	}
	
	public void setFont(ViewGroup g, String font) {
		Typeface mFont = Typeface.createFromAsset(a.getAssets(), font);
		setFont(g, mFont);
	}
	public void setFont(ViewGroup group, Typeface font) {
		int count = group.getChildCount();
		View v;
		for (int i = 0; i < count; i++) {
			v = group.getChildAt(i);
			if (v instanceof TextView || v instanceof EditText || v instanceof Button) {
				((TextView) v).setTypeface(font);
			} else if (v instanceof ViewGroup)
				setFont((ViewGroup) v, font);
		}
	}
	public void setLightFont(View v) {
		Typeface mFont = Typeface.createFromAsset(a.getAssets(), "Roboto-Thin.ttf");
		((TextView) v).setTypeface(mFont);
	}
}