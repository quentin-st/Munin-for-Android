package com.chteuchteu.munin;

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
	private String label;
	@Column(name = "Server") // fix : plugin.getServer() does not returns
	private MuninServer server; // valid server.
	
	public LabelRelation() { }
	
	public LabelRelation(MuninPlugin plugin, String label, MuninServer installedOn) {
		this.plugin = plugin;
		this.label = label;
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
	public void setLabelName(String l) {
		this.label = l;
	}
	public MuninPlugin getPlugin() {
		return this.plugin;
	}
	public String getLabelName() {
		return this.label;
	}
	public MuninServer getInstalledOn() {
		return this.server;
	}
	public void setInstalledOn(MuninServer s) {
		this.server = s;
	}
}