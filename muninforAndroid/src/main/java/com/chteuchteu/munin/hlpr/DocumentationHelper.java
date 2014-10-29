package com.chteuchteu.munin.hlpr;


import android.content.Context;

import com.chteuchteu.munin.obj.MuninPlugin;

public class DocumentationHelper {
	private static final String HTML_DOCS_SUBDIR = "html_docs/";

	public static String getDocumentation(Context context, MuninPlugin plugin) {
		String fileName = HTML_DOCS_SUBDIR + plugin.getName() + ".html";
		return Util.readFromAssets(context, fileName);
	}

	public static boolean hasDocumentation(Context context, MuninPlugin plugin) {
		String pluginName = plugin.getName();

		for (String str : documentedPlugins) {
			if (str.equals(pluginName))
				return true;
		}

		return false;
	}

	private static final String[] documentedPlugins = {
			"amavis", "apache", "asterisk", "bind9", "bind9_rndc", "ejabberd_", "haproxy_",
			"ifx_concurrent_sessions_", "courier_", "courier_mta_mailqueue", "courier_mta_mailstats", "courier_mta_mailvolume", "exim_mailqueue_alt",
			"exim_mailqueue", "exim_mailstats", "postfix_mailqueue", "postfix_mailstats", "postfix_mailvolume", "qmailscan",
			"sendmail_mailqueue", "sendmail_mailstats", "sendmail_mailtraffic", "gp_gbl_mem_util", "munin_update", "mysql_bytes",
			"mysql_", "mysql_innodb", "mysql_isam_space_", "mysql_queries", "mysql_slowqueries", "mysql_threads",
			"nginx_request", "nginx_status", "nomadix_users_", "slapd_bdb_cache_", "postgres_streaming_", "cupsys_pages",
			"lpstat", "snmp__print_pages", "snmp__print_supplies", "snmp__eltek_rectifier", "samba", "apt_all",
			"apt", "yum", "snort_alerts", "snort_bytes_pkt", "snort_drop_rate", "snort_pattern_match",
			"snort_pkts", "snort_traffic", "squeezebox_", "sybase_space", "ntp_", "ntp_kernel_err",
			"ntp_kernel_pll_freq", "ntp_kernel_pll_off", "ntp_offset", "ntp_states", "snmp__apc_ups", "varnish_",
			"vserver_cpu_", "vserver_loadavg", "vserver_resources", "zimbra_", "2wire", "accounting_",
			"asterisk_14_fax_ffa/asterisk_fax_cancelled", "asterisk_14_fax_ffa/asterisk_fax_current_sessions", "asterisk_14_fax_ffa/asterisk_fax_failed_completed", "asterisk_14_fax_ffa/asterisk_fax_iofail", "asterisk_14_fax_ffa/asterisk_fax_iopartial", "asterisk_14_fax_ffa/asterisk_fax_licensed_channels",
			"asterisk_14_fax_ffa/asterisk_fax_max_concurrent", "asterisk_14_fax_ffa/asterisk_fax_negotiations_failed", "asterisk_14_fax_ffa/asterisk_fax_nofax", "asterisk_14_fax_ffa/asterisk_fax_partial", "asterisk_14_fax_ffa/asterisk_fax_protocol_error", "asterisk_14_fax_ffa/asterisk_fax_success",
			"asterisk_14_fax_ffa/asterisk_fax_switched2t38", "asterisk_14_fax_ffa/asterisk_fax_train_failure", "asterisk_14_fax_ffa/asterisk_fax_txrx_attempts", "asterisk_16_fax_ffa/asterisk_fax_cancelled", "asterisk_16_fax_ffa/asterisk_fax_channels", "asterisk_16_fax_ffa/asterisk_fax_current_sessions",
			"asterisk_16_fax_ffa/asterisk_fax_failed_completed", "asterisk_16_fax_ffa/asterisk_fax_iofail", "asterisk_16_fax_ffa/asterisk_fax_iopartial", "asterisk_16_fax_ffa/asterisk_fax_licensed_channels", "asterisk_16_fax_ffa/asterisk_fax_max_concurrent", "asterisk_16_fax_ffa/asterisk_fax_negotiations_failed",
			"asterisk_16_fax_ffa/asterisk_fax_nofax", "asterisk_16_fax_ffa/asterisk_fax_partial", "asterisk_16_fax_ffa/asterisk_fax_protocol_error", "asterisk_16_fax_ffa/asterisk_fax_success", "asterisk_16_fax_ffa/asterisk_fax_switched2t38", "asterisk_16_fax_ffa/asterisk_fax_train_failure",
			"asterisk_16_fax_ffa/asterisk_fax_txrx_attempts", "asterisk_18_fax_spandsp/asterisk_fax_call_dropped", "asterisk_18_fax_spandsp/asterisk_fax_current_sessions", "asterisk_18_fax_spandsp/asterisk_fax_failed_completed", "asterisk_18_fax_spandsp/asterisk_fax_file_error", "asterisk_18_fax_spandsp/asterisk_fax_memory_error",
			"asterisk_18_fax_spandsp/asterisk_fax_negotiations_failed", "asterisk_18_fax_spandsp/asterisk_fax_nofax", "asterisk_18_fax_spandsp/asterisk_fax_protocol_error", "asterisk_18_fax_spandsp/asterisk_fax_retries_exceeded", "asterisk_18_fax_spandsp/asterisk_fax_rxtx_protocol_error", "asterisk_18_fax_spandsp/asterisk_fax_success",
			"asterisk_18_fax_spandsp/asterisk_fax_switched2t38", "asterisk_18_fax_spandsp/asterisk_fax_train_failure", "asterisk_18_fax_spandsp/asterisk_fax_txrx_attempts", "asterisk_18_fax_spandsp/asterisk_fax_unknown_error", "bird", "boinc_credit",
			"boinc_estwk", "boinc_projs", "boinc_wus", "celery_tasks", "celery_tasks_states", "ceph_capacity",
			"ceph_osd", "cyrus-imapd", "dspam_", "dspam_activity", "femon", "geowebcache-bandwidth",
			"geowebcache-blankitems", "geowebcache-cache-hits-ratio", "glassfish_counters_", "amd_gpu_", "nvidia_gpu_", "haproxy_abort_backend",
			"haproxy_active_backend", "haproxy_bytes_backend", "haproxy_bytes_compressor_backend", "haproxy_bytes_compressor_frontend", "haproxy_bytes_frontend", "haproxy_denied_backend",
			"haproxy_denied_frontend", "haproxy_errors_backend", "haproxy_errors_frontend", "haproxy_queue_backend", "haproxy_rate_backend", "haproxy_rate_frontend",
			"haproxy_reqrate_frontend", "haproxy_response_compressor_backend", "haproxy_response_compressor_frontend", "haproxy_responses_backend", "haproxy_responses_frontend", "haproxy_sessions_backend",
			"haproxy_sessions_frontend", "haproxy_sessions_total_backend", "haproxy_sessions_total_frontend", "haproxy_warnings_backend", "hhvm_", "imapproxy_multi",
			"ipvs_active", "ipvs_bps", "ipvs_conn", "ipvs_cps", "jchkmail_counters_", "jenkins_",
			"joomla-sessions", "kamailio_memory", "kamailio_mysql_shared_memory", "kamailio_transactions_users", "relayd", "lustre_abs",
			"lustre_df", "lustre_df_free", "lustre_df_indodes", "lxc_cpu", "lxc_cpu_time", "lxc_net",
			"lxc_proc", "lxc_ram", "dovecot_stats_", "postfwd2", "sa-learn", "sendmail_mailq",
			"memcached_servers_", "mpdstats_", "munin_stats_plugins", "hs_read", "hs_write", "snmp__netapp_cifs2",
			"snmp__netapp_cifscalls", "snmp__netapp_cpu2", "snmp__netapp_diskbusy", "snmp__netapp_diskusage2_", "snmp__netapp_diskutil", "snmp__netapp_ndmp",
			"snmp__netapp_net", "snmp__netapp_nfs3calls", "snmp__netapp_ops", "snmp__netapp_reallocate", "snmp__netapp_sis", "snmp__netscaler_connections",
			"snmp__netscaler_cpu", "nn_", "openfire_", "openvpn_as_mtime", "openvpn_as_traffic", "openvpn_as_ttime",
			"openvpn_as_users", "openvpn_multi", "openvzcpu", "snmp__sentry", "pgbouncer_", "eaccelerator-usage",
			"php-fastcgi", "php_fpm_process", "xerox-wc3220", "xerox-wc7232-consumables", "multimemory", "rabbitmq_connections",
			"rt_ticket_loadtime", "shoutcast", "squeezebox_multi", "ssl_", "syslog_ng_stats", "ntp_kernel_pll_prec",
			"ntp_kernel_pll_tol", "ntp_peers", "ntp_peers_ipv6", "ntp_pool_score_", "ntp_queries", "vmware/fusion_",
			"xen-multi", "dar_vpnd", "zimbra-mailboxsizes"
	};
}
