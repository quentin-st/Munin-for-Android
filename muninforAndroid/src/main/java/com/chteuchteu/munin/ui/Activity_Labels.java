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
	private enum LayoutStyle { FULLSCREEN, SIDE_BY_SIDE }
	private LayoutStyle layoutStyle;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_labels);
		super.onContentViewSet();
		actionBar.setTitle(getString(R.string.button_labels));

		activityState = ActivityState.SELECTING_LABEL;
		layoutStyle = isDeviceLarge(Util.getDeviceSizeCategory(this)) ? LayoutStyle.SIDE_BY_SIDE : LayoutStyle.FULLSCREEN;

		if (layoutStyle == LayoutStyle.FULLSCREEN)
			findViewById(R.id.labelsItemsListfragment_container).setVisibility(View.GONE);

		if (savedInstanceState == null) {
			Bundle arguments = new Bundle();
			arguments.putBoolean(Fragment_LabelsList.SELECT_CURRENT_ITEM, layoutStyle == LayoutStyle.SIDE_BY_SIDE);
			labelsListFragment = new Fragment_LabelsList();
			labelsListFragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().add(R.id.labelsListfragment_container, labelsListFragment, "labelsList").commit();

			labelsItemsListFragment = new Fragment_LabelsItemsList();
			getSupportFragmentManager().beginTransaction().add(R.id.labelsItemsListfragment_container, labelsItemsListFragment, "labelsItemsList").commit();
		} else {
			labelsListFragment = (Fragment_LabelsList) getSupportFragmentManager().findFragmentByTag("labelsList");
			labelsItemsListFragment = (Fragment_LabelsItemsList) getSupportFragmentManager().findFragmentByTag("labelsItemsList");
		}
	}

	@Override
	public void onLabelsFragmentLoaded() {
		// On side-by-side layout, select current label when coming back from Activity_GraphView
		if (layoutStyle == LayoutStyle.SIDE_BY_SIDE
				&& getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey("labelId")) {
			long labelId = getIntent().getExtras().getLong("labelId");
			labelsListFragment.setSelectedItem(muninFoo.labels.indexOf(muninFoo.getLabel(labelId)));
		}
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

		if (layoutStyle == LayoutStyle.FULLSCREEN) {
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
		Util.setTransition(this, Util.TransitionStyle.DEEPER);
	}

	@Override
	public void unselectLabel() { labelsItemsListFragment.unselectLabel(); }
	
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
        if (drawerHelper.closeDrawerIfOpen())
            return;

		if (activityState == ActivityState.SELECTING_PLUGIN) {
			labelsItemsListFragment.unselectLabel();

			if (layoutStyle == LayoutStyle.FULLSCREEN) {
				findViewById(R.id.labelsItemsListfragment_container).setVisibility(View.GONE);
				findViewById(R.id.labelsListfragment_container).setVisibility(View.VISIBLE);
			}
			addLabel.setVisible(true);
			actionBar.setTitle(getString(R.string.button_labels));
		} else {
			Intent intent = new Intent(this, Activity_Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			Util.setTransition(this, TransitionStyle.SHALLOWER);
		}
	}

	private static boolean isDeviceLarge(Util.DeviceSizeCategory deviceSizeCategory) {
		return deviceSizeCategory == Util.DeviceSizeCategory.LARGE || deviceSizeCategory == Util.DeviceSizeCategory.XLARGE;
	}
}
