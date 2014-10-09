# Munin for Android #

## Objects structure ##
* MuninMaster
* MuninServer
* MuninPlugin

* Grid
* GridItem
* Label
* SearchResult
* Widget

## Activities ##
* Activity_About
* Activity_AddServer
* Activity_Alerts
* Activity_AlertsPluginSelection
* Activity_GoPremium
* Activity_GraphView
* Activity_Grid
* Activity_GridSelection
* Activity_Init
* Activity_Labels
* Activity_LabelsPluginSelection
* Activity_Main
* Activity_Notifications
* Activity_PluginSelection
* Activity_Servers
* Activity_ServersEdit
* Activity_Settings

## Helpers & Utils classes ##
* DatabaseHelper_old *- kept here for migration reasons*
* DatabaseHelper *- SQLite database interface*
* DigestUtils *- Apache digest auth util class*
* DrawerHelper
* GridDownloadHelper *- Grids simultaneous downloads helper*
* JSONHelper *- JSON import/export - not used for now*
* MediaScannerUtil *- warns Android system about a new picture on the filesystem*
* SQLite *- SQLite database top-level methods (saveServers(), ...)*
* Util *- Generic util methods*

## Adapters ##
* Adapter_ExpandableListView.java
* Adapter_GraphView.java
* Adapter_IconList.java
* Adapter_SeparatedList.java

## Others ##
* BootReceiver
* CustomSSLFactory
* MuninFoo *- App Singleton*
* Service_Notifications
* Widget_Configure
* Widget_GraphWidget

## Custom Exceptions ##
* ImportExportWebServiceException

## WebServices ##
### Import / Export ###
Import/export servers configuration  
Target : munin-for-android.com/ws/importExport.php  
Generic Import/Export class : src / com / chteuchteu / munin / hlpr / ImportExportHelper.java  
Read more : importExport.php
