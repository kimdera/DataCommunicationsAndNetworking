package Others;

import Others.FileOperation;
import UDP.UDPClient;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class ClientLibrary {

    private UDPClient client;
    private String host;
    private int portNum;

    /* use specific data structure to store required information */
    private HashMap<String, String> requestHeader = new HashMap<>();
    private String requestBody;
    private HashMap<String, String> queryHM = new HashMap<>();

    public ClientLibrary(UDPClient client) {
        this.client = client;
    }

    public String GETorPOST(String method, String str) throws MalformedURLException {

        /* to get host and portNum from given URL */
        String url = str.substring(str.indexOf("http://"), str.indexOf("'", str.indexOf("http://")));
        URL mainUrl = new URL(url);
        this.host = mainUrl.getHost();
        this.portNum = mainUrl.getPort();

        if (str.contains("?")) {
            queryParams(mainUrl);// handle queryHM parameters
        }
        String response = null;
        try {
            if (method.equals("GET")) {
                response = buildResponse(
                        client.run(new InetSocketAddress(host, portNum), buildRequest("GET", str, url)), str);
            } else if (method.equals("POST")) {
                response = buildResponse(
                        client.run(new InetSocketAddress(host, portNum), buildRequest("POST", str, url)), str);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return response;
        }
    }

    private void queryParams(URL mainUrl) {
        String queryLine = mainUrl.getQuery();
        String[] queries = queryLine.split("&");
        for (String s : queries) {
            String[] queryKV = s.split("=");
            queryHM.put(queryKV[0], queryKV[1]);
        }
    }

    private String buildRequest(String method, String str, String url) throws IOException {

        /*
         * default information setting(Notice : not all of http requestHeader field
         * definitions
         * are defined,only those that appeared in the assignment are defined)
         */
        String connectionType = "close";
        String userAgent = "COMP445";
        String contentType = null;
        String contentLength = null;
        FileOperation fileOperation = new FileOperation();
        if (method.equals("POST")) {// these options are only valid under POST request
            contentType = "text/plain";
            if (str.contains("-d")) {
                requestBody = str.substring(str.indexOf("{", str.indexOf("-d")), str.indexOf("}") + 1);
            } else if (str.contains("-f")) {
                String path = str.substring(str.indexOf("-f") + 3, str.indexOf(" ", str.indexOf("-f") + 3));
                requestBody = fileOperation.readFile(path);
            }
            contentLength = String.valueOf(requestBody.length());
        }

        /* initial headers to hash-map */
        requestHeader.put("Host", this.host);
        requestHeader.put("Connection", connectionType);
        requestHeader.put("User-Agent", userAgent);
        if (method.equals("POST")) {
            requestHeader.put("Content-Length", contentLength);
            requestHeader.put("Content-Type", contentType);
        }

        /*-h Header requirement:support multiple headers add or update*/
        String stringVar = str;
        String key;
        String value;
        for (int i = 0; i < str.length(); i++) {
            if (!stringVar.contains("-h")) {
                break;
            } else {
                i = stringVar.indexOf("-h") + 3;
                stringVar = stringVar.substring(i);
                key = stringVar.substring(0, stringVar.indexOf(":", 0));
                value = stringVar.substring(stringVar.indexOf(":", stringVar.indexOf(key)) + 1,
                        stringVar.indexOf(" ", stringVar.indexOf(key)));
                if (requestHeader.containsKey(key)) {
                    requestHeader.replace(key, value);
                    continue;
                }
                requestHeader.put(key, value);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(method + " " + url + " HTTP/1.0\r\n");
        for (String keys : requestHeader.keySet()) {
            stringBuilder.append(keys).append(": ").append(requestHeader.get(keys)).append("\r\n");
        }
        if (method.equals("POST")) {
            stringBuilder.append("\r\n").append(requestBody);
        } else if (method.equals("GET")) {
            stringBuilder.append("\r\n");
        }
        return stringBuilder.toString();
    }

    /* str: whole command line input */
    private String buildResponse(String payload, String str) throws IOException {

        FileOperation fileOperation = new FileOperation();
        String response = payload;

        /* verbose requirement */
        if (str.contains("-v")) {// case that needs verbose
            if (outputToFile(str)) {// case need to output requestBody data
                fileOperation.writeFile(response, str.substring(str.indexOf("-o") + 3));
            }
            return response;
        } else {// case that does not need verbose
            response = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1);
            if (outputToFile(str)) {// case need to output requestBody data
                fileOperation.writeFile(response, str.substring(str.indexOf("-o") + 3));
            }
        }
        return response;
    }

    private boolean outputToFile(String str) {
        if (str.contains("-o")) {
            return true;
        }
        return false;
    }

    /* method to determine if the http response needs a redirection or not */
    private boolean needRedirection(String data) {
        data = data.substring(0, 20);// this is due to status will always be the first line, 0-20 characters for
                                     // approximation of it.
        if (data.contains("300") || data.contains("301") || data.contains("302") || data.contains("304")) {// satisfy
                                                                                                           // any of
                                                                                                           // those will
                                                                                                           // need
                                                                                                           // redirect
            return true;
        }
        return false;
    }
}
