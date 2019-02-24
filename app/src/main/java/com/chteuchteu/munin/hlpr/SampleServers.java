package com.chteuchteu.munin.hlpr;

abstract public class SampleServers {
    public static final String[] URLS = {
        "demo.munin-monitoring.org",
        "munin.ping.uio.no"
    };

    public static boolean isSample(String url) {
        for (String sampleServer : URLS) {
            if (url.equals(sampleServer)) {
                return true;
            }
        }

        return false;
    }
}
