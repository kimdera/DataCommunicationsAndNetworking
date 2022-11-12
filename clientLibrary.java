import java.io.*;
//Provides classes for implementing networking apps
import java.net.*;
import java.util.HashMap;

public class clientLibrary {


    private String host;
    private int portNum;

    // store requestHeader and queries on hashmaps
    private HashMap<String,String> requestHeader = new HashMap<>();
    private String requestBody;
    private HashMap<String,String> queryHM = new HashMap<>();

    /*-----------------------------------------------------------------------------------------------
    * GET REQUEST
    *-----------------------------------------------------------------------------------------------
    */
    
    public String GET(String str) throws MalformedURLException{

        /*to get host and port from given URL*/
        String url = str.substring(str.indexOf("http://"), str.indexOf("'",str.indexOf("http://")));
        URL mainURL = new URL(url);
        host = mainURL.getHost();
        if(host.equals("localhost")){
            portNum = 8080;
        }else {
            portNum = mainURL.getDefaultPort();
        }
        //managing the query params
        if(str.contains("?")) {
            queryParams(mainURL);
        }

        String response = null;

        try {

            // open a socket with host and port number
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(host, portNum);
            socket.connect(socketAddress);

            //send http request
            sendRequest("GET",socket,str,mainURL,url);

            //receive http response
            response = receiveResponse("GET",socket,str,mainURL,url);

            //close socket
            socket.close();
        }catch (UnknownHostException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        finally {
            return response;
        }
    }

    /*-----------------------------------------------------------------------------------------------
    * POST REQUEST
    *-----------------------------------------------------------------------------------------------
    */

    public String POST(String str) throws MalformedURLException{

        //get port and host from url
        String url = str.substring(str.indexOf("http://"), str.indexOf("'",str.indexOf("http://")));
        URL mainURL = new URL(url);
        host = mainURL.getHost();
        if(host.equals("localhost")){
            portNum = 8080;
        }else {
            portNum = mainURL.getDefaultPort();
        }
        if(str.contains("?")) {
            queryParams(mainURL);//handle query parameters
        }

        String response = null;

        try {
            /*open a socket*/
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(host, portNum);
            socket.connect(socketAddress);

            /*send http request*/
            sendRequest("POST",socket,str,mainURL,url);

            /*receive http response*/
            response = receiveResponse("POST",socket,str,mainURL,url);

            /*close a socket*/
            socket.close();
        }catch (UnknownHostException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        finally {
            return response;
        }
    }

    // Query Parameters
    // --------------------------------------------------------------------------------------------------------------------
    

    public void queryParams(URL mainURL){
        String queryLine = mainURL.getQuery();
        String [] queries = queryLine.split("&");
        for (String s:queries) {
            String [] queryKV = s.split("=");
            queryHM.put(queryKV[0],queryKV[1]);
        }
    }

    // sendHttpRequest
   // --------------------------------------------------------------------------------------------------------------------

    //method :get/post  str: command line input  url:  URL 
    public void sendRequest(String method,Socket socket,String str,URL mainURL,String url) throws IOException{

        // setting default values to http request headers seen in the assignment doc 
        String connectionType = "close";
        String userAgent = "COMP445";
        String contentType = null;
        String contentLength = null;
        manageFiles manageFiles = new manageFiles();

        // valid options for POST requests
        // ----------------------------------------------------------------------------------------------------
        if(method.equals("POST")) {
            contentType = "text/plain";

                // -d string Associates an inline data to the body HTTP POST request. [-d
                // inline-data]
                // Post with inline data example:
                // httpc post -h Content-Type:application/json --d '{"Assignment": 1}'
                // http://httpbin.org/post

                // -f file Associates the content of a file to the body HTTP POST request. [-f
                // file]

            if(str.contains("-d")) {
                requestBody = str.substring(str.indexOf("{", str.indexOf("-d")), str.indexOf("}") + 1);
            }else if(str.contains("-f")){
                String path = str.substring(str.indexOf("-f") + 3,str.indexOf(" ",str.indexOf("-f") + 3));
                requestBody = manageFiles.readFile(path);
            }
            contentLength = String.valueOf(requestBody.length());
        }

    // Initialize request headers values to the request header hash map
    // -----------------------------------------------------------------
        requestHeader.put("Host",this.host);
        requestHeader.put("Connection",connectionType);
        requestHeader.put("User-Agent",userAgent);
        if(method.equals("POST")) {
            requestHeader.put("Content-Length", contentLength);
            requestHeader.put("Content-Type", contentType);
        }


    // -h requestHeader, add or update various request headers
    // --------------------------------------------------------------------------
        String stringVar = str;
        String key;
        String value;
        for (int i = 0; i < str.length(); i++) {
            if(!stringVar.contains("-h")){
                break;
            }else{
                i = stringVar.indexOf("-h") + 3;
                stringVar = stringVar.substring(i);
                key = stringVar.substring(0,stringVar.indexOf(":",0));
                value = stringVar.substring(stringVar.indexOf(":",stringVar.indexOf(key))+ 1,stringVar.indexOf(" ",stringVar.indexOf(key)));
                if(requestHeader.containsKey(key)){
                    requestHeader.replace(key,value);
                    continue;
                }
                requestHeader.put(key,value);
            }
        }

    // creating and sending requests
    // -----------------------------------------------------------------------------------------------------------------
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
        BufferedWriter bufferedWriter= new BufferedWriter(outputStreamWriter);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(method + " " + url + " HTTP/1.0\r\n");
        for (String keys:requestHeader.keySet()) {
            stringBuilder.append(keys).append(": ").append(requestHeader.get(keys)).append("\r\n");
        }
        if(method.equals("POST")){
            stringBuilder.append("\r\n").append(requestBody);
        }else if(method.equals("GET")){
            stringBuilder.append("\r\n");
        }
        bufferedWriter.write(stringBuilder.toString());
        bufferedWriter.flush();
    }

    // handle user input string
   // -----------------------------------------------------------------------------------------------------------------------
    public String receiveResponse(String method,Socket socket,String str,URL mainURL,String url) throws IOException{
        InputStream inputStream = socket.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String data;
        manageFiles manageFiles = new manageFiles();
        do {
            data = bufferedReader.readLine();
            stringBuilder.append(data+"\r\n");
        }
        while (data != null);
        String response = stringBuilder.toString();
        bufferedReader.close();
        inputStream.close();

    // -v verbose
    // ----------------------------------------------------------------------------------------------------------------------------------
        if(str.contains("-v")) {//case that needs verbose
            if(outputToFile(str)){//case need to output body data
                manageFiles.writeFile(response,str.substring(str.indexOf("-o") + 3));
            }
            return response;
        }else {//case that does not need verbose
            response = response.substring(response.indexOf("{"),response.lastIndexOf("}")+ 1);
            if(outputToFile(str)){//case need to output body data
                manageFiles.writeFile(response,str.substring(str.indexOf("-o") + 3));
            }
        }
        return response;
    }

    public boolean outputToFile(String str){
        if(str.contains("-o")){
            return true;
        }
        return false;
    }
}
