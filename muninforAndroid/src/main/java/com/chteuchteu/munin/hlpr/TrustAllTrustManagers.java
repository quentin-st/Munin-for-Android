package com.chteuchteu.munin.hlpr;

public class TrustAllTrustManagers implements javax.net.ssl.X509TrustManager {

    public java.security.cert.X509Certificate[] getAcceptedIssuers() {

        return new java.security.cert.X509Certificate[] {};

    }

    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)  {

    }

    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        for (int index=0; certs!=null && index<certs.length; index++) {

            //System.out.println(“Certificate ["+index+"]: ” + certs[0].getSubjectDN().getName());

        }
    }
}
