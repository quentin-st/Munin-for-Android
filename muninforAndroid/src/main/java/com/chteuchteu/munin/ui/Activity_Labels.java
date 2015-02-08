package com.chteuchteu.munin.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.DrawerHelper;
import com.chteuchteu.munin.hlpr.Util;
import com.chteuchteu.munin.hlpr.Util.TransitionStyle;
import com.chteuchteu.munin.obj.Label;


public class Activity_Labels extends MuninActivity implements ILabelsActivity {
	private Fragment_LabelsList labelsListFragment;
	private Fragment_LabelsItemsList labelsItemsListFragment;
	private MenuItem addLabel;

	private enum ActivityState { SELECTING_LABEL, SELECTING_PLUGIN }
	private ActivityState activityState;

	private Util.DeviceSizeCategory deviceSizeCategory;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_labels);
		super.onContentViewSet();
		dh.setDrawerActivity(this);
		actionBar.setTitle(getString(R.string.button_labels));

		activityState = ActivityState.SELECTING_LABEL;

		if (savedInstanceState == null) {
			labelsListFragment = new Fragment_LabelsList();
			getSupportFragmentManager().beginTransaction().add(R.id.labelsListfragment_container, labelsListFragment, "labelsList").commit();

			labelsItemsListFragment = new Fragment_LabelsItemsList();
			getSupportFragmentManager().beginTransaction().add(R.id.labelsItemsListfragment_container, labelsItemsListFragment, "labelsItemsList").commit();
		} else {
			labelsListFragment = (Fragment_LabelsList) getSupportFragmentManager().findFragmentByTag("labelsList");
			labelsItemsListFragment = (Fragment_LabelsItemsList) getSupportFragmentManager().findFragmentByTag("labelsItemsList");
		}

		deviceSizeCategory = Util.getDeviceSizeCategory(this);
		if (!isDeviceLarge())
			findViewById(R.id.labelsItemsListfragment_container).setVisibility(View.GONE);
	}

	@Override
	public void onLabelsFragmentLoaded() {
		if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey("labelId"))
			labelsListFragment.setSelectedItem(muninFoo.labels.indexOf(muninFoo.getLabel(getIntent().getExtras().getLong("labelId"))));
	}

	@Override
	public void onLabelsItemsListFragmentLoaded() {
		if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey("labelId"))
			onLabelClick(muninFoo.getLabel(getIntent().getExtras().getLong("labelId")));
	}

	@Override
	public DrawerHelper.DrawerMenuItem getDrawerMenuItem() { return DrawerHelper.DrawerMenuItem.Labels; }

	protected void createOptionsMenu() {
		super.createOptionsMenu();

		getMenuInflater().inflate(R.menu.labels, menu);
		this.addLabel = menu.findItem(R.id.menu_add);
		this.addLabel.setVisible(activityState == ActivityState.SELECTING_LABEL);
	}

	@Override
	public void onLabelClick(Label label) {
		activityState = ActivityState.SELECTING_PLUGIN;
		labelsItemsListFragment.setLabel(label);

		if (!isDeviceLarge()) {
			findViewById(R.id.labelsListfragment_container).setVisibility(View.GONE);
			findViewById(R.id.labelsItemsListfragment_container).setVisibility(View.VISIBLE);
			if (addLabel != null)
				addLabel.setVisible(false);
		}

		actionBar.setTitle(label.getName());
	}

	@Override
	public void onLabelItemClick(int pos, String labelName, long labelId) {
		Intent intent = new Intent(context, Activity_GraphView.class);
		intent.putExtra("position", pos);
		intent.putExtra("from", "labels");
		intent.putExtra("label", labelName);
		intent.putExtra("labelId", labelId);
		startActivity(intent);
		Util.setTransition(context, Util.TransitionStyle.DEEPER);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
			case R.id.menu_add:
				final LinearLayout ll = new LinearLayout(this);
				ll.setOrientation(LinearLayout.VERTICAL);
				ll.setPadding(10, 30, 10, 10);
				final EditText input = new EditText(this);
				ll.addView(input);
				
				new AlertDialog.Builder(activity)
				.setTitle(getText(R.string.text70_2))
				.setView(ll)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						if (!value.trim().equals(""))
							muninFoo.addLabel(new Label(value));
						dialog.dismiss();
						labelsListFragment.updateListView();
						labelsItemsListFragment.unselectLabel();
					}
				}).setNegativeButton(R.string.text64, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) { }
				}).show();
					
				return true;
		}

		return true;
	}
	
	@Override
	public void onBackPressed() {
		if (activityState == ActivityState.SELECTING_PLUGIN) {
			labelsItemsListFragment.unselectLabel();

			if (!isDeviceLarge()) {
				findViewById(R.id.labelsItemsListfragment_container).setVisibility(View.GONE);
				findViewById(R.id.labelsListfragment_container).setVisibility(View.VISIBLE);
			}
			addLabel.setVisible(true);
			actionBar.setTitle(getString(R.string.button_labels));
		} else {
			Intent intent = new Intent(this, Activity_Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(context, TransitionStyle.SHALLOWER);
		}
	}

	private boolean isDeviceLarge() {
		return deviceSizeCategory == Util.DeviceSizeCategory.LARGE || deviceSizeCategory == Util.DeviceSizeCategory.XLARGE;
	}
}
