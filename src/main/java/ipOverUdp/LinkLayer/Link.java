package ipOverUdp.LinkLayer;


import java.io.IOException;
import java.net.*;

public class Link {
    private int port;
    private String ip;
    private byte[] buffer;
    private DatagramSocket socket;

    final static int MAX_FRAME_SIZE = 1400;

    public Link(String _ip, int _port) throws SocketException {
        this.ip = _ip;
        this.port = _port;

        this.socket = new DatagramSocket();
    }

    public void sendNewFrame(byte[] newData, int newDataSize) {
        // TODO: encapsulate it,
        //  get ip packet protocol and add to frame header,
        //  on size > MAX_FRAME_SIZE, throws error
        int size = generateFrameData(newData, newDataSize);

        sendFrame(this.buffer, size);
    }

    public void sendFrame(byte[] frameData, int size) {
        // TODO: on size > MAX_FRAME_SIZE, throws error
        try {
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket frame = new DatagramPacket(frameData, size, address, port);
            this.socket.send(frame);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int generateFrameData(byte[] newData, int newDataSize) {
        this.buffer = new byte[Link.MAX_FRAME_SIZE];
        System.arraycopy(newData, 0, this.buffer, 0, newDataSize);

        // return actual frame size(payload + header)
        return newData.length;
    }
}
