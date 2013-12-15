package com.chteuchteu.munin.obj;

/*
 * DEPRECATED
 */

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "MuninLabelRelations")
public class LabelRelation extends Model {
	private long bddId;
	@Column(name = "Plugin")
	private MuninPlugin plugin;
	@Column(name = "LabelName")
	private String labelName;
	@Column(name = "Server") // fix : plugin.getServer() does not returns
	private MuninServer server; // valid server // deprecated
	private Label label;
	
	public LabelRelation() { }
	
	public LabelRelation(MuninPlugin plugin, String labelName, MuninServer installedOn) {
		this.plugin = plugin;
		this.labelName = labelName;
		this.server = installedOn;
	}
	
	public void setBddId(long id) {
		this.bddId = id;
	}
	public long getBddId() {
		return this.bddId;
	}
	public void setPlugin(MuninPlugin p) {
		this.plugin = p;
	}
	public MuninPlugin getPlugin() {
		return this.plugin;
	}
	public void setLabel(Label l) {
		this.label = l;
	}
	public Label getLabel() {
		return this.label;
	}
	
	
	// Deprecated (ActiveAndroid)
	public String getLabelName() {
		return this.labelName;
	}
	public void setLabelName(String l) {
		this.labelName = l;
	}
	public void setInstalledOn(MuninServer s) {
		this.server = s;
	}
	public MuninServer getInstalledOn() {
		return this.server;
	}
}