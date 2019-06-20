package ipOverUdp.routing;


import ipOverUdp.LinkLayer.Link;
import ipOverUdp.PacketFactory;
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

    public void updateRoutingTable(String intermediateInterface , ArrayList<Pair<String, Integer>> distances, ArrayList<Link> links) {
        for (Pair<String, Integer> set: distances) {
            if (targetsInterface.contains(set.getLeft()))
                targetsInterface.get(set.getLeft()).updateDistances(intermediateInterface, set.getRight() + 1);
            else {
                RoutingEntity re = new RoutingEntity(set.getLeft(), links);
                re.updateDistances(intermediateInterface, 1 + set.getRight());
                addTargetInterface(re);
            }

        }
    }

    public byte[] getRoutingPacketPayload(String receivingInterface) {
        byte[] res = new byte[targetsInterface.size() * 5];

        Enumeration<String> enumeration = targetsInterface.keys();
        int i = 0;
        while(enumeration.hasMoreElements()) {
            String ip = enumeration.nextElement();
            System.arraycopy(PacketFactory.ipStringToByteArray(ip), 0, res, i * 5, 4);
            // TODO: complete here.
        }
        return res;
    }


}
