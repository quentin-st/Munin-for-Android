package com.chteuchteu.munin.obj;

/**
 * GridItem highly depends on its MuninPlugin attribute.
 * When it is deleted, we ask the user if he wants to delete the attached
 *  gridItems. If no, we keep the GridItem but consider it as detached.
 */
public class GridItem_Detached {
    private long id;
    private GridItem gridItem;

    // Keep some information about the plugin/node/master
    private String pluginName;
    private String pluginPageUrl;
    private String nodeUrl;
    private String masterUrl;

    public GridItem_Detached() {}

    public GridItem_Detached(GridItem gridItem) {
        this.gridItem = gridItem;
        this.pluginName = gridItem.getPlugin().getName();
        this.pluginPageUrl = gridItem.getPlugin().getPluginPageUrl();
        this.nodeUrl = gridItem.getPlugin().getInstalledOn().getUrl();
        this.masterUrl = gridItem.getPlugin().getInstalledOn().getParent().getUrl();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public GridItem getGridItem() { return gridItem; }
    public void setGridItem(GridItem val) { this.gridItem = val; }
    public String getPluginName() { return pluginName; }
    public void setPluginName(String val) { this.pluginName = val; }
    public String getPluginPageUrl() { return pluginPageUrl; }
    public void setPluginPageUrl(String val) { this.pluginPageUrl = val; }
    public String getNodeUrl() { return nodeUrl; }
    public void setNodeUrl(String val) { this.nodeUrl = val; }
    public String getMasterUrl() { return masterUrl; }
    public void setMasterUrl(String val) { this.masterUrl = val; }
}
