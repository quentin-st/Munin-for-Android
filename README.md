# Munin for Android #

Welcome to the public repository for [Munin for Android](https://play.google.com/store/apps/details?id=com.chteuchteu.munin) application. If you're willing to contribute, please read the following indications - please also update them if you make significant changes.

Here are the different packages of the muninForAndroid module:

## Objects *(obj)* ##
* MuninMaster
* MuninServer
* MuninPlugin
* Grid
* GridItem
* Label
* SearchResult
* AlertsWidget
* GraphWidget

## Activities *(ui)* ##
* Activity_About
* Activity_Alerts
* Activity_AlertsPluginSelection
* Activity_GoPremium
* Activity_GraphView
* Activity_Grid
* Activity_Grids
* Activity_Label
* Activity_Labels
* Activity_Main
* Activity_Notifications
* Activity_Plugins
* Activity_Server
* Activity_Servers
* Activity_ServersEdit
* Activity_Settings
* MuninActivity - _Every class extends MuninActivity to avoid code redundancy_

## Helpers & Utils classes *(hlpr)* ##
* BillingService
* DatabaseHelper *- SQLite database interface*
* DigestUtils *- Apache digest auth util class*
* DocumentationHelper
* DrawerHelper
* DynazoomHelper
* EncryptionHelper
* GridDownloadHelper *- Grids simultaneous downloads helper*
* ImportExportHelper
* JSONHelper *- JSON import/export - not used for now*
* MediaScannerUtil *- warns Android system about a new picture on the filesystem*
* NetHelper
* SQLite *- SQLite database top-level methods (saveServers(), ...)*
* Util *- Generic util methods*

## Adapters ##
* Adapter_ExpandableListView.java
* Adapter_GraphView.java
* Adapter_IconList.java
* Adapter_SeparatedList.java

## Widgets *(wdget)* ##
* Widget_AlertsWidget_Configure
* Widget_AlertsWidget_ViewsFactory
* Widget_AlertsWidget_WidgetProvider
* Widget_AlertsWidget_WidgetService
* Widget_GraphWidget_Configure
* Widget_GraphWidget_WidgetProvider

## Others ##
* BootReceiver
* CustomSSLFactory
* HackyDrawerLayout
* MuninFoo *- App Singleton*
* Service_Notifications
* HTTPResponse
* HTTPResponse_Bitmap

## Custom Exceptions ##
* ImportExportWebServiceException
* NullMuninFooException

## WebServices ##
### Import / Export ###
Import/export servers configuration  
Target : munin-for-android.com/ws/importExport.php  
Generic Import/Export class : hlpr / ImportExportHelper.java  
Read more : importExport.php
