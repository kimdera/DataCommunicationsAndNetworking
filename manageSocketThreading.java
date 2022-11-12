import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;

public class manageSocketThreading extends Thread {
    private ServerSocketChannel serverSocketChannel;
    private boolean debugMessage;
    private String directoryPath;
    private int portNum;
    private HashMap<String, String> query = new HashMap<>();

    public void setPort(int portNum) {
        this.portNum = portNum;
    }

    public void setDebugMessage(boolean debugMessage) {
        this.debugMessage = debugMessage;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public manageSocketThreading(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
        this.debugMessage = false;
        this.directoryPath = null;
        this.portNum = 8080;
    }

    public void run() {
        readInput();
        try {
            SocketAddress socketAddress = new InetSocketAddress(portNum);
            serverSocketChannel.bind(socketAddress);
            while (true) {
                SocketChannel client = serverSocketChannel.accept();
                handleRequest(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readInput() {
        Scanner inputStream = new Scanner(System.in);
        String userInput = inputStream.nextLine();
        if (!userInput.isEmpty() && userInput.contains("httpfs")) {
            if (userInput.contains("-p")) {
                String temp = userInput.substring(userInput.indexOf("-p") + 3);
                String portNumber = temp.substring(0, temp.indexOf(" ", 0));
                this.setPort(Integer.valueOf(portNumber));
            }
            if (userInput.contains("-v")) {
                this.setDebugMessage(true);
            }
            if (userInput.contains("-d")) {
                this.setDirectoryPath(userInput.substring(userInput.indexOf("-d") + 3));
            }
            System.out.println("The server has been launched");
        } else {
            System.out.println("Invalid command");
            System.exit(0);
        }
    }

    private void handleRequest(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(10000);
        int buffer = socketChannel.read(byteBuffer);
        String header = null;
        String body = null;
        if (buffer > 0) {
            byteBuffer.flip();
            String request = StandardCharsets.UTF_8.decode(byteBuffer).toString();
            if (debugMessage) {
                System.out.println("Here are the debug messages:");
                System.out.println(request);
                System.out.println();
            }
            byteBuffer.clear();
            header = request.split("\r\n\r\n")[0];
            if (header.contains("POST")) {
                body = request.split("\r\n\r\n")[1];
            }
        }
        socketChannel.socket().shutdownInput();

        OutputStream outputStream = socketChannel.socket().getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.write(response(header, body));
        printWriter.flush();
        printWriter.close();
        outputStream.close();
    }

    private String response(String header, String body) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(status(header) + "\r\n");

        if (status(header).contains("200") && status(header).contains("OK")) {
            stringBuilder.append("Connection: keep-alive\r\n");
        } else {
            stringBuilder.append(header.split("\r\n")[1] + "\r\n");
        }
        for (int i = 2; i < header.split("\r\n").length; i++) {
            stringBuilder.append(header.split("\r\n")[i] + "\r\n");
        }
        String[] firstLine = header.split("\r\n")[0].split(" ");
        String path = firstLine[1];
        if (!path.contains("get") && !path.contains("post")) {
            stringBuilder.append(contentType(header));
        }
        stringBuilder.append("\r\n\r\n");
        if (status(header).contains("200") && status(header).contains("OK")) {
            stringBuilder.append(locateFiles(header, body) + "\r\n");
        }
        if (path.contains("get") || path.contains("post")) {// normal request as A1
            stringBuilder.append(output(header, body));
        }
        return stringBuilder.toString();
    }

    private String status(String header) throws IOException {
        String[] firstLine = header.split("\r\n")[0].split(" ");
        String path = firstLine[1];

        if (path.contains("get") || path.contains("post")) {// normal request as A1
            return "HTTP/1.0 200 OK";
        }
        URL url = new URL(path);
        String fileName = url.getFile();
        File file = new File(this.directoryPath + fileName);
        if ((file.exists() || fileName.equals("") && header.contains("GET")) || header.contains("POST")) {
            // if(!file.canRead()&&header.contains("GET")){
            // return "HTTP/1.0 ERROR 500";
            // }
            return "HTTP/1.0 200 OK";
        } else {
            return "HTTP/1.0 ERROR 404";
        }
    }

    /* A1 normal request */
    private String output(String header, String body) throws MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder();
        String[] firstLine = header.split("\r\n")[0].split(" ");
        String path = firstLine[1];
        URL url = new URL(path);
        if (path.contains("?")) {
            queryParameters(url);
        }
        stringBuilder.append("{\r\n");
        stringBuilder.append("  \"args\": {\r\n");
        for (String key : query.keySet()) {
            stringBuilder.append("      ").append("\"" + key + "\"").append("\"" + query.get(key) + "\"")
                    .append(",\r\n");
        }
        stringBuilder.append("  },\r\n");
        if (header.contains("POST")) {
            stringBuilder.append("  \"data\": ").append("\"" + body + "\"\r\n");
        }
        stringBuilder.append("  \"headers\": {\r\n");
        for (int i = 2; i < header.split("\r\n").length; i++) {
            stringBuilder.append("      ").append(header.split("\r\n")[i] + "\r\n");
        }
        stringBuilder.append("  },\r\n");
        stringBuilder.append("  \"url\": ").append("\"" + url + "\"\r\n");
        stringBuilder.append("}\r\n");
        query.clear();
        return stringBuilder.toString();
    }

    private void queryParameters(URL u) {
        String queryLine = u.getQuery();
        String[] pair = queryLine.split("&");
        for (String s : pair) {
            String[] rest = s.split("=");
            query.put(rest[0], rest[1]);
        }
    }

    private synchronized String locateFiles(String header, String body) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String[] firstLine = header.split("\r\n")[0].split(" ");
        String path = firstLine[1];
        if (path.contains("get") || path.contains("post")) {
            return "";
        }
        URL url = new URL(path);
        String fileName = url.getFile();
        File file = new File(directoryPath + fileName);
        if (header.contains("GET")) {
            if (fileName.equals("")) {
                File[] fileList = file.listFiles((dir, name) -> name.charAt(0) != '.');
                stringBuilder.append("Here are the files in this directory:\r\n");
                for (File f : fileList) {
                    stringBuilder.append(f.getName() + "\r\n");
                }
            } else {
                manageFiles fileOperation = new manageFiles();
                stringBuilder.append(fileOperation.readFile(this.directoryPath + fileName));
            }
        } else if (header.contains("POST")) {
            manageFiles fileOperation = new manageFiles();
            fileOperation.writeFile(body, this.directoryPath + fileName);
        }
        return stringBuilder.toString();
    }

    private String contentType(String header) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] firstLine = header.split("\r\n")[0].split(" ");
        String path = firstLine[1];
        String type = path.substring(path.indexOf(".") + 1);
        switch (type) {
            case "html":
                stringBuilder.append("Content-Type: text/html\r\n");
                stringBuilder.append("Content-Disposition: inline\r\n");
                break;
            case "json": {
                stringBuilder.append("Content-Type: application/json\r\n");
                stringBuilder.append("Content-Disposition: inline\r\n");
                break;
            }
            case "txt": {
                stringBuilder.append("Content-Type: text/plain\r\n");
                stringBuilder.append("Content-Disposition: inline\r\n");
                break;
            }
            default: {
                stringBuilder.append("Content-Type: text/plain\r\n");
                stringBuilder.append("Content-Disposition: attachment\r\n");
            }
        }
        return stringBuilder.toString();
    }
}
