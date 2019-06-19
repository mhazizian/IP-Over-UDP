package ipOverUdp;

import ipOverUdp.LinkLayer.Link;

public class PacketFactory {
    private static final int IP_PROTOCOL_INDX = 0;
    private static final int SRC_IP_INDX = 1;
    private static final int DST_IP_INDX = 5;
    private static final int PAYLOAD_INDX = 9;

    private static final int HEADER_SIZE = 9;


    private byte[] data;
    private int size;


    public PacketFactory() {
        // for creating new Packet
        data = new byte[Link.MAX_FRAME_SIZE];
        size = 0;

    }

    public PacketFactory(byte[] packetData, int size) {
        // for parsing existing Packet
        data = new byte[Link.MAX_FRAME_SIZE];

        System.arraycopy(packetData, 0, this.data, 0, size);
        this.size = size;

    }

    public void setPayload(byte[] newData) {
        System.arraycopy(newData, 0, this.data, PAYLOAD_INDX, newData.length);
        this.size += newData.length + HEADER_SIZE;

    }

    public void setSrcIp(byte[] ip) {
        System.arraycopy(ip, 0, this.data, SRC_IP_INDX, 4);
    }

    public void setDstIp(byte[] ip) {
        System.arraycopy(ip, 0, this.data, DST_IP_INDX, 4);
    }

    public void setIpProtocol(int protocolNum) {
        this.data[IP_PROTOCOL_INDX] = (byte) protocolNum;
    }


    public int getIpProtocol() {
        return (this.data[IP_PROTOCOL_INDX] & 0xFF);
    }

    public byte[] getSrcIp() {
        byte[] ip = new byte[4];
        System.arraycopy(this.data, SRC_IP_INDX, ip, 0, 4);
        return ip;
    }

    public byte[] getDstIp() {
        byte[] ip = new byte[4];
        System.arraycopy(this.data, DST_IP_INDX, ip, 0, 4);
        return ip;
    }

    public byte[] getPayload() {
        byte[] payload = new byte[size - HEADER_SIZE];
        System.arraycopy(this.data, PAYLOAD_INDX, payload, 0, size - HEADER_SIZE);
        return payload;
    }

    public byte[] getPacketData() {
        // TODO: should copy to another byte[] or not?!
        return this.data;
    }

    public int getPacketSize() {
        return this.size;
    }

}
