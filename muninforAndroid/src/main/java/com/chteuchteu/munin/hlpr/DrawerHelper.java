package com.chteuchteu.munin.hlpr;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Label;
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
import com.chteuchteu.munin.ui.Activity_Servers;

import org.taptwo.android.widget.ViewFlow;

import java.util.ArrayList;
import java.util.List;


@SuppressLint("InflateParams")
public class DrawerHelper {
	public static final int Activity_About = -1;
	public static final int Activity_Alerts = 2;
	public static final int Activity_AlertsPluginSelection = 2;
	public static final int Activity_Label = 9;
	public static final int Activity_Labels = 9;
	public static final int Activity_GoPremium = 10;
	public static final int Activity_GraphView = 3;
	public static final int Activity_Grid = 11;
	public static final int Activity_Grids = 11;
	public static final int Activity_Main = 0;
	public static final int Activity_Notifications = 4;
	public static final int Activity_Plugins = 7;
	public static final int Activity_Server_Add = 1;
	public static final int Activity_Servers = 5;
	public static final int Activity_ServersEdit = 5;
	public static final int Activity_Settings = 6;
	
	private ActionBarActivity a;
	private Context c;
	private MuninFoo m;
	private int n;
	//private SlidingMenu sm;
	private DrawerLayout drawerLayout;
	
	private EditText search;
	private ListView search_results;
	private SearchAdapter search_results_adapter;
	private ArrayList<SearchResult> search_results_array;
	private List<String> search_cachedGridsList;
	
	public DrawerHelper(ActionBarActivity a, MuninFoo m) {
		this.a = a;
		this.m = m;
		this.c = a.getApplicationContext();
		initDrawer();
	}
	
	public void reset() {
		initDrawer();
		setDrawerActivity(n);
	}
	
	public void setDrawerActivity(int activity) {
		this.n = activity;
		switch (activity) {
		case Activity_Main:
			// Home : nothing
			setSelectedMenuItem("");
			break;
		case Activity_Server_Add:
			setSelectedMenuItem("servers");
			break;
		case Activity_Alerts:
			setSelectedMenuItem("alerts");
			break;
		case Activity_GraphView:
			setSelectedMenuItem("graphs");
			//initPluginsList();
			break;
		case Activity_Notifications:
			setSelectedMenuItem("notifications");
			break;
		case Activity_Servers: // Activity_ServersEdit
			setSelectedMenuItem("servers");
			break;
		case Activity_Settings:
			// Nothing selected (ActionBar)
			setSelectedMenuItem("");
			break;
		case Activity_Plugins:
			setSelectedMenuItem("graphs");
			break;
		case Activity_Label: // Activity_Labels
			setSelectedMenuItem("labels");
			break;
		case Activity_GoPremium:
			setSelectedMenuItem("premium");
			break;
		case Activity_Grid: // Activity_Grids
			setSelectedMenuItem("grid");
			break;
		default:
			setSelectedMenuItem("");
			break;
		}
	}

	public void toggle() {
		if (drawerLayout.isDrawerVisible(Gravity.START))
			drawerLayout.closeDrawer(Gravity.START);
		else
			drawerLayout.openDrawer(Gravity.START);
	}

	public DrawerLayout getDrawerLayout() { return this.drawerLayout; }

