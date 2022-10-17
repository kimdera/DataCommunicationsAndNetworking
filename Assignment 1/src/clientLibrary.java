import java.io.*;
import java.util.HashMap;
//Provides classes for implementing networking apps
import java.net.*;

public class clientLibrary {

  private String host;
  private int portNum;

  // store requestHeader and queries on hashmaps
  private HashMap<String, String> requestHeader = new HashMap<>();
  private HashMap<String, String> queryHM = new HashMap<>();

  private String requestBody;
  String response = null;

  /*-----------------------------------------------------------------------------------------------
  * GET REQUEST
  *-----------------------------------------------------------------------------------------------
  */

  // catch exception if either no legal protocol could be found in a specification
  // string or the string could not be parsed
  public String GET(String str) throws MalformedURLException {

    // getting the host and port number from the url
    String url = str.substring(str.indexOf("http://"), str.indexOf("'", str.indexOf("http://")));
    // use Java's URL class (from .net library)
    URL mainUrl = new URL(url);

    host = mainUrl.getHost();
    portNum = mainUrl.getDefaultPort();

    // checking for query parameters
    if (str.contains("?"))
      queryParams(mainUrl);

    try {
      // open a socket with host and port number
      Socket socket = new Socket();
      SocketAddress socketAddress = new InetSocketAddress(host, portNum);
      socket.connect(socketAddress);

      // sending get http request
      sendHttpRequest("GET", socket, str, mainUrl, url);
      // getting get http response
      response = receiveResponse("GET", socket, str, mainUrl, url);

      if (response.contains("HTTP/1.0") || response.contains("HTTP/1.1") || response.contains("HTTP/2.0")) {
        if (needRedirection(response)) {// check if this request needs to be redirected or not
          // ex: httpc get
          // 'http:httpbin.org/redirect-to?url=http://httpbin.org/get?course=networking&assignment=1&status_code=200'
          sendHttpRequest(response, socket, str, mainUrl, queryHM.get("url"));
          response = receiveResponse("POST", socket, str, mainUrl, queryHM.get("url"));
        }
      }

      socket.close();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      return response;
    }
  }

  /*-----------------------------------------------------------------------------------------------
  * POST REQUEST
  *-----------------------------------------------------------------------------------------------
  */

  // catch exception if either no legal protocol could be found in a specification
  // string or the string could not be parsed
  public String POST(String str) throws MalformedURLException {

    // getting the host and port number from the url
    String url = str.substring(str.indexOf("http://"), str.indexOf("'", str.indexOf("http://")));
    URL mainUrl = new URL(url);
    host = mainUrl.getHost();
    portNum = mainUrl.getDefaultPort();
    String response = null;

    // checking for query parameters
    if (str.contains("?")) {
      queryParams(mainUrl);
    }

    try {
      // opening a socket
      Socket socket = new Socket();
      SocketAddress socketAddress = new InetSocketAddress(host, portNum);
      socket.connect(socketAddress);

      // sending post http request
      sendHttpRequest("POST", socket, str, mainUrl, url);

      // getting post http response
      response = receiveResponse("POST", socket, str, mainUrl, url);
      if (response.contains("HTTP/1.0") || response.contains("HTTP/1.1") || response.contains("HTTP/2.0")) {
        if (needRedirection(response)) {// check if this request needs to be redirected or not
          // ex: httpc post
          // 'http:httpbin.org/redirect-to?url=http://httpbin.org/get?course=networking&assignment=1&status_code=200'
          sendHttpRequest(response, socket, str, mainUrl, queryHM.get("url"));
          response = receiveResponse("POST", socket, str, mainUrl, queryHM.get("url"));
        }
      }

      socket.close();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      return response;
    }
  }

  // Query Parameters
  // --------------------------------------------------------------------------------------------------------------------

  public void queryParams(URL mainUrl) {
    // gets query after ? and splits by &
    String[] queries = mainUrl.getQuery().split("&");
    for (String s : queries) {
      // add query key values in the query hashmap
      String[] queryKV = s.split("=");
      queryHM.put(queryKV[0], queryKV[1]);
    }
  }

  // Query Parameters
  // --------------------------------------------------------------------------------------------------------------------

