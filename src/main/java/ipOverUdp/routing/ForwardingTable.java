package ipOverUdp.routing;


import ipOverUdp.LinkLayer.Link;
import ipOverUdp.PacketFactory;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class ForwardingTable {
    private Hashtable<String, RoutingEntity> targetsInterface;

    public ForwardingTable() {
        this.targetsInterface = new Hashtable<>();
    }

    public Link getLink(String interfaceIp) {
        if (targetsInterface.containsKey(interfaceIp))
            return targetsInterface.get(interfaceIp).getMinPath();
        else
            throw new RuntimeException("given interface not found");
    }

    public void addTargetInterface(RoutingEntity re) {
        targetsInterface.put(re.getTargetInterface(), re);
    }

    public boolean updateRoutingTable(String intermediateInterface, ArrayList<Pair<String, Integer>> distances, ArrayList<Link> links) {
        boolean isChanged = false;
        for (Pair<String, Integer> set : distances) {
            int distance = set.getRight();
            if (distance != -1)
                distance += 1;

            if (targetsInterface.contains(set.getLeft()))
                isChanged = isChanged | targetsInterface.get(set.getLeft()).updateDistances(intermediateInterface, distance);
            else {
                RoutingEntity re = new RoutingEntity(set.getLeft(), links);
                re.updateDistances(intermediateInterface, distance);
                addTargetInterface(re);
                isChanged = true;
            }

        }
        return isChanged;
    }

    public boolean updateRoutingTable(String myInterfaceIp, byte[] payload, int payloadSize, ArrayList<Link> links) {
        byte[] temp = new byte[4];
        ArrayList<Pair<String, Integer>> distances = new ArrayList<>();

        for (int i = 0; i < payloadSize; i += 5) {
            System.arraycopy(payload, i, temp, 0, 4);
            String ip = PacketFactory.ipByteArrayToString(temp);
            Integer distance = payload[i + 4] & 0xFF;
            distances.add(new MutablePair<>(ip, distance));
        }
        return this.updateRoutingTable(myInterfaceIp, distances, links);
    }

    public byte[] getRoutingPacketPayload(String receivingInterface) {
        byte[] res = new byte[targetsInterface.size() * 5];

        Enumeration<String> enumeration = targetsInterface.keys();
        int i = 0;
        while (enumeration.hasMoreElements()) {
            String ip = enumeration.nextElement();
            System.arraycopy(PacketFactory.ipStringToByteArray(ip), 0, res, i * 5, 4);
            Pair<Link, Integer> path = this.targetsInterface.get(ip).getMinPathInfo();
            if (path.getRight() == -1)
                continue;

            if (path.getLeft().getTargetInterface().equals(receivingInterface))
                res[5 * i + 4] = -1;
            else
                res[5 * i + 4] = path.getRight().byteValue();

            i++;
        }
        return res;
    }

    public void printRoutes() {
        System.out.println("## Routes:");
        System.out.println("DstIp       \tthrough  \tDistance");

        Enumeration<String> enumeration = targetsInterface.keys();
        while(enumeration.hasMoreElements()) {
            String ip = enumeration.nextElement();
            System.out.print(ip + "\t");

            Pair<Link, Integer> path = targetsInterface.get(ip).getMinPathInfo();
            if (path.getLeft() == null)
                System.out.print("invalid   \t");
            else
                System.out.print(path.getLeft().getLinkInterface()+ "\t");

            System.out.println(path.getRight()+ "\t");


        }
    }



}
