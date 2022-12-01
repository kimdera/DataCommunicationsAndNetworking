package UDP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;

import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPClient {

    private SocketAddress localAddress;
    private SocketAddress routerAddress;
    private boolean handShakeSuccess = false;

    public UDPClient() {
        this.localAddress = new InetSocketAddress(41830);
        this.routerAddress = new InetSocketAddress("localhost", 3000);
    }

    public UDPClient(int localPort) {
        this.localAddress = new InetSocketAddress(localPort);
        this.routerAddress = new InetSocketAddress("localhost", 3000);
    }

    public String run(InetSocketAddress serverAddress, String message) throws IOException {
        DatagramChannel datagramChannel = DatagramChannel.open();
        datagramChannel.bind(this.localAddress);
        long newSeq = threeWayHandShake(datagramChannel, serverAddress);
        if (this.handShakeSuccess) {
            Packet p = null;
            if (message.getBytes().length <= Packet.MAX_DATA) {
                p = new Packet.Builder()
                        .setType(0)
                        .setSequenceNumber(newSeq + 1)
                        .setPortNumber(serverAddress.getPort())
                        .setPeerAddress(serverAddress.getAddress())
                        .setPayload(message.getBytes())
                        .create();
            } else {
                return "Data is too long for a single packet";
            }
            datagramChannel.send(p.packetToBuffer(), routerAddress);
            timer(datagramChannel, p);

            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            this.routerAddress = datagramChannel.receive(buf);
            buf.flip();
            Packet resp = Packet.bufferToPacket(buf);
            if (resp.getType() == 1) {
                String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
                return payload;
            }
        }
        return null;
    }

    private void timer(DatagramChannel datagramChannel, Packet packet) throws IOException {
        // receiving a packet within the timeout
        try {
            datagramChannel.configureBlocking(false);
            Selector selector = Selector.open();
            datagramChannel.register(selector, OP_READ);
            selector.select(1000);

            Set<SelectionKey> keys = selector.selectedKeys();
            if (keys.isEmpty()) {
                datagramChannel.send(packet.packetToBuffer(), routerAddress);
                timer(datagramChannel, packet);
            }
            keys.clear();
            return;
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // mimicking TCP three way handshake
    private long threeWayHandShake(DatagramChannel datagramChannel, InetSocketAddress serverAddress)
            throws IOException {

        String testString = "Hi S";
        Packet test = new Packet.Builder()
                .setType(2)
                .setSequenceNumber(1L)
                .setPortNumber(serverAddress.getPort())
                .setPeerAddress(serverAddress.getAddress())
                .setPayload(testString.getBytes())
                .create();
        datagramChannel.send(test.packetToBuffer(), routerAddress);
        System.out.println("Handshaking #1 SYN packet has already been sent out");

        timer(datagramChannel, test);

        System.out.println("after the timer has finished: ");
        ByteBuffer byteBuffer = ByteBuffer.allocate(Packet.MAX_LEN);
        byteBuffer.clear();
        datagramChannel.receive(byteBuffer);
        byteBuffer.flip();
        System.out.println("after the buffer flip has been done: ");
        Packet packet = Packet.bufferToPacket(byteBuffer);

        System.out.println("Message from the server is :" + new String(packet.getPayload(), StandardCharsets.UTF_8));
        this.handShakeSuccess = true;
        System.out.println("Three-way handshake is successful. Therefore, data will start transferring");
        System.out.println("\r\n");
        return packet.getSequenceNumber();
    }
}
