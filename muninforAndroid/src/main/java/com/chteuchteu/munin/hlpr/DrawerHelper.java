package com.chteuchteu.munin.hlpr;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import com.chteuchteu.munin.ui.Activity_GraphView;
import com.chteuchteu.munin.ui.Activity_Grid;
import com.chteuchteu.munin.ui.Activity_Grids;
import com.chteuchteu.munin.ui.Activity_Labels;
import com.chteuchteu.munin.ui.Activity_Notifications;
import com.chteuchteu.munin.ui.Activity_Plugins;
import com.chteuchteu.munin.ui.Activity_Servers;
import com.chteuchteu.munin.ui.MuninActivity;

import org.taptwo.android.widget.ViewFlow;

import java.util.ArrayList;
import java.util.List;


@SuppressLint("InflateParams")
public class DrawerHelper {
	private ActionBarActivity activity;
	private Context context;
	private MuninFoo muninFoo;
	private MuninActivity currentActivity;
	private DrawerLayout drawerLayout;

	public enum DrawerMenuItem { None, Servers, Alerts, Graphs, Notifications, Labels, Premium, Grid }
	
	private EditText search;
	private ListView search_results;
	private SearchAdapter search_results_adapter;
	private ArrayList<SearchResult> search_results_array;
	private List<String> search_cachedGridsList;

	private int pluginsList_currentlySelectedItem;
	
	public DrawerHelper(ActionBarActivity activity, MuninFoo muninFoo) {
		this.activity = activity;
		this.muninFoo = muninFoo;
		this.context = activity;
		initDrawer();
	}
	
	public void reset() {
		initDrawer();
		setDrawerActivity(currentActivity);
	}
	
	public void setDrawerActivity(MuninActivity activity) {
		this.currentActivity = activity;
		setSelectedMenuItem(activity == null ? DrawerMenuItem.None : activity.getDrawerMenuItem());
	}

	public void toggle() {
		if (drawerLayout.isDrawerVisible(Gravity.START))
			drawerLayout.closeDrawer(Gravity.START);
		else
			drawerLayout.openDrawer(Gravity.START);
	}

	public DrawerLayout getDrawerLayout() { return this.drawerLayout; }

	private int getIntentFlag() {
		return this.currentActivity instanceof Activity_Grid ? Intent.FLAG_ACTIVITY_CLEAR_TOP
				: Intent.FLAG_ACTIVITY_NEW_TASK;
	}

	private void initDrawer() {
		drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawerLayout);

