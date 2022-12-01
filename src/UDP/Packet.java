package UDP;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// network simulation of a packet. Using larger types for packets.
public class Packet {
    public static final int DATA = 0;
    public static final int ACK = 1;
    public static final int SYN_1 = 2;
    public static final int SYN_2 = 3;

    public static final int MIN_LEN = 11;
    public static final int MAX_LEN = 1024;
    public static final int MAX_DATA = MAX_LEN - MIN_LEN;

    private final int type;
    private final long sequenceNumber;
    private final InetAddress peerAddress;
    private final int peerPort;
    private final byte[] payload;

    public Packet(int type, long sequenceNumber, InetAddress peerAddress, int peerPort, byte[] payload) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        this.payload = payload;
    }

    public int getType() {
        return type;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public InetAddress getPeerAddress() {
        return peerAddress;
    }

    public int getPeerPort() {
        return peerPort;
    }

    public byte[] getPayload() {
        return payload;
    }

    /**
     * creating a builder from the current packet.
     * The builder is used to create another packet by reusing some parts of the
     * current packet.
     */
    public Builder packetToBuilder() {
        return new Builder()
                .setType(type)
                .setSequenceNumber(sequenceNumber)
                .setPeerAddress(peerAddress)
                .setPortNumber(peerPort)
                .setPayload(payload);
    }

    /**
     * Writing a raw presentation of the packet to byte buffer. buffer order is in
     * BigEndian
     */
    private void write(ByteBuffer buffer) {
        buffer.put((byte) type);
        buffer.putInt((int) sequenceNumber);
        buffer.put(peerAddress.getAddress());
        buffer.putShort((short) peerPort);
        buffer.put(payload);
    }

    /**
     * Creating a byte buffer in BigEndian for the packet.
     * The returned buffer is flipped.
     */
    public ByteBuffer packetToBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        write(buffer);
        buffer.flip();
        return buffer;
    }

    /**
     * Returning a raw representation of the packet.
     */
    public byte[] packetToBytes() {
        ByteBuffer buffer = packetToBuffer();
        byte[] raw = new byte[buffer.remaining()];
        buffer.get(raw);
        return raw;
    }

    /**
     * creating a packet from the given ByteBuffer in BigEndian.
     */
    public static Packet bufferToPacket(ByteBuffer buffer) throws IOException {
        if (buffer.limit() < MIN_LEN || buffer.limit() > MAX_LEN) {
            throw new IOException("Invalid length");
        }

        Builder builder = new Builder();

        builder.setType(Byte.toUnsignedInt(buffer.get()));
        builder.setSequenceNumber(Integer.toUnsignedLong(buffer.getInt()));

        byte[] host = new byte[] { buffer.get(), buffer.get(), buffer.get(), buffer.get() };
        builder.setPeerAddress(Inet4Address.getByAddress(host));
        builder.setPortNumber(Short.toUnsignedInt(buffer.getShort()));

        byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);
        builder.setPayload(payload);

        return builder.create();
    }

    /**
     * creating a packet from the given array of bytes.
     */
    public static Packet fromBytes(byte[] bytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        buffer.put(bytes);
        buffer.flip();
        return bufferToPacket(buffer);
    }

    @Override
    public String toString() {
        return String.format("#%d peer=%s:%d, size=%d", sequenceNumber, peerAddress, peerPort, payload.length);
    }

    public static class Builder {
        private int type;
        private long sequenceNumber;
        private InetAddress peerAddress;
        private int portNumber;
        private byte[] payload;

        public Builder setType(int type) {
            this.type = type;
            return this;
        }

        public Builder setSequenceNumber(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public Builder setPeerAddress(InetAddress peerAddress) {
            this.peerAddress = peerAddress;
            return this;
        }

        public Builder setPortNumber(int portNumber) {
            this.portNumber = portNumber;
            return this;
        }

        public Builder setPayload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public Packet create() {
            return new Packet(type, sequenceNumber, peerAddress, portNumber, payload);
        }
    }

}