  public void sendHttpRequest(String method, Socket socket, String str, URL mainUrl, String url) throws IOException {
    // method: post or get
    // str: input string
    // mainURL: parsed url using the java URL class
    // url: url part of the input

    // setting default values to http request headers seen in the assignment
    // doc----------------------------------------------------------
    String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36";
    String contentLength = null;
    String contentType = null;
    String connectionType = "close";

    // valid options for POST
    // requests----------------------------------------------------------------------------------------------------
    if (method.equals("POST")) {
      contentType = "text/plain";

      // -d string Associates an inline data to the body HTTP POST request. [-d
      // inline-data]
      // Post with inline data example:
      // httpc post -h Content-Type:application/json --d '{"Assignment": 1}'
      // http://httpbin.org/post

      // -f file Associates the content of a file to the body HTTP POST request. [-f
      // file]

      if (str.contains("-d")) {
        requestBody = str.substring(str.indexOf("{", str.indexOf("-d")), str.indexOf("}") + 1);
      } else if (str.contains("-f")) {
        String path = str.substring(str.indexOf("-f") + 3, str.indexOf(" ", str.indexOf("-f") + 3));
        requestBody = readFile(path);
      }
      contentLength = String.valueOf(requestBody.length());
    }

    // Initialize request headers values to the request header hash
    // map-----------------------------------------------------------------

    requestHeader.put("Host", this.host);
    requestHeader.put("Connection", connectionType);
    requestHeader.put("User-Agent", userAgent);
    if (method.equals("POST")) {
      requestHeader.put("Content-Length", contentLength);
      requestHeader.put("Content-Type", contentType);
    }

    // -h requestHeader, add or update various request
    // headers--------------------------------------------------------------------------

    String key;
    String value;
    String stringVar = str;

    for (int i = 0; i < str.length(); i++) {
      if (!stringVar.contains("-h")) {
        break;
      } else {
        i = stringVar.indexOf("-h") + 3;
        // grab -h values from index+3
        stringVar = stringVar.substring(i);
        // [-h key:value]
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

    // creating and sending requests
    // -----------------------------------------------------------------------------------------------------------------

    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
    StringBuilder stringBuilder = new StringBuilder();
    BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

    stringBuilder.append(method + " " + url + " HTTP/1.0\r\n");

    for (String keys : requestHeader.keySet()) {
      stringBuilder.append(keys).append(": ").append(requestHeader.get(keys)).append("\r\n");
    }

    if (method.equals("POST")) {
      stringBuilder.append("\r\n").append(requestBody);
    } else if (method.equals("GET")) {
      stringBuilder.append("\r\n");
    }

    bufferedWriter.write(stringBuilder.toString());
    bufferedWriter.flush();
  }

  // handle user input string
  // -----------------------------------------------------------------------------------------------------------------------

  public String receiveResponse(String method, Socket socket, String str, URL mainUrl, String url) throws IOException {
    InputStream inputStream = socket.getInputStream();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    StringBuilder stringBuilder = new StringBuilder();
    String data;
    do {
      data = bufferedReader.readLine();
      stringBuilder.append(data + "\r\n");
    } while (data != null);

    String response = stringBuilder.toString();
    bufferedReader.close();
    inputStream.close();

    // -v verbose
    // ----------------------------------------------------------------------------------------------------------------------------------

    if (str.contains("-v")) {
      // -v Prints the detail of the response such as protocol, status, and headers.
      // option –o filename, which allow the HTTP client to write the body of the
      // response to the specified file instead of the console
      // example: httpc -v 'http://httpbin.org/get?course=networking&assignment=1' -o
      // hello.txt

      // check if we need to output body to a file -o
      if (outputToFile(str)) {
        outputFile(response, str.substring(str.indexOf("-o") + 3));
      }
      return response;
    } else {
      // if doesn't need verbose
      response = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1);
      // check if we need to output body to a file -o
      if (outputToFile(str)) {
        outputFile(response, str.substring(str.indexOf("-o") + 3));
      }
    }
    return response;
  }

  // reading body from a file
  // --------------------------------------------------------------------------------------------------------------------------

  public String readFile(String path) throws IOException {

    File file = new File(path);
    StringBuilder stringBuilder = new StringBuilder();
    String nextFileString;

    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

    do {
      nextFileString = bufferedReader.readLine();
      stringBuilder.append(nextFileString);
    } while (nextFileString != null);
    return stringBuilder.toString();
  }

  // -o output to a file
  // --------------------------------------------------------------------------------------------------------------------------------

  public void outputFile(String requestBody, String filePath) {
    File file = new File(filePath);
    BufferedWriter bufferedOWriter = null;
    try {
      bufferedOWriter = new BufferedWriter(new FileWriter(file));
      bufferedOWriter.write(requestBody);
      bufferedOWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean outputToFile(String str) {
    if (str.contains("-o")) {
      return true;
    }
    return false;
  }

  // method checking if http response has to be redirected
  // ----------------------------------------------------------------------------------------------

  public boolean needRedirection(String data) {
    // client receives the 3xx on the first line usually the 20th char
    data = data.substring(0, 20);
    // if client receives a redirection code (3xx)
    if (data.contains("200") || data.contains("301") || data.contains("302") || data.contains("304")) {
      return true;
    }
    return false;
  }
}