		activity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);

		// Graphs
		activity.findViewById(R.id.drawer_graphs_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Activity_Plugins.class);
			}
		});
		activity.findViewById(R.id.drawer_grid_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Activity_Grids.class);
			}
		});
		// Alerts
		activity.findViewById(R.id.drawer_alerts_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Activity_Alerts.class);
			}
		});
		// Labels
		activity.findViewById(R.id.drawer_labels_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Activity_Labels.class);
			}
		});
		// Servers
		activity.findViewById(R.id.drawer_servers_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Activity_Servers.class);
			}
		});
		// Notifications
		activity.findViewById(R.id.drawer_notifications_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Activity_Notifications.class);
			}
		});
		// Premium
		activity.findViewById(R.id.drawer_premium_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(Activity_GoPremium.class);
			}
		});
		// Support
		activity.findViewById(R.id.drawer_support_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent send = new Intent(Intent.ACTION_SENDTO);
				String uriText = "mailto:" + Uri.encode("support@munin-for-android.com") + 
						"?subject=" + Uri.encode("Support request");
				Uri uri = Uri.parse(uriText);
				
				send.setData(uri);
				activity.startActivity(Intent.createChooser(send, context.getString(R.string.choose_email_client)));
			}
		});
		// Donate
		activity.findViewById(R.id.drawer_donate_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				donate();
			}
		});
		
		if (!muninFoo.premium) {
			activity.findViewById(R.id.drawer_notifications_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_grid_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_notifications_icon).setAlpha(0.5f);
			activity.findViewById(R.id.drawer_notifications_txt).setAlpha(0.5f);
			activity.findViewById(R.id.drawer_grids_icon).setAlpha(0.5f);
			activity.findViewById(R.id.drawer_grids_txt).setAlpha(0.5f);
			activity.findViewById(R.id.drawer_premium_btn).setVisibility(View.VISIBLE);
		}
		if (muninFoo.getServers().size() == 0) {
			activity.findViewById(R.id.drawer_graphs_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_grid_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_alerts_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_notifications_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_labels_btn).setEnabled(false);
		}
		
		Util.Fonts.setFont(context, (ViewGroup) activity.findViewById(R.id.drawer_scrollview), CustomFont.Roboto_Regular);

		initSearch();
	}

	private void startActivity(Class<?> targetActivity) {
		if (((Object) activity).getClass() == targetActivity)
			closeDrawerIfOpened();
		else {
			Intent intent = new Intent(activity, targetActivity);
			intent.addFlags(getIntentFlag());
			activity.startActivity(intent);
			Util.setTransition(activity, TransitionStyle.DEEPER);
		}
	}

	private void initSearch() {
		search = (EditText) activity.findViewById(R.id.drawer_search);
		search_results = (ListView) activity.findViewById(R.id.drawer_search_results);
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
					activity.findViewById(R.id.drawer_scrollview).setVisibility(View.VISIBLE);
					activity.findViewById(R.id.drawer_search_results).setVisibility(View.GONE);
					search.getCompoundDrawables()[DRAWABLE_RIGHT].setAlpha(0);
					return;
				} else {
					activity.findViewById(R.id.drawer_scrollview).setVisibility(View.GONE);
					activity.findViewById(R.id.drawer_search_results).setVisibility(View.VISIBLE);
					search.getCompoundDrawables()[DRAWABLE_RIGHT].setAlpha(255);
				}

				if (search_results_adapter != null) {
					search_results_array.clear();
					search_results_adapter.notifyDataSetChanged();
				} else {
					search_results_array = new ArrayList<>();
					search_results_adapter = new SearchAdapter(activity, search_results_array);
					search_results.setAdapter(search_results_adapter);
				}

				// Search in plugins and servers
				for (MuninServer server : MuninFoo.getInstance(context).getServers()) {
					String serverName = server.getName().toLowerCase();
					String serverUrl = server.getServerUrl().toLowerCase();

					if (serverName.contains(string) || serverUrl.contains(string))
						search_results_array.add(new SearchResult(SearchResultType.SERVER, server, context));


					for (MuninPlugin plugin : server.getPlugins()) {
						if (plugin.getName().toLowerCase().contains(string)
								|| plugin.getFancyName().toLowerCase().contains(string))
							search_results_array.add(new SearchResult(SearchResultType.PLUGIN, plugin, context));
					}
				}

				// Search in grids
				if (search_cachedGridsList == null)
					search_cachedGridsList = MuninFoo.getInstance(context).sqlite.dbHlpr.getGridsNames();

				for (String grid : search_cachedGridsList) {
					if (grid.toLowerCase().contains(string))
						search_results_array.add(new SearchResult(SearchResultType.GRID, grid, context));
				}

				// Search in labels
				for (Label label : MuninFoo.getInstance(context).labels) {
					if (label.getName().toLowerCase().contains(string))
						search_results_array.add(new SearchResult(SearchResultType.LABEL, label, context));
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
				searchResult.onClick(activity);
			}
		});

		// Cancel button listener
		search.setOnTouchListener(new OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility") @Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (event.getX() >= (search.getRight() - search.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
						search.setText("");
						Util.hideKeyboard(activity, search);
					}
				}

				return false;
			}
		});
	}

	private void donate() {
		new AlertDialog.Builder(activity)
				.setTitle(R.string.donate)
				.setMessage(R.string.donate_text)
				.setPositiveButton(R.string.donate, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						View view = inflater.inflate(R.layout.dialog_donate, null);

						final Spinner spinnerAmount = (Spinner) view.findViewById(R.id.donate_amountSpinner);
						List<String> list = new ArrayList<>();
						String euroSlashDollar = "\u20Ac/\u0024";
						list.add("1 " + euroSlashDollar);
						list.add("2 " + euroSlashDollar);
						list.add("5 " + euroSlashDollar);
						list.add("20 " + euroSlashDollar);
						ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, list);
						dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						spinnerAmount.setAdapter(dataAdapter);

						new AlertDialog.Builder(activity)
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
										new DonateAsync(activity, product).execute();
									}
								})
								.setNegativeButton(R.string.text64, null)
								.show();
					}
				})
				.setNegativeButton(R.string.text64, null)
				.show();
	}

	public void closeDrawerIfOpened() {
		if (drawerLayout.isDrawerOpen(Gravity.START))
			drawerLayout.closeDrawer(Gravity.START);
	}
	
	private void setSelectedMenuItem(DrawerMenuItem menuItemName) {
		int textViewResId = -1;
		int iconResId = -1;


		switch (menuItemName) {
			case Graphs: {
				textViewResId = R.id.drawer_graphs_txt;
				iconResId = R.id.drawer_graphs_icon;
				break;
			}
			case Grid: {
				textViewResId = R.id.drawer_grids_txt;
				iconResId = R.id.drawer_grids_icon;
				break;
			}
			case Alerts: {
				textViewResId = R.id.drawer_alerts_txt;
				iconResId = R.id.drawer_alerts_icon;
				break;
			}
			case Labels: {
				textViewResId = R.id.drawer_labels_txt;
				iconResId = R.id.drawer_labels_icon;
				break;
			}
			case Servers: {
				textViewResId = R.id.drawer_servers_txt;
				iconResId = R.id.drawer_servers_icon;
				break;
			}
			case Notifications: {
				textViewResId = R.id.drawer_notifications_txt;
				iconResId = R.id.drawer_notifications_icon;
				break;
			}
			case Premium: {
				textViewResId = R.id.drawer_premium_txt;
				iconResId = R.id.drawer_premium_icon;
				break;
			}
			case None: break;
		}

		if (textViewResId != -1) {
			int selectedDrawerItemColor = context.getResources().getColor(R.color.selectedDrawerItem);
			TextView textView = (TextView) activity.findViewById(textViewResId);
			textView.setTextColor(selectedDrawerItemColor);
			Util.Fonts.setFont(context, textView, CustomFont.Roboto_Medium);
			ImageView icon = (ImageView) activity.findViewById(iconResId);
			icon.setColorFilter(selectedDrawerItemColor, Mode.MULTIPLY);
		}
	}
	
	public void initPluginsList() {
		initPluginsList(-1);
	}

	public void initPluginsList(final int scrollY) {
		((LinearLayout)activity.findViewById(R.id.drawer_containerPlugins)).removeAllViews();
		
		activity.findViewById(R.id.drawer_containerPlugins).setVisibility(View.VISIBLE);
		LayoutInflater vi = (LayoutInflater) activity.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final Activity_GraphView activityGraphView = (Activity_GraphView) activity;
		final ViewFlow vf = activityGraphView.viewFlow;


		int vfpos = vf.getSelectedItemPosition();
		this.pluginsList_currentlySelectedItem = vfpos;
		int pos = 0;
		View insertPoint = activity.findViewById(R.id.drawer_containerPlugins);

		for (final MuninPlugin mp : muninFoo.getCurrentServer().getPlugins()) {
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
						activity.findViewById(R.id.drawer_scrollview).setScrollY(scroll);
					}
				});
			}
			
			b.setOnClickListener(new OnClickListener() {
				public void onClick (View v) {
					TextView b = (TextView) v;
					int p = 0;
					for (int i=0; i<muninFoo.getCurrentServer().getPlugins().size(); i++) {
						if (muninFoo.getCurrentServer().getPlugin(i).getFancyName().equals(b.getText().toString())) {
							p = i;
							break;
						}
					}
					vf.setSelection(p);
					initPluginsList(activity.findViewById(R.id.drawer_scrollview).getScrollY());
					if (activityGraphView.isDynazoomOpen())
						activityGraphView.hideDynazoom();
					toggle();
				}
			});

			((ViewGroup) insertPoint).addView(v);
			pos++;
		}
	}

	/**
	 * Update selected item in plugins list
	 */
	public void updatePluginsList() {
		// Remove background styling for previously selected element
		ViewGroup container = (ViewGroup) activity.findViewById(R.id.drawer_containerPlugins);
		View selectedItem = container.getChildAt(this.pluginsList_currentlySelectedItem);

		TextView tv = (TextView) selectedItem.findViewById(R.id.button);
		tv.setBackgroundColor(Color.TRANSPARENT);
		tv.setTextColor(0xffbbbbbb);

		// Set background styling for current element
		final Activity_GraphView activityGraphView = (Activity_GraphView) activity;
		final ViewFlow vf = activityGraphView.viewFlow;

		this.pluginsList_currentlySelectedItem = vf.getSelectedItemPosition();

		View newSelectedItem = container.getChildAt(this.pluginsList_currentlySelectedItem);
		final TextView tv2 = (TextView) newSelectedItem.findViewById(R.id.button);
		tv2.setBackgroundResource(R.drawable.drawer_selectedsubbutton);
		tv2.setTextColor(0xffffffff);

		// setScrollY
		int scroll = (newSelectedItem.getHeight() + 1) * pluginsList_currentlySelectedItem;
		activity.findViewById(R.id.drawer_scrollview).setScrollY(scroll);
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