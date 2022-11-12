import java.io.*;
import java.nio.channels.ServerSocketChannel;


public class httpFileServer {

    public static void main(String[] args) throws IOException {
        listenAndServe();
    }


    private static void listenAndServe() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        manageSocketThreading socketThread = new manageSocketThreading(serverSocketChannel);
        socketThread.start();
    }
}
