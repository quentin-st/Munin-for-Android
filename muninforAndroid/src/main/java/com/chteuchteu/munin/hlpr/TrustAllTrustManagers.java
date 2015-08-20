package com.chteuchteu.munin.hlpr;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class TrustAllTrustManagers implements X509TrustManager {

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[] {};
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType) { }

    public void checkServerTrusted(X509Certificate[] certs, String authType) {
        for (int index=0; certs!=null && index<certs.length; index++) {
            //System.out.println(“Certificate ["+index+"]: ” + certs[0].getSubjectDN().getName());
        }
    }
}
