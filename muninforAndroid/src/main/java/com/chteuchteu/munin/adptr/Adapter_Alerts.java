package com.chteuchteu.munin.adptr;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.obj.MuninServer;
import com.chteuchteu.munin.ui.Activity_Alerts;
import com.chteuchteu.munin.ui.Activity_AlertsPluginSelection;

import java.util.ArrayList;
import java.util.List;

public class Adapter_Alerts extends ArrayAdapter<MuninServer> {
	private List<AlertPart> parts;
	private Context context;

	public enum ListItemSize { REDUCED, EXPANDED }
	private ListItemSize listItemSize;

	public enum ListItemPolicy { SHOW_ALL, HIDE_NORMAL }
	private ListItemPolicy listItemPolicy;

	public Adapter_Alerts(Context context, List<MuninServer> items,
	                      ListItemSize listItemSize, ListItemPolicy listItemPolicy) {
		super(context, R.layout.alerts_part, items);
		this.context = context;
		this.listItemSize = listItemSize;
		this.listItemPolicy = listItemPolicy;
		this.parts = new ArrayList<>();

		for (MuninServer server : items)
			this.parts.add(new AlertPart(server, this));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		AlertPart alertPart = parts.get(position);

		View view;
		if (convertView != null)
			view = convertView;
		else
			view = LayoutInflater.from(context).inflate(R.layout.alerts_part, parent, false);

		return alertPart.inflate(context, view);
	}

	public void updateViews() {
		for (AlertPart alertPart : parts)
			alertPart.updateView();
	}

	public void updateViewsPartial() {
		for (AlertPart alertPart : parts)
			alertPart.updateViewPartial();
	}

	public void setAllGray() {
		for (AlertPart part : parts)
			part.setGray();
	}

	public void setListItemSize(ListItemSize val) {
		this.listItemSize = val;
		for (AlertPart part : parts)
			part.onListItemSizeChange();
	}
	public ListItemSize getListItemSize() { return this.listItemSize; }

	public void setListItemPolicy(ListItemPolicy val) { this.listItemPolicy = val; }
	public ListItemPolicy getListItemPolicy() { return this.listItemPolicy; }
	public boolean isEverythingOk() {
		for (AlertPart alertPart : parts) {
			if (!alertPart.isEverythingOk())
				return false;
		}
		return true;
	}

	public class AlertPart {
		private Adapter_Alerts adapter;
		private MuninServer server;
		private LinearLayout part;
		private TextView serverName;
		private LinearLayout criticals;
		private TextView criticalsAmount;
		private TextView criticalsLabel;
		private TextView criticalsPluginsList;
		private LinearLayout warnings;
		private TextView warningsAmount;
		private TextView warningsLabel;
		private TextView warningsPluginsList;
		private boolean everythingsOk;

		public AlertPart(MuninServer server, Adapter_Alerts adapter) {
			this.server = server;
			this.adapter = adapter;
			this.everythingsOk = true;
		}

