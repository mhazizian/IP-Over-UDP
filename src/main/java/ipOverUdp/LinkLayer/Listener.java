package ipOverUdp.LinkLayer;

import javafx.util.Pair;

import java.io.IOException;
import java.net.*;

public class Listener {
    private int port;
    private String ip;
    private byte[] buffer;
    private DatagramSocket socket;
    private DatagramPacket rcvdPacket;

    private final static int INPUT_BUFFER_SIZE = 10;

    public Listener(String _ip, int _port) throws SocketException, UnknownHostException {
        this.ip = _ip;
        this.port = _port;

        InetAddress address = InetAddress.getByName(ip);
        this.socket = new DatagramSocket(port, address);
        this.socket.setReceiveBufferSize(Listener.INPUT_BUFFER_SIZE);

        buffer = new byte[Link.MAX_FRAME_SIZE];
        rcvdPacket = new DatagramPacket(buffer, Link.MAX_FRAME_SIZE);
    }

    public Pair<byte[], Integer> getFrame() {
        try {
            this.socket.setSoTimeout(300);
            this.socket.receive(this.rcvdPacket);

            return new Pair<>(rcvdPacket.getData(), rcvdPacket.getLength());
        } catch (SocketTimeoutException e) {
            return null;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public void close() {
        this.socket.close();
    }
}
