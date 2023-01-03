import java.io.*;
// In Java NIO, the channel is a medium used to transport data between the entity and byte buffers. 
// It reads the data from an entity and places it inside buffer blocks for consumption. Channels act as gateway provided by java NIO to access the I/O mechanism.
//A channel represents an open connection to an entity such as a network socket (for reading or writing).
//ServerSocketChannel: A channel to a stream-oriented listening socket
import java.nio.channels.ServerSocketChannel;

public class httpFileServer {
    //ListenAndServe starts an HTTP server with a given address and handler. The handler is usually nil, which means to use DefaultServeMux.
    public static void main(String[] args) throws IOException {listenAndServe();}


    private static void listenAndServe() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //SocketThread listens to a socket and handles one HTTP request for a file in the start directory.
        manageSocketThreading socketThread = new manageSocketThreading(serverSocketChannel);
        socketThread.start();
    }
}
