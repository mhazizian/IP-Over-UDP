package ipOverUdp;


import java.io.IOException;
import java.net.*;

public class Link {
    private int port;
    private String ip;
    private byte[] buffer;
    private DatagramSocket socket;

    private final static int MAX_FRAME_SIZE = 1400;

    public Link(String _ip, int _port) throws SocketException {
        this.ip = _ip;
        this.port = _port;

        this.socket = new DatagramSocket();
    }

    public void sendNewFrame(byte[] newData) {
        // TODO: encapsulate it,
        //  get ip packet protocol and add to frame header
        int size = generateFrameData(newData);

        sendFrame(this.buffer, size);
    }

    public void sendFrame(byte[] frameData, int size) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket frame = new DatagramPacket(frameData, size, address, port);
            this.socket.send(frame);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int generateFrameData(byte[] newData) {
        this.buffer = new byte[Link.MAX_FRAME_SIZE];
        System.arraycopy(newData, 0, this.buffer, 0, newData.length);

        // return actual frame size(payload + header)
        return newData.length;
    }
}
