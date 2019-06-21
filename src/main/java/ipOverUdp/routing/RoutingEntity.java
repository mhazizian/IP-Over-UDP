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
            if (link.getTargetInterface().equals(this.targetInterface))
                this.addPath(link, 1);
            else if (link.getLinkInterface().equals(this.targetInterface))
                this.addPath(link, 0);
            else
                this.addPath(link, null);

        }
    }


    public void addPath(Link link, Integer distance) {
        if (distance == null)
            distance = 255;

        if (link.getLinkInterface().equals(targetInterface))
            distance = 0;

        this.distances.add(new MutablePair<>(link, distance));
    }

    public boolean updateDistances(String intermediateInterface, Integer distance) {
        // intermediateInterface is virtualIp related to self Interfaces
        for (Pair<Link, Integer> path : this.distances) {

            if (path.getLeft().getLinkInterface().equals(intermediateInterface)) {
                if (path.getLeft().getLinkInterface().equals(targetInterface)) {
                    path.setValue(0);
                } else if (!path.getValue().equals(distance)) {
//                    System.out.println("changed: " + path.getLeft().getLinkInterface() + " from:" + path.getValue() +" into=" + distance + " dest:" + targetInterface);
                    path.setValue(distance);
                    return true;
                }
                return false;
            }
        }

        throw new RuntimeException("given intermediateInterface does not exist!!!");
    }

    public Pair<Link, Integer> getMinPathInfo() {

        int minDistance = 255;
        Link optLink = null;

        for (Pair<Link, Integer> path : this.distances) {
            if (!path.getLeft().isActive())
                continue;

            if (path.getRight() == 255)
                continue;

            if (path.getRight() < minDistance) {
                minDistance = path.getRight();
                optLink = path.getLeft();
            }
        }
//        if (minDistance == -1)
//            throw new RuntimeException("No valid path to given interface");

        return new MutablePair<>(optLink, minDistance);
    }

    public Link getMinPath() {
        return this.getMinPathInfo().getLeft();
    }


    public String getTargetInterface() {
        return targetInterface;
    }
}
