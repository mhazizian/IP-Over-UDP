package ipOverUdp.LinkLayer;


import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class Link {
    private int port;
    private String ip;
    private String linkInterface; // interface related to src
    private String targetInterface; // interface related to dst
    private boolean isActive;

    private byte[] buffer;
    private DatagramSocket socket;

    public final static int MAX_FRAME_SIZE = 1400;

    public Link(String _ip, int _port, String linkInterface, String targetInterface) throws SocketException {
        this.ip = _ip;
        this.port = _port;
        this.linkInterface = linkInterface;
        this.targetInterface = targetInterface;
        this.isActive = true;
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
        if (!isActive) {
            System.out.println("Link is deactivate, can't send frame.");
            return;
        }


        if (size > MAX_FRAME_SIZE)
            throw new RuntimeException("frameSize is greater than MTU");

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

    public String getLinkInterface() {
        return linkInterface;
    }

    public String getTargetInterface() {
        return this.targetInterface;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public static Link getLinkByInterface(String ipInterface ,ArrayList<Link> links) {
        for(Link link: links) {
            if (link.getLinkInterface().equals(ipInterface))
                return link;
        }
        return null;
    }
}