		public View inflate(final Context context, View v) {
			MuninFoo.log("Inflating server " + server.getName());
			part 					= (LinearLayout) v.findViewById(R.id.alerts_part);
			serverName 				= (TextView) v.findViewById(R.id.alerts_part_serverName);
			criticals 				= (LinearLayout) v.findViewById(R.id.alerts_part_criticals);
			criticalsAmount 		= (TextView) v.findViewById(R.id.alerts_part_criticalsNumber);
			criticalsLabel 			= (TextView) v.findViewById(R.id.alerts_part_criticalsLabel);
			criticalsPluginsList 	= (TextView) v.findViewById(R.id.alerts_part_criticalsPluginsList);
			warnings 				= (LinearLayout) v.findViewById(R.id.alerts_part_warnings);
			warningsAmount			= (TextView) v.findViewById(R.id.alerts_part_warningsNumber);
			warningsLabel 			= (TextView) v.findViewById(R.id.alerts_part_warningsLabel);
			warningsPluginsList 	= (TextView) v.findViewById(R.id.alerts_part_warningsPluginsList);

			part.setVisibility(View.GONE);
			serverName.setText(server.getName());
			serverName.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MuninFoo.getInstance().setCurrentServer(server);
					context.startActivity(new Intent(context, Activity_AlertsPluginSelection.class));
					Util.setTransition(context, Util.TransitionStyle.DEEPER);
				}
			});

			Util.Fonts.setFont(context, (ViewGroup) v, Util.Fonts.CustomFont.RobotoCondensed_Regular);

			return v;
		}

		public void updateView() {
			int nbErrors = server.getErroredPlugins().size();
			int nbWarnings = server.getWarnedPlugins().size();

			if (server.reachable == Util.SpecialBool.TRUE) {
				everythingsOk = nbErrors == 0 && nbWarnings == 0;

				if (nbErrors > 0) {
					criticals.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_CRITICAL));
					criticalsPluginsList.setText(Util.pluginsListAsString(server.getErroredPlugins()));
				}
				else
					criticals.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_OK));

				if (nbWarnings > 0) {
					warnings.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_WARNING));
					warningsPluginsList.setText(Util.pluginsListAsString(server.getWarnedPlugins()));
				}
				else
					warnings.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_OK));

				criticalsAmount.setText(String.valueOf(nbErrors));
				criticalsLabel.setText(context.getString(
						nbErrors == 1 ? R.string.text50_1 // critical
								: R.string.text50_2 // criticals
				));

				warningsAmount.setText(String.valueOf(nbWarnings));
				warningsLabel.setText(context.getString(
						nbWarnings == 1 ? R.string.text51_1 // warning
								: R.string.text51_2 // warnings
				));
			}
			else if (server.reachable == Util.SpecialBool.FALSE) {
				everythingsOk = true;
				criticalsPluginsList.setText("");
				warningsPluginsList.setText("");
				criticalsAmount.setText("?");
				warningsAmount.setText("?");
				criticalsLabel.setText(context.getString(R.string.text50_2));
				warningsLabel.setText(context.getString(R.string.text51_2));
				criticals.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
				warnings.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
			}

			updateViewPartial();

			/*
			if (nbErrors > 0 || nbWarnings > 0)
			shouldDisplayEverythingsOk = false;

			if (!hideNormalStateServers)
				shouldDisplayEverythingsOk = false;

			// Can't flat the list before the first loading is finished
			if (index == muninFoo.getServers().size()-1) {
				// menu_flatList can be null if onCreateMenu hasn't been called yet
				if (menu_flatList != null)
					menu_flatList.setVisible(true);

				if (shouldDisplayEverythingsOk)
					everythingsOk.setVisibility(View.VISIBLE);
				else
					everythingsOk.setVisibility(View.GONE);
			}
			 */
		}

		public void updateViewPartial() {
			boolean hide = false;
			boolean hideNormal = adapter.getListItemPolicy() == ListItemPolicy.HIDE_NORMAL;

			int nbErrors = server.getErroredPlugins().size();
			int nbWarnings = server.getWarnedPlugins().size();

			if (nbErrors == 0 && nbWarnings == 0) {
				if (hideNormal)
					hide = true;
				serverName.setClickable(false);
				switchArrow(false);
			} else {
				if (hideNormal)
					hide = false;
				serverName.setClickable(true);
				switchArrow(true);
			}

			part.setVisibility(hide ? View.GONE : View.VISIBLE);
		}

		public void setGray() {
			criticals.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_UNDEFINED));
		}

		public void switchArrow(boolean value) {
			if (value)
				serverName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.arrow, 0);
			else
				serverName.setCompoundDrawables(null, null, null, null);
		}

		public void onListItemSizeChange() {
			if (adapter.getListItemSize() == ListItemSize.REDUCED) {
				// Expand
				criticals.setVisibility(View.VISIBLE);
				warnings.setVisibility(View.VISIBLE);
				serverName.setTextColor(Color.parseColor(Activity_Alerts.TEXT_COLOR));
				serverName.setBackgroundColor(Color.WHITE);
			} else {
				criticals.setVisibility(View.GONE);
				warnings.setVisibility(View.GONE);

				String s_criticalsAmount = criticalsAmount.getText().toString();
				int i_criticalsAmount = s_criticalsAmount.equals("?") ? -1 : Integer.parseInt(s_criticalsAmount);
				String s_warningsNumber = warningsAmount.getText().toString();
				int i_warningsAmount = s_warningsNumber.equals("?") ? -1 : Integer.parseInt(s_warningsNumber);

				if (i_criticalsAmount > 0 || i_warningsAmount > 0) {
					if (i_criticalsAmount > 0)
						serverName.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_CRITICAL));
					else if (i_warningsAmount > 0)
						serverName.setBackgroundColor(Color.parseColor(Activity_Alerts.BG_COLOR_WARNING));

					serverName.setTextColor(Color.WHITE);
				}
			}
		}

		public boolean isEverythingOk() { return this.everythingsOk; }
	}
}
