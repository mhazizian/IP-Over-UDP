package ipOverUdp;

import ipOverUdp.LinkLayer.Link;

import java.nio.charset.StandardCharsets;

public class PacketFactory {
    private static final int IP_PROTOCOL_IDX = 0;
    private static final int SRC_IP_IDX = 1;
    private static final int DST_IP_IDX = 5;
    private static final int TTL_IDX = 9;
    private static final int PAYLOAD_IDX = 10;

    private static final int HEADER_SIZE = 10;


    private byte[] data;
    private int size;

    public static byte[] ipStringToByteArray(String ip) {
        byte[] res = new byte[4];
        String[] splitedIp = ip.split("\\.", 4);

        for (int i = 0; i < 4; i++)
            res[i] = (byte) Integer.parseInt(splitedIp[i]);

        return res;
    }

    public static String ipByteArrayToString(byte[] ip) {
        String res = (ip[0] & 0xFF) + ".";
        res += (ip[1] & 0xFF) + ".";
        res += (ip[2] & 0xFF) + ".";
        res += Integer.toString(ip[3] & 0xFF);
        return res;

    }


    public PacketFactory() {
        // for creating new Packet
        data = new byte[Link.MAX_FRAME_SIZE];
        size = HEADER_SIZE;

    }

    public PacketFactory(byte[] packetData, int size) {
        // for parsing existing Packet
        data = new byte[Link.MAX_FRAME_SIZE];

        System.arraycopy(packetData, 0, this.data, 0, size);
        this.size = size;

    }

    public void setPayload(byte[] newData) {
        System.arraycopy(newData, 0, this.data, PAYLOAD_IDX, newData.length);
        this.size = newData.length + HEADER_SIZE;

    }

    public void setPayload(String newData) {
        this.setPayload(newData.getBytes(StandardCharsets.UTF_8));
    }

    public void setSrcIp(String ip) {
        byte[] byteIp = ipStringToByteArray(ip);
        System.arraycopy(byteIp, 0, this.data, SRC_IP_IDX, 4);
    }

    public void setDstIp(String ip) {
        byte[] byteIp = ipStringToByteArray(ip);
        System.arraycopy(byteIp, 0, this.data, DST_IP_IDX, 4);
    }

    public void setIpProtocol(int protocolNum) {
        this.data[IP_PROTOCOL_IDX] = (byte) protocolNum;
    }


    public int getIpProtocol() {
        return (this.data[IP_PROTOCOL_IDX] & 0xFF);
    }

    public void setTTL(int ttl) {
        this.data[TTL_IDX] = (byte) ttl;
    }

    public int getTTL() {
        return (this.data[TTL_IDX] & 0xFF);
    }

    public String getSrcIp() {
        byte[] ip = new byte[4];
        System.arraycopy(this.data, SRC_IP_IDX, ip, 0, 4);
        return ipByteArrayToString(ip);
    }

    public String getDstIp() {
        byte[] ip = new byte[4];
        System.arraycopy(this.data, DST_IP_IDX, ip, 0, 4);
        return ipByteArrayToString(ip);
    }

    public byte[] getPayloadInByteArray() {
        byte[] payload = new byte[size - HEADER_SIZE];
        System.arraycopy(this.data, PAYLOAD_IDX, payload, 0, size - HEADER_SIZE);
        return payload;
    }

    public int getPayloadSize() {
        return size - HEADER_SIZE;
    }

    public String getPayload() {
        return new String(this.getPayloadInByteArray(), StandardCharsets.UTF_8);
    }

    public byte[] getPacketData() {
        // TODO: should copy to another byte[] or not?!
        return this.data;
    }

    public int getPacketSize() {
        return this.size;
    }

    public void print() {
        System.out.println("ProtocolNum: " + getIpProtocol());
        System.out.println("SrcIP: " + getSrcIp());
        System.out.println("DstIP: " + getDstIp());
        System.out.println("Payload: " + getPayload());
    }

    public boolean isICMP() {
        return (getIpProtocol() < 13) && (getIpProtocol() != 0);
    }


}
