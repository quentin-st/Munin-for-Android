package com.chteuchteu.munin.hlpr;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.async.DonateAsync;
import com.chteuchteu.munin.hlpr.Util.Fonts.CustomFont;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.ui.Activity_Alerts;
import com.chteuchteu.munin.ui.Activity_GoPremium;
import com.chteuchteu.munin.ui.Activity_Grid;
import com.chteuchteu.munin.ui.Activity_Grids;
import com.chteuchteu.munin.ui.Activity_Labels;
import com.chteuchteu.munin.ui.Activity_Notifications;
import com.chteuchteu.munin.ui.Activity_Plugins;
import com.chteuchteu.munin.ui.Activity_Servers;
import com.chteuchteu.munin.ui.MuninActivity;

import java.util.ArrayList;
import java.util.List;

public class DrawerHelper {
	private AppCompatActivity activity;
	private Context context;
	private MuninFoo muninFoo;
	private MuninActivity currentActivity;
	private DrawerLayout drawerLayout;

	public enum DrawerMenuItem { None, Servers, Alerts, Graphs, Notifications, Labels, Premium, Grid }

    private SearchHelper searchHelper;
	
	public DrawerHelper(AppCompatActivity activity, MuninFoo muninFoo) {
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
		if (muninFoo.getNodes().size() == 0) {
			activity.findViewById(R.id.drawer_graphs_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_grid_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_alerts_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_notifications_btn).setEnabled(false);
			activity.findViewById(R.id.drawer_labels_btn).setEnabled(false);
		}
		
		Util.Fonts.setFont(context, (ViewGroup) activity.findViewById(R.id.drawer_scrollview), CustomFont.Roboto_Regular);

		if (this.searchHelper == null)
            this.searchHelper = new SearchHelper(activity);
        this.searchHelper.initSearch();
	}

	private void startActivity(Class<?> targetActivity) {
		if (((Object) activity).getClass() == targetActivity)
			closeDrawerIfOpen();
		else {
			Intent intent = new Intent(activity, targetActivity);
			intent.addFlags(getIntentFlag());
			activity.startActivity(intent);
			Util.setTransition(activity, TransitionStyle.DEEPER);
		}
	}

	private void donate() {
		new AlertDialog.Builder(activity)
				.setTitle(R.string.donate)
				.setMessage(R.string.donate_text)
				.setPositiveButton(R.string.donate, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						@SuppressLint("InflateParams")
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

    /**
     * Close drawer if it is open
     * @return boolean true if drawer has been closed
     */
	public boolean closeDrawerIfOpen() {
		if (drawerLayout.isDrawerOpen(Gravity.START)) {
            drawerLayout.closeDrawer(Gravity.START);
            return true;
        }
        return false;
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

    public SearchHelper getSearchHelper() { return this.searchHelper; }
}
