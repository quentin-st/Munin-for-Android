package com.chteuchteu.munin.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.obj.MuninMaster;
import com.chteuchteu.munin.obj.MuninPlugin;
import com.chteuchteu.munin.obj.MuninServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-level list with checkboxes
 */
public class MultiLevelCheckboxList extends ScrollView {
    private Context context;
    private List<MuninMaster> masters;
	private List<HierarchyGroup> mastersHierarchyGroups;

    public MultiLevelCheckboxList(Context context) {
        super(context);
        this.context = context;
    }

    public void initList(List<MuninMaster> masters) {
        this.masters = masters;
        inflateLayout();
    }

    private void inflateLayout() {
	    LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LayoutInflater inflater = LayoutInflater.from(context);
        mastersHierarchyGroups = new ArrayList<>();

	    for (MuninMaster master : this.masters) {
		    HierarchyGroup group = new HierarchyGroup(null, master, inflater);
		    mastersHierarchyGroups.add(group);
		    linearLayout.addView(group.buildView(this));
	    }

	    this.addView(linearLayout);
    }

	public List<MuninPlugin> getCheckedPlugins() {
		List<MuninPlugin> list = new ArrayList<>();
		for (HierarchyGroup group : mastersHierarchyGroups)
			list.addAll(group.getChecked());

		return list;
	}

	private class HierarchyGroup {
		private LayoutInflater inflater;
		private HierarchyGroup parent;
		private List<HierarchyGroup> children;
		/**
		 * MuninMaster / Server / Plugin
		 */
		private Object object;
		private boolean reduced;

		private LinearLayout view;
		private View head;
		private CheckBox head_checkbox;
		private LinearLayout childrenView;


		public HierarchyGroup(HierarchyGroup parent, Object object, LayoutInflater inflater) {
			this.parent = parent;
			this.object = object;
			this.inflater = inflater;
			this.children = new ArrayList<>();
		}

		private CompoundButton.OnCheckedChangeListener checkChangeListener
				= new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (hasChildren()) {
					for (HierarchyGroup group : children)
						group.onParentCheckChanged(isChecked);
				}
				if (parent != null)
					parent.onChildrenCheckChanged();
			}
		};

		public View buildView(ViewGroup layoutParent) {
			this.view = new LinearLayout(context);
			this.view.setOrientation(LinearLayout.VERTICAL);

			// Head
			this.head = inflater.inflate(R.layout.expandable_cb_head, layoutParent, false);
			TextView groupName = (TextView) head.findViewById(R.id.groupName);
			final ImageView arrow = (ImageView) head.findViewById(R.id.icon);
			head_checkbox = (CheckBox) head.findViewById(R.id.checkbox);
			this.reduced = true;

			groupName.setText(getGroupName());
			if (!this.hasChildren())
				arrow.setVisibility(View.GONE);
			head_checkbox.setOnCheckedChangeListener(checkChangeListener);
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (hasChildren()) {
						if (reduced) {
							reduced = false;
							childrenView.setVisibility(View.VISIBLE);
							arrow.setImageResource(R.drawable.ic_action_down);
						} else {
							reduced = true;
							childrenView.setVisibility(View.GONE);
							arrow.setImageResource(R.drawable.ic_action_up);
						}
					} else {
						setChecked(!head_checkbox.isChecked(), true);
					}
				}
			});
			view.addView(head);

			// Children view
			if (this.hasChildren()) {
				this.childrenView = new LinearLayout(context);
				childrenView.setOrientation(LinearLayout.VERTICAL);
				childrenView.setVisibility(View.GONE);

				if (object instanceof MuninMaster) {
					for (MuninServer server : ((MuninMaster) object).getChildren()) {
						HierarchyGroup group = new HierarchyGroup(this, server, inflater);
						this.children.add(group);
						childrenView.addView(group.buildView(childrenView));
					}
				} else if (object instanceof MuninServer) {
					for (MuninPlugin plugin : ((MuninServer) object).getPlugins()) {
						HierarchyGroup group = new HierarchyGroup(this, plugin, inflater);
						this.children.add(group);
						childrenView.addView(group.buildView(childrenView));
					}
				}

				view.addView(childrenView);
			}


			return view;
		}

		public void setChecked(boolean checked, boolean triggerListener) {
			if (!triggerListener)
				head_checkbox.setOnCheckedChangeListener(null);
			head_checkbox.setChecked(checked);
			if (!triggerListener)
				head_checkbox.setOnCheckedChangeListener(checkChangeListener);
		}

		public void onParentCheckChanged(boolean isChecked) {
			setChecked(isChecked, true);
		}

		public void onChildrenCheckChanged() {
			if (!this.hasChildren())
				return;

			int nbChecked = 0;
			for (HierarchyGroup group : children) {
				if (group.head_checkbox.isChecked())
					nbChecked++;
			}
			setChecked(nbChecked == children.size(), false);
		}

		private boolean hasChildren() {
			return this.object instanceof MuninMaster || this.object instanceof MuninServer;
		}

		private String getGroupName() {
			if (this.object instanceof MuninMaster)
				return ((MuninMaster) object).getName();
			else if (this.object instanceof MuninServer)
				return ((MuninServer) object).getName();
			else if (this.object instanceof MuninPlugin)
				return ((MuninPlugin) object).getFancyName();
			return "";
		}

		public List<MuninPlugin> getChecked() {
			List<MuninPlugin> list = new ArrayList<>();

			if (object instanceof MuninPlugin) {
				if (head_checkbox.isChecked())
					list.add((MuninPlugin) object);
			} else {
				for (HierarchyGroup hierarchyGroup : children)
					list.addAll(hierarchyGroup.getChecked());
			}
			return list;
		}
	}
}
