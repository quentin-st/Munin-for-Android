package com.chteuchteu.munin.obj;

public abstract class Entity {
	protected long id;

	public Entity() {
		this.id = -1;
	}

	public void setId(long id) { this.id = id; }
	public long getId() { return this.id; }
}
