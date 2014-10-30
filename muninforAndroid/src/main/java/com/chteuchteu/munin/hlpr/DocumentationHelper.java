package com.chteuchteu.munin.hlpr;


import android.content.Context;

import com.chteuchteu.munin.obj.MuninPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DocumentationHelper {
	private static final String HTML_DOCS_SUBDIR = "html_docs/";

	public static String getDocumentation(Context context, MuninPlugin plugin, String node) {
		String fileName = "";
		try {
			JSONObject obj = new JSONObject(jsonStruct);
			JSONArray index = obj.getJSONArray("index");
			for (int i=0; i<index.length(); i++) {
				JSONObject element = (JSONObject) index.get(i);

				if (node.equals("")) { // Don't care about node : take the first
					if (element.getString("name").equals(plugin.getName())) {
						fileName = element.getString("file");
						break;
					}
				} else {
					if (element.getString("name").equals(plugin.getName())) {
						String elementNode = element.getString("node");
						if (elementNode.equals("node")) { // "node" : only one node
							fileName = element.getString("file");
							break;
						} else {
							if (elementNode.equals(node)) {
								fileName = element.getString("file");
								break;
							}
						}
					}
				}
			}
		} catch (JSONException ex) { ex.printStackTrace(); return ""; }

		if (fileName.equals(""))
			return "";

		fileName = HTML_DOCS_SUBDIR + fileName;

		return Util.readFromAssets(context, fileName);
	}

	public static List<String> getNodes(MuninPlugin plugin) {
		List<String> list = new ArrayList<String>();

		try {
			JSONObject obj = new JSONObject(jsonStruct);
			JSONArray index = obj.getJSONArray("index");
			for (int i=0; i<index.length(); i++) {
				JSONObject element = (JSONObject) index.get(i);

				if (element.getString("name").equals(plugin.getName())) {
					String nodeName = element.getString("node");
					if (nodeName.equals("node"))
						list.add(0, "");
					else
						list.add(nodeName);
				}
			}
		} catch (JSONException ex) { ex.printStackTrace(); return null; }

		return list;
	}

	public static boolean hasDocumentation(MuninPlugin plugin) {
		String pluginName = plugin.getName();

		try {
			JSONObject obj = new JSONObject(jsonStruct);
			JSONArray index = obj.getJSONArray("index");
			for (int i=0; i<index.length(); i++) {
				if (((JSONObject) index.get(i)).getString("name").equals(pluginName))
					return true;
			}
		} catch (JSONException ex) { ex.printStackTrace(); return false; }

		return false;
	}

	/**
	 * Json index of the assets/html_docs/ files
	 * { name, category, node, file }
	 */
	private static final String jsonStruct = "{\"index\":[{\"name\":\"amavis\",\"category\":\"antivirus\",\"node\":\"node\",\"file\":\"core_antivirus_node_amavis.html\"},{\"name\":\"apache\",\"category\":\"apache\",\"node\":\"node\",\"file\":\"core_apache_node_apache.html\"},{\"name\":\"asterisk\",\"category\":\"asterisk\",\"node\":\"node\",\"file\":\"core_asterisk_node_asterisk.html\"},{\"name\":\"bind9\",\"category\":\"bind\",\"node\":\"node\",\"file\":\"core_bind_node_bind9.html\"},{\"name\":\"bind9_rndc\",\"category\":\"bind\",\"node\":\"node\",\"file\":\"core_bind_node_bind9_rndc.html\"},{\"name\":\"df\",\"category\":\"disk\",\"node\":\"aix\",\"file\":\"core_disk_aix_df.html\"},{\"name\":\"iostat.hd_only\",\"category\":\"disk\",\"node\":\"aix\",\"file\":\"core_disk_aix_iostat.hd_only.html\"},{\"name\":\"iostat\",\"category\":\"disk\",\"node\":\"aix\",\"file\":\"core_disk_aix_iostat.html\"},{\"name\":\"iostat.vp_only\",\"category\":\"disk\",\"node\":\"aix\",\"file\":\"core_disk_aix_iostat.vp_only.html\"},{\"name\":\"df\",\"category\":\"disk\",\"node\":\"cygwin\",\"file\":\"core_disk_cygwin_df.html\"},{\"name\":\"df\",\"category\":\"disk\",\"node\":\"node\",\"file\":\"core_disk_node_df.html\"},{\"name\":\"df_inode\",\"category\":\"disk\",\"node\":\"node\",\"file\":\"core_disk_node_df_inode.html\"},{\"name\":\"df\",\"category\":\"disk\",\"node\":\"hp-ux\",\"file\":\"core_disk_hp-ux_df.html\"},{\"name\":\"df_inode\",\"category\":\"disk\",\"node\":\"hp-ux\",\"file\":\"core_disk_hp-ux_df_inode.html\"},{\"name\":\"df_abs\",\"category\":\"disk\",\"node\":\"linux\",\"file\":\"core_disk_linux_df_abs.html\"},{\"name\":\"df\",\"category\":\"disk\",\"node\":\"linux\",\"file\":\"core_disk_linux_df.html\"},{\"name\":\"diskstat_\",\"category\":\"disk\",\"node\":\"linux\",\"file\":\"core_disk_linux_diskstat_.html\"},{\"name\":\"diskstats\",\"category\":\"disk\",\"node\":\"linux\",\"file\":\"core_disk_linux_diskstats.html\"},{\"name\":\"iostat\",\"category\":\"disk\",\"node\":\"linux\",\"file\":\"core_disk_linux_iostat.html\"},{\"name\":\"iostat_ios\",\"category\":\"disk\",\"node\":\"linux\",\"file\":\"core_disk_linux_iostat_ios.html\"},{\"name\":\"quota_usage_\",\"category\":\"disk\",\"node\":\"linux\",\"file\":\"core_disk_linux_quota_usage_.html\"},{\"name\":\"smart_\",\"category\":\"disk\",\"node\":\"node\",\"file\":\"core_disk_node_smart_.html\"},{\"name\":\"snmp__df\",\"category\":\"disk\",\"node\":\"node\",\"file\":\"core_disk_node_snmp__df.html\"},{\"name\":\"snmp__netapp_diskusage_\",\"category\":\"disk\",\"node\":\"node\",\"file\":\"core_disk_node_snmp__netapp_diskusage_.html\"},{\"name\":\"snmp__netapp_inodeusage_\",\"category\":\"disk\",\"node\":\"node\",\"file\":\"core_disk_node_snmp__netapp_inodeusage_.html\"},{\"name\":\"df\",\"category\":\"disk\",\"node\":\"sunos\",\"file\":\"core_disk_sunos_df.html\"},{\"name\":\"df_inode\",\"category\":\"disk\",\"node\":\"sunos\",\"file\":\"core_disk_sunos_df_inode.html\"},{\"name\":\"ejabberd_\",\"category\":\"ejabberd\",\"node\":\"node\",\"file\":\"core_ejabberd_node_ejabberd_.html\"},{\"name\":\"haproxy_\",\"category\":\"haproxy\",\"node\":\"node\",\"file\":\"core_haproxy_node_haproxy_.html\"},{\"name\":\"ifx_concurrent_sessions_\",\"category\":\"informix\",\"node\":\"node\",\"file\":\"core_informix_node_ifx_concurrent_sessions_.html\"},{\"name\":\"courier_\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_courier_.html\"},{\"name\":\"courier_mta_mailqueue\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_courier_mta_mailqueue.html\"},{\"name\":\"courier_mta_mailstats\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_courier_mta_mailstats.html\"},{\"name\":\"courier_mta_mailvolume\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_courier_mta_mailvolume.html\"},{\"name\":\"exim_mailqueue_alt\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_exim_mailqueue_alt.html\"},{\"name\":\"exim_mailqueue\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_exim_mailqueue.html\"},{\"name\":\"exim_mailstats\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_exim_mailstats.html\"},{\"name\":\"postfix_mailqueue\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_postfix_mailqueue.html\"},{\"name\":\"postfix_mailstats\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_postfix_mailstats.html\"},{\"name\":\"postfix_mailvolume\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_postfix_mailvolume.html\"},{\"name\":\"qmailscan\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_qmailscan.html\"},{\"name\":\"sendmail_mailqueue\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_sendmail_mailqueue.html\"},{\"name\":\"sendmail_mailstats\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_sendmail_mailstats.html\"},{\"name\":\"sendmail_mailtraffic\",\"category\":\"mail\",\"node\":\"node\",\"file\":\"core_mail_node_sendmail_mailtraffic.html\"},{\"name\":\"gp_gbl_mem_util\",\"category\":\"memory\",\"node\":\"hp-ux\",\"file\":\"core_memory_hp-ux_gp_gbl_mem_util.html\"},{\"name\":\"munin_update\",\"category\":\"munin\",\"node\":\"node\",\"file\":\"core_munin_node_munin_update.html\"},{\"name\":\"mysql_bytes\",\"category\":\"mysql\",\"node\":\"node\",\"file\":\"core_mysql_node_mysql_bytes.html\"},{\"name\":\"mysql_\",\"category\":\"mysql\",\"node\":\"node\",\"file\":\"core_mysql_node_mysql_.html\"},{\"name\":\"mysql_innodb\",\"category\":\"mysql\",\"node\":\"node\",\"file\":\"core_mysql_node_mysql_innodb.html\"},{\"name\":\"mysql_isam_space_\",\"category\":\"mysql\",\"node\":\"node\",\"file\":\"core_mysql_node_mysql_isam_space_.html\"},{\"name\":\"mysql_queries\",\"category\":\"mysql\",\"node\":\"node\",\"file\":\"core_mysql_node_mysql_queries.html\"},{\"name\":\"mysql_slowqueries\",\"category\":\"mysql\",\"node\":\"node\",\"file\":\"core_mysql_node_mysql_slowqueries.html\"},{\"name\":\"mysql_threads\",\"category\":\"mysql\",\"node\":\"node\",\"file\":\"core_mysql_node_mysql_threads.html\"},{\"name\":\"netstat\",\"category\":\"network\",\"node\":\"aix\",\"file\":\"core_network_aix_netstat.html\"},{\"name\":\"netstat\",\"category\":\"network\",\"node\":\"cygwin\",\"file\":\"core_network_cygwin_netstat.html\"},{\"name\":\"if_err_\",\"category\":\"network\",\"node\":\"darwin\",\"file\":\"core_network_darwin_if_err_.html\"},{\"name\":\"if_\",\"category\":\"network\",\"node\":\"darwin\",\"file\":\"core_network_darwin_if_.html\"},{\"name\":\"fail2ban\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_fail2ban.html\"},{\"name\":\"netstat\",\"category\":\"network\",\"node\":\"freebsd\",\"file\":\"core_network_freebsd_netstat.html\"},{\"name\":\"http_loadtime\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_http_loadtime.html\"},{\"name\":\"bonding_err_\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_bonding_err_.html\"},{\"name\":\"cps_\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_cps_.html\"},{\"name\":\"fw_conntrack\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_fw_conntrack.html\"},{\"name\":\"fw_forwarded_local\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_fw_forwarded_local.html\"},{\"name\":\"fw_packets\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_fw_packets.html\"},{\"name\":\"if_err_\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_if_err_.html\"},{\"name\":\"if_\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_if_.html\"},{\"name\":\"ip_\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_ip_.html\"},{\"name\":\"netstat\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_netstat.html\"},{\"name\":\"netstat_multi\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_netstat_multi.html\"},{\"name\":\"port_\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_port_.html\"},{\"name\":\"tcp\",\"category\":\"network\",\"node\":\"linux\",\"file\":\"core_network_linux_tcp.html\"},{\"name\":\"multiping\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_multiping.html\"},{\"name\":\"netstat\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_netstat.html\"},{\"name\":\"netstat\",\"category\":\"network\",\"node\":\"openbsd\",\"file\":\"core_network_openbsd_netstat.html\"},{\"name\":\"openvpn\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_openvpn.html\"},{\"name\":\"ping_\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_ping_.html\"},{\"name\":\"snmp__fc_if_\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_snmp__fc_if_.html\"},{\"name\":\"snmp__if_err_\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_snmp__if_err_.html\"},{\"name\":\"snmp__if_\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_snmp__if_.html\"},{\"name\":\"snmp__if_multi\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_snmp__if_multi.html\"},{\"name\":\"snmp__netstat\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_snmp__netstat.html\"},{\"name\":\"squid\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_squid.html\"},{\"name\":\"if_err_\",\"category\":\"network\",\"node\":\"sunos\",\"file\":\"core_network_sunos_if_err_.html\"},{\"name\":\"if_\",\"category\":\"network\",\"node\":\"sunos\",\"file\":\"core_network_sunos_if_.html\"},{\"name\":\"netstat\",\"category\":\"network\",\"node\":\"sunos\",\"file\":\"core_network_sunos_netstat.html\"},{\"name\":\"surfboard\",\"category\":\"network\",\"node\":\"node\",\"file\":\"core_network_node_surfboard.html\"},{\"name\":\"nfs4_client\",\"category\":\"nfs\",\"node\":\"linux\",\"file\":\"core_nfs_linux_nfs4_client.html\"},{\"name\":\"nfs_client\",\"category\":\"nfs\",\"node\":\"linux\",\"file\":\"core_nfs_linux_nfs_client.html\"},{\"name\":\"nfsd4\",\"category\":\"nfs\",\"node\":\"linux\",\"file\":\"core_nfs_linux_nfsd4.html\"},{\"name\":\"nfsd\",\"category\":\"nfs\",\"node\":\"linux\",\"file\":\"core_nfs_linux_nfsd.html\"},{\"name\":\"nfs_client\",\"category\":\"nfs\",\"node\":\"netbsd\",\"file\":\"core_nfs_netbsd_nfs_client.html\"},{\"name\":\"nginx_request\",\"category\":\"nginx\",\"node\":\"node\",\"file\":\"core_nginx_node_nginx_request.html\"},{\"name\":\"nginx_status\",\"category\":\"nginx\",\"node\":\"node\",\"file\":\"core_nginx_node_nginx_status.html\"},{\"name\":\"nomadix_users_\",\"category\":\"nomadix\",\"node\":\"node\",\"file\":\"core_nomadix_node_nomadix_users_.html\"},{\"name\":\"slapd_bdb_cache_\",\"category\":\"openldap\",\"node\":\"node\",\"file\":\"core_openldap_node_slapd_bdb_cache_.html\"},{\"name\":\"apc_envunit_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_apc_envunit_.html\"},{\"name\":\"apc_nis\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_apc_nis.html\"},{\"name\":\"dhcpd3\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_dhcpd3.html\"},{\"name\":\"external_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_external_.html\"},{\"name\":\"foldingathome_wu\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_foldingathome_wu.html\"},{\"name\":\"freeradius_acct\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_freeradius_acct.html\"},{\"name\":\"freeradius_auth\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_freeradius_auth.html\"},{\"name\":\"freeradius_proxy_acct\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_freeradius_proxy_acct.html\"},{\"name\":\"freeradius_proxy_auth\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_freeradius_proxy_auth.html\"},{\"name\":\"ipac-ng\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_ipac-ng.html\"},{\"name\":\"ircu\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_ircu.html\"},{\"name\":\"jmx_\",\"category\":\"other\",\"node\":\"java\",\"file\":\"core_other_java_jmx_.html\"},{\"name\":\"hwmon\",\"category\":\"other\",\"node\":\"linux\",\"file\":\"core_other_linux_hwmon.html\"},{\"name\":\"meminfo\",\"category\":\"other\",\"node\":\"linux\",\"file\":\"core_other_linux_meminfo.html\"},{\"name\":\"loggrep\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_loggrep.html\"},{\"name\":\"mailman\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_mailman.html\"},{\"name\":\"mailscanner\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_mailscanner.html\"},{\"name\":\"named\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_named.html\"},{\"name\":\"nut_misc\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_nut_misc.html\"},{\"name\":\"perdition\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_perdition.html\"},{\"name\":\"pgbouncer_connections\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_pgbouncer_connections.html\"},{\"name\":\"pgbouncer_requests\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_pgbouncer_requests.html\"},{\"name\":\"pm3users_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_pm3users_.html\"},{\"name\":\"postgres_autovacuum\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_autovacuum.html\"},{\"name\":\"postgres_bgwriter\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_bgwriter.html\"},{\"name\":\"postgres_cache_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_cache_.html\"},{\"name\":\"postgres_checkpoints\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_checkpoints.html\"},{\"name\":\"postgres_connections_db\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_connections_db.html\"},{\"name\":\"postgres_connections_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_connections_.html\"},{\"name\":\"postgres_locks_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_locks_.html\"},{\"name\":\"postgres_oldest_prepared_xact_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_oldest_prepared_xact_.html\"},{\"name\":\"postgres_prepared_xacts_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_prepared_xacts_.html\"},{\"name\":\"postgres_querylength_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_querylength_.html\"},{\"name\":\"postgres_scans_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_scans_.html\"},{\"name\":\"postgres_size_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_size_.html\"},{\"name\":\"postgres_transactions_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_transactions_.html\"},{\"name\":\"postgres_tuples_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_tuples_.html\"},{\"name\":\"postgres_users\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_users.html\"},{\"name\":\"postgres_xlog\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_postgres_xlog.html\"},{\"name\":\"proxy_plugin\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_proxy_plugin.html\"},{\"name\":\"slony_lag_\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_slony_lag_.html\"},{\"name\":\"spamstats\",\"category\":\"other\",\"node\":\"node\",\"file\":\"core_other_node_spamstats.html\"},{\"name\":\"iostat\",\"category\":\"other\",\"node\":\"sunos\",\"file\":\"core_other_sunos_iostat.html\"},{\"name\":\"postgres_streaming_\",\"category\":\"postgresql\",\"node\":\"node\",\"file\":\"core_postgresql_node_postgres_streaming_.html\"},{\"name\":\"cupsys_pages\",\"category\":\"printing\",\"node\":\"node\",\"file\":\"core_printing_node_cupsys_pages.html\"},{\"name\":\"lpstat\",\"category\":\"printing\",\"node\":\"node\",\"file\":\"core_printing_node_lpstat.html\"},{\"name\":\"snmp__print_pages\",\"category\":\"printing\",\"node\":\"node\",\"file\":\"core_printing_node_snmp__print_pages.html\"},{\"name\":\"snmp__print_supplies\",\"category\":\"printing\",\"node\":\"node\",\"file\":\"core_printing_node_snmp__print_supplies.html\"},{\"name\":\"processes\",\"category\":\"processes\",\"node\":\"aix\",\"file\":\"core_processes_aix_processes.html\"},{\"name\":\"proc\",\"category\":\"processes\",\"node\":\"linux\",\"file\":\"core_processes_linux_proc.html\"},{\"name\":\"proc_pri\",\"category\":\"processes\",\"node\":\"linux\",\"file\":\"core_processes_linux_proc_pri.html\"},{\"name\":\"threads\",\"category\":\"processes\",\"node\":\"linux\",\"file\":\"core_processes_linux_threads.html\"},{\"name\":\"multips\",\"category\":\"processes\",\"node\":\"node\",\"file\":\"core_processes_node_multips.html\"},{\"name\":\"multips_memory\",\"category\":\"processes\",\"node\":\"node\",\"file\":\"core_processes_node_multips_memory.html\"},{\"name\":\"processes\",\"category\":\"processes\",\"node\":\"node\",\"file\":\"core_processes_node_processes.html\"},{\"name\":\"ps_\",\"category\":\"processes\",\"node\":\"node\",\"file\":\"core_processes_node_ps_.html\"},{\"name\":\"psu_\",\"category\":\"processes\",\"node\":\"node\",\"file\":\"core_processes_node_psu_.html\"},{\"name\":\"vmstat\",\"category\":\"processes\",\"node\":\"node\",\"file\":\"core_processes_node_vmstat.html\"},{\"name\":\"snmp__eltek_rectifier\",\"category\":\"rectifier\",\"node\":\"node\",\"file\":\"core_rectifier_node_snmp__eltek_rectifier.html\"},{\"name\":\"samba\",\"category\":\"samba\",\"node\":\"node\",\"file\":\"core_samba_node_samba.html\"},{\"name\":\"apt_all\",\"category\":\"security\",\"node\":\"linux\",\"file\":\"core_security_linux_apt_all.html\"},{\"name\":\"apt\",\"category\":\"security\",\"node\":\"linux\",\"file\":\"core_security_linux_apt.html\"},{\"name\":\"yum\",\"category\":\"security\",\"node\":\"linux\",\"file\":\"core_security_linux_yum.html\"},{\"name\":\"cmc_tc_sensor_\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_cmc_tc_sensor_.html\"},{\"name\":\"digitemp_\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_digitemp_.html\"},{\"name\":\"hddtemp_smartctl\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_hddtemp_smartctl.html\"},{\"name\":\"ipmi_\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_ipmi_.html\"},{\"name\":\"ipmi_sensor_\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_ipmi_sensor_.html\"},{\"name\":\"nvidia_\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_nvidia_.html\"},{\"name\":\"snmp__sensors_fsc_bx_fan\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_snmp__sensors_fsc_bx_fan.html\"},{\"name\":\"snmp__sensors_fsc_bx_temp\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_snmp__sensors_fsc_bx_temp.html\"},{\"name\":\"snmp__sensors_mbm_fan\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_snmp__sensors_mbm_fan.html\"},{\"name\":\"snmp__sensors_mbm_temp\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_snmp__sensors_mbm_temp.html\"},{\"name\":\"snmp__sensors_mbm_volt\",\"category\":\"sensors\",\"node\":\"node\",\"file\":\"core_sensors_node_snmp__sensors_mbm_volt.html\"},{\"name\":\"temperature\",\"category\":\"sensors\",\"node\":\"sunos\",\"file\":\"core_sensors_sunos_temperature.html\"},{\"name\":\"snort_alerts\",\"category\":\"snort\",\"node\":\"node\",\"file\":\"core_snort_node_snort_alerts.html\"},{\"name\":\"snort_bytes_pkt\",\"category\":\"snort\",\"node\":\"node\",\"file\":\"core_snort_node_snort_bytes_pkt.html\"},{\"name\":\"snort_drop_rate\",\"category\":\"snort\",\"node\":\"node\",\"file\":\"core_snort_node_snort_drop_rate.html\"},{\"name\":\"snort_pattern_match\",\"category\":\"snort\",\"node\":\"node\",\"file\":\"core_snort_node_snort_pattern_match.html\"},{\"name\":\"snort_pkts\",\"category\":\"snort\",\"node\":\"node\",\"file\":\"core_snort_node_snort_pkts.html\"},{\"name\":\"snort_traffic\",\"category\":\"snort\",\"node\":\"node\",\"file\":\"core_snort_node_snort_traffic.html\"},{\"name\":\"squeezebox_\",\"category\":\"squeezebox\",\"node\":\"node\",\"file\":\"core_squeezebox_node_squeezebox_.html\"},{\"name\":\"sybase_space\",\"category\":\"sybase\",\"node\":\"node\",\"file\":\"core_sybase_node_sybase_space.html\"},{\"name\":\"cpu\",\"category\":\"system\",\"node\":\"aix\",\"file\":\"core_system_aix_cpu.html\"},{\"name\":\"load\",\"category\":\"system\",\"node\":\"aix\",\"file\":\"core_system_aix_load.html\"},{\"name\":\"memory\",\"category\":\"system\",\"node\":\"aix\",\"file\":\"core_system_aix_memory.html\"},{\"name\":\"swap\",\"category\":\"system\",\"node\":\"aix\",\"file\":\"core_system_aix_swap.html\"},{\"name\":\"load\",\"category\":\"system\",\"node\":\"darwin\",\"file\":\"core_system_darwin_load.html\"},{\"name\":\"sar_cpu\",\"category\":\"system\",\"node\":\"hp-ux\",\"file\":\"core_system_hp-ux_sar_cpu.html\"},{\"name\":\"buddyinfo\",\"category\":\"system\",\"node\":\"linux\",\"file\":\"core_system_linux_buddyinfo.html\"},{\"name\":\"cpuspeed\",\"category\":\"system\",\"node\":\"linux\",\"file\":\"core_system_linux_cpuspeed.html\"},{\"name\":\"irqstats\",\"category\":\"system\",\"node\":\"linux\",\"file\":\"core_system_linux_irqstats.html\"},{\"name\":\"lpar_cpu\",\"category\":\"system\",\"node\":\"linux\",\"file\":\"core_system_linux_lpar_cpu.html\"},{\"name\":\"memory\",\"category\":\"system\",\"node\":\"linux\",\"file\":\"core_system_linux_memory.html\"},{\"name\":\"numa_\",\"category\":\"system\",\"node\":\"linux\",\"file\":\"core_system_linux_numa_.html\"},{\"name\":\"procfs\",\"category\":\"system\",\"node\":\"linux\",\"file\":\"core_system_linux_procfs.html\"},{\"name\":\"procsys\",\"category\":\"system\",\"node\":\"linux\",\"file\":\"core_system_linux_procsys.html\"},{\"name\":\"selinux_avcstat\",\"category\":\"system\",\"node\":\"linux\",\"file\":\"core_system_linux_selinux_avcstat.html\"},{\"name\":\"snmp__cpuload\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_snmp__cpuload.html\"},{\"name\":\"snmp__df_ram\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_snmp__df_ram.html\"},{\"name\":\"snmp__load\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_snmp__load.html\"},{\"name\":\"snmp__memory\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_snmp__memory.html\"},{\"name\":\"snmp__processes\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_snmp__processes.html\"},{\"name\":\"snmp__rdp_users\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_snmp__rdp_users.html\"},{\"name\":\"snmp__uptime\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_snmp__uptime.html\"},{\"name\":\"snmp__users\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_snmp__users.html\"},{\"name\":\"snmp__winmem\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_snmp__winmem.html\"},{\"name\":\"cpu\",\"category\":\"system\",\"node\":\"sunos\",\"file\":\"core_system_sunos_cpu.html\"},{\"name\":\"load\",\"category\":\"system\",\"node\":\"sunos\",\"file\":\"core_system_sunos_load.html\"},{\"name\":\"memory\",\"category\":\"system\",\"node\":\"sunos\",\"file\":\"core_system_sunos_memory.html\"},{\"name\":\"paging_in\",\"category\":\"system\",\"node\":\"sunos\",\"file\":\"core_system_sunos_paging_in.html\"},{\"name\":\"paging_out\",\"category\":\"system\",\"node\":\"sunos\",\"file\":\"core_system_sunos_paging_out.html\"},{\"name\":\"uptime\",\"category\":\"system\",\"node\":\"sunos\",\"file\":\"core_system_sunos_uptime.html\"},{\"name\":\"users\",\"category\":\"system\",\"node\":\"node\",\"file\":\"core_system_node_users.html\"},{\"name\":\"ntp_\",\"category\":\"time\",\"node\":\"node\",\"file\":\"core_time_node_ntp_.html\"},{\"name\":\"ntp_kernel_err\",\"category\":\"time\",\"node\":\"node\",\"file\":\"core_time_node_ntp_kernel_err.html\"},{\"name\":\"ntp_kernel_pll_freq\",\"category\":\"time\",\"node\":\"node\",\"file\":\"core_time_node_ntp_kernel_pll_freq.html\"},{\"name\":\"ntp_kernel_pll_off\",\"category\":\"time\",\"node\":\"node\",\"file\":\"core_time_node_ntp_kernel_pll_off.html\"},{\"name\":\"ntp_offset\",\"category\":\"time\",\"node\":\"node\",\"file\":\"core_time_node_ntp_offset.html\"},{\"name\":\"ntp_states\",\"category\":\"time\",\"node\":\"node\",\"file\":\"core_time_node_ntp_states.html\"},{\"name\":\"jmx_tomcat_dbpools\",\"category\":\"tomcat\",\"node\":\"java\",\"file\":\"core_tomcat_java_jmx_tomcat_dbpools.html\"},{\"name\":\"tomcat_access\",\"category\":\"tomcat\",\"node\":\"node\",\"file\":\"core_tomcat_node_tomcat_access.html\"},{\"name\":\"tomcat_\",\"category\":\"tomcat\",\"node\":\"node\",\"file\":\"core_tomcat_node_tomcat_.html\"},{\"name\":\"tomcat_jvm\",\"category\":\"tomcat\",\"node\":\"node\",\"file\":\"core_tomcat_node_tomcat_jvm.html\"},{\"name\":\"tomcat_threads\",\"category\":\"tomcat\",\"node\":\"node\",\"file\":\"core_tomcat_node_tomcat_threads.html\"},{\"name\":\"tomcat_volume\",\"category\":\"tomcat\",\"node\":\"node\",\"file\":\"core_tomcat_node_tomcat_volume.html\"},{\"name\":\"snmp__apc_ups\",\"category\":\"ups\",\"node\":\"node\",\"file\":\"core_ups_node_snmp__apc_ups.html\"},{\"name\":\"varnish_\",\"category\":\"varnish\",\"node\":\"node\",\"file\":\"core_varnish_node_varnish_.html\"},{\"name\":\"vserver_cpu_\",\"category\":\"vserver\",\"node\":\"linux\",\"file\":\"core_vserver_linux_vserver_cpu_.html\"},{\"name\":\"vserver_loadavg\",\"category\":\"vserver\",\"node\":\"linux\",\"file\":\"core_vserver_linux_vserver_loadavg.html\"},{\"name\":\"vserver_resources\",\"category\":\"vserver\",\"node\":\"linux\",\"file\":\"core_vserver_linux_vserver_resources.html\"},{\"name\":\"zimbra_\",\"category\":\"zimbra\",\"node\":\"node\",\"file\":\"core_zimbra_node_zimbra_.html\"}],\"currentCategory\":\"zimbra\",\"currentNode\":\"node\"}\n";
}