	private void initDrawer() {
		drawerLayout = (DrawerLayout) a.findViewById(R.id.drawerLayout);
		
		a.getSupportActionBar().setDisplayHomeAsUpEnabled(false);

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
		// Support
		a.findViewById(R.id.drawer_support_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent send = new Intent(Intent.ACTION_SENDTO);
				String uriText = "mailto:" + Uri.encode("support@munin-for-android.com") + 
						"?subject=" + Uri.encode("Support request");
				Uri uri = Uri.parse(uriText);
				
				send.setData(uri);
				a.startActivity(Intent.createChooser(send, c.getString(R.string.choose_email_client)));
			}
		});
		// Donate
		a.findViewById(R.id.drawer_donate_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(a)
				.setTitle(R.string.donate)
				.setMessage(R.string.donate_text)
				.setPositiveButton(R.string.donate, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						LayoutInflater inflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						View view = inflater.inflate(R.layout.dialog_donate, null);
						
						final Spinner spinnerAmount = (Spinner) view.findViewById(R.id.donate_amountSpinner);
						List<String> list = new ArrayList<>();
						String euroSlashDollar = "\u20Ac/\u0024";
						list.add("1 " + euroSlashDollar);
						list.add("2 " + euroSlashDollar);
						list.add("5 " + euroSlashDollar);
						list.add("20 " + euroSlashDollar);
						ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(a, android.R.layout.simple_spinner_item, list);
						dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						spinnerAmount.setAdapter(dataAdapter);
						
						new AlertDialog.Builder(a)
						.setTitle(R.string.donate)
						.setView(view)
						.setPositiveButton(R.string.donate, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Launch BillingService, and then purchase the thing
								String product = "";
								switch (spinnerAmount.getSelectedItemPosition()) {
									case 0: product = BillingService.DONATE_1; break;
									case 1: product = BillingService.DONATE_2; break;
									case 2: product = BillingService.DONATE_5; break;
									case 3: product = BillingService.DONATE_20; break;
								}
								new DonateAsync(a, product).execute();
							}
						})
						.setNegativeButton(R.string.text64, null)
						.show();
					}
				})
				.setNegativeButton(R.string.text64, null)
				.show();
			}
		});
		
		if (!m.premium) {
			a.findViewById(R.id.drawer_notifications_btn).setEnabled(false);
			a.findViewById(R.id.drawer_grid_btn).setEnabled(false);
			a.findViewById(R.id.drawer_notifications_icon).setAlpha(0.5f);
			a.findViewById(R.id.drawer_notifications_txt).setAlpha(0.5f);
			a.findViewById(R.id.drawer_grids_icon).setAlpha(0.5f);
			a.findViewById(R.id.drawer_grids_txt).setAlpha(0.5f);
			a.findViewById(R.id.drawer_premium_btn).setVisibility(View.VISIBLE);
		}
		if (m.getServers().size() == 0) {
			a.findViewById(R.id.drawer_graphs_btn).setEnabled(false);
			a.findViewById(R.id.drawer_grid_btn).setEnabled(false);
			a.findViewById(R.id.drawer_alerts_btn).setEnabled(false);
			a.findViewById(R.id.drawer_notifications_btn).setEnabled(false);
			a.findViewById(R.id.drawer_labels_btn).setEnabled(false);
		}
		
		Util.Fonts.setFont(c, (ViewGroup) a.findViewById(R.id.drawer_scrollview), CustomFont.Roboto_Regular);
		
		// Init search
		search = (EditText) a.findViewById(R.id.drawer_search);
		search_results = (ListView) a.findViewById(R.id.drawer_search_results);
		search_results.setVisibility(View.VISIBLE);
		
		
		// Cancel button
		//final int DRAWABLE_LEFT = 0;
		//final int DRAWABLE_TOP = 1;
		final int DRAWABLE_RIGHT = 2;
		//final int DRAWABLE_BOTTOM = 3;
		search.getCompoundDrawables()[DRAWABLE_RIGHT].setAlpha(0);
		
		search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				return actionId == EditorInfo.IME_ACTION_SEARCH;
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
					search_results_array = new ArrayList<>();
					search_results_adapter = new SearchAdapter(a, search_results_array);
					search_results.setAdapter(search_results_adapter);
				}
				
				// Search in plugins and servers
				for (MuninServer server : MuninFoo.getInstance(c).getServers()) {
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
					search_cachedGridsList = MuninFoo.getInstance(c).sqlite.dbHlpr.getGridsNames();
				
				for (String grid : search_cachedGridsList) {
					if (grid.toLowerCase().contains(string))
						search_results_array.add(new SearchResult(SearchResultType.GRID, grid, c));
				}
				
				// Search in labels
				for (Label label : MuninFoo.getInstance(c).labels) {
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
				SearchResult searchResult = search_results_array.get(position);
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
		if (drawerLayout.isDrawerOpen(Gravity.START))
			drawerLayout.closeDrawer(Gravity.START);
	}
	
	private void setSelectedMenuItem(String menuItemName) {
		switch (menuItemName) {
			case "graphs": {
				TextView tv = (TextView) a.findViewById(R.id.drawer_graphs_txt);
				tv.setTextColor(c.getResources().getColor(R.color.selectedDrawerItem));
				Util.Fonts.setFont(c, tv, CustomFont.Roboto_Medium);
				((ImageView) a.findViewById(R.id.drawer_graphs_icon)).setColorFilter(c.getResources().getColor(R.color.selectedDrawerItem), Mode.MULTIPLY);
				break;
			}
			case "grid": {
				TextView tv = (TextView) a.findViewById(R.id.drawer_grids_txt);
				tv.setTextColor(c.getResources().getColor(R.color.selectedDrawerItem));
				Util.Fonts.setFont(c, tv, CustomFont.Roboto_Medium);
				((ImageView) a.findViewById(R.id.drawer_grids_icon)).setColorFilter(c.getResources().getColor(R.color.selectedDrawerItem), Mode.MULTIPLY);
				break;
			}
			case "alerts": {
				TextView tv = (TextView) a.findViewById(R.id.drawer_alerts_txt);
				tv.setTextColor(c.getResources().getColor(R.color.selectedDrawerItem));
				Util.Fonts.setFont(c, tv, CustomFont.Roboto_Medium);
				((ImageView) a.findViewById(R.id.drawer_alerts_icon)).setColorFilter(c.getResources().getColor(R.color.selectedDrawerItem), Mode.MULTIPLY);
				break;
			}
			case "labels": {
				TextView tv = (TextView) a.findViewById(R.id.drawer_labels_txt);
				tv.setTextColor(c.getResources().getColor(R.color.selectedDrawerItem));
				Util.Fonts.setFont(c, tv, CustomFont.Roboto_Medium);
				((ImageView) a.findViewById(R.id.drawer_labels_icon)).setColorFilter(c.getResources().getColor(R.color.selectedDrawerItem), Mode.MULTIPLY);
				break;
			}
			case "servers": {
				TextView tv = (TextView) a.findViewById(R.id.drawer_servers_txt);
				tv.setTextColor(c.getResources().getColor(R.color.selectedDrawerItem));
				Util.Fonts.setFont(c, tv, CustomFont.Roboto_Medium);
				((ImageView) a.findViewById(R.id.drawer_servers_icon)).setColorFilter(c.getResources().getColor(R.color.selectedDrawerItem), Mode.MULTIPLY);
				break;
			}
			case "notifications": {
				TextView tv = (TextView) a.findViewById(R.id.drawer_notifications_txt);
				tv.setTextColor(c.getResources().getColor(R.color.selectedDrawerItem));
				Util.Fonts.setFont(c, tv, CustomFont.Roboto_Medium);
				((ImageView) a.findViewById(R.id.drawer_notifications_icon)).setColorFilter(c.getResources().getColor(R.color.selectedDrawerItem), Mode.MULTIPLY);
				break;
			}
			case "premium": {
				TextView tv = (TextView) a.findViewById(R.id.drawer_premium_txt);
				tv.setTextColor(c.getResources().getColor(R.color.selectedDrawerItem));
				Util.Fonts.setFont(c, tv, CustomFont.Roboto_Medium);
				((ImageView) a.findViewById(R.id.drawer_premium_icon)).setColorFilter(c.getResources().getColor(R.color.selectedDrawerItem), Mode.MULTIPLY);
				break;
			}
			case "":
				((TextView) a.findViewById(R.id.drawer_graphs_txt)).setTextColor(0xffffffff);
				((TextView) a.findViewById(R.id.drawer_grids_txt)).setTextColor(0xffffffff);
				((TextView) a.findViewById(R.id.drawer_alerts_txt)).setTextColor(0xffffffff);
				((TextView) a.findViewById(R.id.drawer_labels_txt)).setTextColor(0xffffffff);
				((TextView) a.findViewById(R.id.drawer_servers_txt)).setTextColor(0xffffffff);
				((TextView) a.findViewById(R.id.drawer_notifications_txt)).setTextColor(0xffffffff);
				((TextView) a.findViewById(R.id.drawer_premium_txt)).setTextColor(0xffffffff);
				break;
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
		((LinearLayout)a.findViewById(R.id.drawer_containerPlugins)).removeAllViews();
		
		a.findViewById(R.id.drawer_containerPlugins).setVisibility(View.VISIBLE);
		LayoutInflater vi = (LayoutInflater) a.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final com.chteuchteu.munin.ui.Activity_GraphView activity = (com.chteuchteu.munin.ui.Activity_GraphView) a;
		final ViewFlow vf = activity.viewFlow;


		int vfpos = vf.getSelectedItemPosition();
		int pos = 0;
		for (final MuninPlugin mp : m.getCurrentServer().getPlugins()) {
			View v = vi.inflate(R.layout.drawer_subbutton, null);
			final TextView b = (TextView)v.findViewById(R.id.button);
			b.setText(mp.getFancyName());
			
			if (vfpos == pos) {
				final int position = pos;
				b.setBackgroundResource(R.drawable.drawer_selectedsubbutton);
				b.setTextColor(0xffffffff);
				
				// setScrollY
				b.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() { // Else getHeight returns 0
					@Override
					public void onGlobalLayout() {
						Util.removeOnGlobalLayoutListener(b, this);

						int scroll;
						if (scrollY != -1)
							scroll = scrollY;
						else
							scroll = (b.getHeight() + 1) * position;
						a.findViewById(R.id.drawer_scrollview).setScrollY(scroll);
					}
				});
				
			}
			
			b.setOnClickListener(new OnClickListener() {
				public void onClick (View v) {
					TextView b = (TextView) v;
					int p = 0;
					for (int i=0; i<m.getCurrentServer().getPlugins().size(); i++) {
						if (m.getCurrentServer().getPlugin(i).getFancyName().equals(b.getText().toString())) {
							p = i;
							break;
						}
					}
					vf.setSelection(p);
					initPluginsList(((ScrollView)a.findViewById(R.id.drawer_scrollview)).getScrollY());
					if (activity.isDynazoomOpen())
						activity.hideDynazoom();
					toggle();
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
	
	private class DonateAsync extends AsyncTask<Void, Integer, Void> {
		private ProgressDialog dialog;
		private Context context;
		private String product;
		
		private DonateAsync(Context context, String product) {
			this.product = product;
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			dialog = ProgressDialog.show(context, "", context.getString(R.string.loading), true);
			dialog.setCancelable(true);
		}
		
		@Override
		protected Void doInBackground(Void... arg0) {
			BillingService.getInstanceAndPurchase(context, product, dialog);
			// Dialog will be dismissed in the BillingService.
			
			return null;
		}
	}
}