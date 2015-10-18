package com.chteuchteu.munin.adptr;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.chteuchteu.munin.MuninFoo;
import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.NotifIgnoreRule;
import com.chteuchteu.munin.ui.Activity_Notifications;
import com.chteuchteu.munin.ui.Fragment_Notifications_Push;

import java.util.List;

public class Adapter_NotifIgnoreRules extends ArrayAdapter<NotifIgnoreRule> {
	private Fragment_Notifications_Push fragment;
	private Context context;
	private List<NotifIgnoreRule> rules;

	public Adapter_NotifIgnoreRules(Fragment_Notifications_Push fragment, Context context, List<NotifIgnoreRule> rules) {
		super(context, R.layout.list_notifignorerule, rules);
		this.fragment = fragment;
		this.context = context;
		this.rules = rules;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView != null)
			view = convertView;
		else
			view = LayoutInflater.from(context).inflate(R.layout.list_notifignorerule, parent, false);

		final NotifIgnoreRule rule = rules.get(position);

		TextView tv_rule = (TextView) view.findViewById(R.id.rule);
		TextView tv_until = (TextView) view.findViewById(R.id.until);
		View delete = view.findViewById(R.id.delete);

		// Depending on what is null and what is not, let's build the rule sentence
		if (rule.getHost() == null && rule.getPlugin() == null)
			tv_rule.setText(String.format(context.getString(R.string.ignore_whole_group), rule.getGroup()));
		else if (rule.getPlugin() == null)
			tv_rule.setText(String.format(context.getString(R.string.ignore_whole_host), rule.getHost(), rule.getGroup()));
		else if (rule.getGroup() == null && rule.getHost() == null)
			tv_rule.setText(String.format(context.getString(R.string.ignore_plugin), rule.getPlugin()));
		else
			tv_rule.setText(String.format(context.getString(R.string.ignore_plugin_in), rule.getPlugin(), rule.getHost(), rule.getGroup()));

		if (rule.getUntil() == null)
			tv_until.setText(context.getString(R.string.forever));
		else {
			long millis = rule.getUntil().getTimeInMillis();
			int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME;
			tv_until.setText(DateUtils.formatDateTime(context, millis, flags));
		}

		delete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				rules.remove(rule);
				MuninFoo.getInstance(context).sqlite.dbHlpr.deleteNotifIgnoreRule(rule);
				notifyDataSetChanged();
				fragment.updateIgnoreRulesCount();
			}
		});

		return view;
	}
}
