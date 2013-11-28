package com.chteuchteu.munin;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "MuninLabelRelations")
public class MuninLabelRelation extends Model {
	@Column(name = "Plugin")
	private MuninPlugin plugin;
	@Column(name = "LabelName")
	private String label;
	
	public MuninLabelRelation() {
		
	}
	
	public MuninLabelRelation(MuninPlugin plugin, String label) {
		this.plugin = plugin;
		this.label = label;
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
}