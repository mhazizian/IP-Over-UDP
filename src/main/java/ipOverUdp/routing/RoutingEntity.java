package ipOverUdp.routing;

import ipOverUdp.LinkLayer.Link;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class RoutingEntity {
    private List<Pair<Link, Integer>> distances;
    private String targetInterface;

    public RoutingEntity(String _targetInterface) {
        this.targetInterface = _targetInterface;
        this.distances = new ArrayList<>();
    }

    public RoutingEntity(String _targetInterface, ArrayList<Link> links) {
        this(_targetInterface);
        for (Link link : links) {
            this.addPath(link, null);
        }
    }


    public void addPath(Link link, Integer distance) {
        if (distance == null)
            distance = -1;

//        this.distances.add(new Pair<>(link, distance));
        this.distances.add(new MutablePair<>(link, distance));
    }

    public void updateDistances(String intermediateInterface, Integer distance) {
        // intermediateInterface is virtualIp related to self Interfaces
        for (Pair<Link, Integer> path : this.distances) {
            if (path.getKey().getLinkInterface().equals(intermediateInterface)) {
                if (path.getValue() == -1 || path.getValue() > distance)
                    path.setValue(distance);
                return;
            }
        }

        throw new RuntimeException("given intermediateInterface does not exist!!!");
    }

    public Link getMinPath() {
        int minDistance = -1;
        Link optLink = null;

        for (Pair<Link, Integer> path : this.distances) {
            if (!path.getKey().isActive())
                continue;

            if (path.getValue() < minDistance || minDistance != -1) {
                minDistance = path.getValue();
                optLink = path.getKey();
            }
        }
        if (minDistance == -1)
            throw new RuntimeException("No valid path to given interface");

        return optLink;
    }

    public String getTargetInterface() {
        return targetInterface;
    }
}
