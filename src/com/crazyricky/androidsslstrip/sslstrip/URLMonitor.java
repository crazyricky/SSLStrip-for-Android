package com.crazyricky.androidsslstrip.sslstrip;

import java.util.HashMap;
import java.util.HashSet;

public class URLMonitor {

    private static URLMonitor sInstance;

    private HashSet<ClientUrl> mStrippedURLs = new HashSet<ClientUrl>();
    private HashMap<ClientUrl, Integer> mStrippedURLPorts = new HashMap<ClientUrl, Integer>();
    // private boolean mFaviconReplacement = false;
    private boolean mFaviconSpoofing = false;

    private URLMonitor() {
        // mFaviconReplacement = false;
    }

    public boolean isSecureLink(String client, String realUrl) {
        int fileIndex = realUrl.indexOf("?", 0);
        String url = realUrl;
        if (fileIndex > 0) {
            url = realUrl.substring(0, fileIndex);
        }
        boolean result = mStrippedURLs.contains(new ClientUrl(client, url));
        return result;
    }

    public int getSecurePort(String client, String url) {
        if (mStrippedURLs.contains(new ClientUrl(client, url))) {
            return mStrippedURLPorts.get(new ClientUrl(client, url));
        } else {
            return 443;
        }
    }

    public void addSecureLink(String client, String url) {
        int methodIndex = url.indexOf("//") + 2;
        String method = url.substring(0, methodIndex);

        int pathIndex = url.indexOf("/", methodIndex);
        if (pathIndex < 0) {
            pathIndex = url.length();
        }
        String host = url.substring(methodIndex, pathIndex);

        int fileIndex = url.indexOf("?", pathIndex);
        String path = url.substring(pathIndex, url.length());
        if (fileIndex > 0) {
            path = url.substring(pathIndex, fileIndex);
        }

        int port = 443;
        int portIndex = host.indexOf(":");

        if (portIndex != -1) {
            host = host.substring(0, portIndex);
            String portStr = host.substring(portIndex + 1, host.length());
            if (portStr.length() == 0) {
                port = 443;
            } else {
                port = Integer.valueOf(portStr);
            }
        }

        url = method + host + path;

        mStrippedURLs.add(new ClientUrl(client, url));
        mStrippedURLPorts
                .put(new ClientUrl(client, url), Integer.valueOf(port));
    }

    public void setFaviconSpoofing(boolean faviconSpoofing) {
        mFaviconSpoofing = faviconSpoofing;
    }

    public boolean isFaviconSpoofing() {
        return mFaviconSpoofing;
    }

    public boolean isSecureFavicon(String client, String url) {
        return ((mFaviconSpoofing) && (url.contains("favicon-x-favicon-x.ico")));
    }

    public static URLMonitor getInstance() {
        if (sInstance == null) {
            sInstance = new URLMonitor();
        }
        return sInstance;
    }

    static class ClientUrl {
        private String mClient;
        private String mUrl;

        ClientUrl(String client, String url) {
            mClient = client;
            mUrl = url;
        }

        @Override
        public int hashCode() {
            return (mClient + ";" + mUrl).hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return hashCode() == ((ClientUrl) other).hashCode();
        }
    }
}
