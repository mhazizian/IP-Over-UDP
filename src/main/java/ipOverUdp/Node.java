package ipOverUdp;


import ipOverUdp.LinkLayer.Link;
import ipOverUdp.LinkLayer.Listener;
import ipOverUdp.protocolNumHandler.TestHandler;
import ipOverUdp.routing.ForwardingTable;
import ipOverUdp.routing.RoutingEntity;
import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Hashtable;

public class Node {
    static final int MAX_NODE_NUMBER = 64;

    private Listener listener;
    private CommandHandler commandHandler;
    private ArrayList<Link> links;
    private ForwardingTable forwardingTable;
    private Hashtable<Integer, Method> ipProtocolHandler;

    private int traceRouteTTL;
    private String traceRouteTragetIP;


    private boolean runProgram;

    public static void main(String[] args) {
        try {
            LnxParser lnxParser = new LnxParser(args[0]);
            new Node(lnxParser);

        } catch (FileNotFoundException e) {
            System.out.println("File " + args[0] + " not found!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Node(LnxParser input) throws IOException {
        // TODO:
        //  0-parse given file -- done
        //  1-initialize links -- done
        //  2-run command handler -- done
        //  3-forwarding table -- done
        //  4-DV routing algorithm -- done
        //  5-traceroute and ICMP

        listener = new Listener(input.getSelfHost(), input.getSelfPort());
        forwardingTable = new ForwardingTable();
        this.links = new ArrayList<>();
        commandHandler = new CommandHandler();
        ipProtocolHandler = new Hashtable<>();

        createLinksFromInputFile(input);

        for (Link link : this.links) {
            RoutingEntity re = new RoutingEntity(link.getTargetInterface(), links);
            forwardingTable.addTargetInterface(re);
        }

        this.runProgram = true;
        this.traceRouteTTL = -1;

        TestHandler.registerHandler(this);
        run();
    }

    private void run() throws IOException {
        while (this.runProgram) {
            if (commandHandler.hasNewCommand()) {
                Pair<CommandName, String[]> command = commandHandler.getCommand();
                if (command != null)
                    handelCommand(command.getKey(), command.getValue());
            }

            Pair<byte[], Integer> newFrame = listener.getFrame();
            if (newFrame != null)
                handelNewPacket(newFrame.getKey(), newFrame.getValue());
        }

        quietNode();
    }

    private void handelCommand(CommandName commandType, String[] args) {
        switch (commandType) {
            case UP:
                activateLink(args[0]);
                break;

            case DOWN:
                deActivateLink(args[0]);
                break;

            case INTERFACES:
                printLinks();
                break;

            case ROUTES:
                forwardingTable.printRoutes();
                break;

            case SEND:
                sendPacket(args[0], args[1], args[2]);
                break;

            case START:
                this.sendDistantVectorPackets();
                break;

            case TRACEROUTE:
                this.traceRouteInitializer(args[0]);
                break;

            case QUIT:
                this.runProgram = false;
                break;
        }
    }

    private void handelNewPacket(byte[] frameData, int frameSize) {
        PacketFactory packetParser = new PacketFactory(frameData, frameSize);
        if (packetParser.isICMP()) {
            if (isSelfInterface(packetParser.getDstIp())) {
                if (packetParser.getIpProtocol() == IpProtocolNumbers.ICMP_TIME_EXEEDED_PACKETS)
                    traceRouteHandler(packetParser);

            } else {
                Link link = forwardingTable.getLink(packetParser.getDstIp());
                if (link != null)
                    link.sendFrame(packetParser.getPacketData(), packetParser.getPacketSize());
                else
                    System.out.println("DstIp no recognized, packet dropped.");

            }
            return;
        }
        if (packetParser.getTTL() == 0) {
            sendICMPTimeExceeded(packetParser);
            return;
        }
        switch (packetParser.getIpProtocol()) {
            case IpProtocolNumbers.DISTANT_VECTOR_PACKETS:
                this.handelDistantVectorPacket(packetParser);
                break;

            case IpProtocolNumbers.ROUTING_PACKET:
                System.out.println(packetParser.getTTL() + " ");
                if (isSelfInterface(packetParser.getDstIp())) {
                    System.out.println("i got my packet :D");
                    packetParser.print();
                } else {
                    Link link = forwardingTable.getLink(packetParser.getDstIp());
                    packetParser.setTTL(packetParser.getTTL() - 1);
                    link.sendFrame(packetParser.getPacketData(), packetParser.getPacketSize());
                }
                break;

            case IpProtocolNumbers.LINK_DOWN_PACKET:
                Link link = Link.getLinkByInterface(packetParser.getDstIp(), links);
                if (link != null && link.getTargetInterface().equals(packetParser.getSrcIp()))
                    link.setActive(false);
                this.sendDistantVectorPackets();
                break;

            case IpProtocolNumbers.LINK_UP_PACKET:
                Link linkUp = Link.getLinkByInterface(packetParser.getDstIp(), links);
                if (linkUp != null && linkUp.getTargetInterface().equals(packetParser.getSrcIp()))
                    linkUp.setActive(true);
                this.sendDistantVectorPackets();
                break;

            case IpProtocolNumbers.TEST_PACKET:
                packetParser.print();
                break;

            default:
                if (ipProtocolHandler.containsKey(packetParser.getIpProtocol())) {
                    Method handler = ipProtocolHandler.get(packetParser.getIpProtocol());
                    try {
                        handler.invoke(null, packetParser);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                break;

        }
    }

    private void createLinksFromInputFile(LnxParser input) throws SocketException {
        while (input.nextLine()) {
            Link link = new Link(input.getLinkHost(), input.getLinkPort(), input.getLinkIpSrc(), input.getLinkIpDst());
            this.links.add(link);
        }
    }

    private boolean isSelfInterface(String interfaceIp) {
        for (Link link : links) {
            if (link.getLinkInterface().equals(interfaceIp))
                return true;
        }
        return false;
    }

    private void sendDistantVectorPackets() {
        for (Link link : links) {
            if (!link.isActive())
                continue;

//            System.out.println("Sending DV packet.");
            PacketFactory pf = new PacketFactory();
            pf.setIpProtocol(17);
            pf.setSrcIp(link.getLinkInterface());
            pf.setDstIp(link.getTargetInterface());
            pf.setTTL(1);
            pf.setPayload(forwardingTable.getRoutingPacketPayload(link.getTargetInterface()));

            link.sendFrame(pf.getPacketData(), pf.getPacketSize());
        }
    }

    private void handelDistantVectorPacket(PacketFactory packetParser) {
//        System.out.println("Got DV Packet.");
        boolean isChanged = this.forwardingTable.updateRoutingTable(
                packetParser.getDstIp(),
                packetParser.getPayloadInByteArray(),
                packetParser.getPayloadSize(),
                this.links
        );
        if (isChanged)
            this.sendDistantVectorPackets();
    }

    private void printLinks() {
        System.out.println("## Interfaces:");
        System.out.println("host       \tport\tfrom       \tto       \tisActive");

        for (Link link : links) {
            System.out.print(link.getIp() + "\t");
            System.out.print(link.getPort() + "\t");
            System.out.print(link.getLinkInterface() + "\t");
            System.out.print(link.getTargetInterface() + "\t");
            System.out.println(link.isActive());
        }
    }

    private void activateLink(String ip) {
        Link link = Link.getLinkByInterface(ip, links);
        if (link == null) {
            System.out.println("Invalid interface.");
            return;
        }

        if (link.isActive())
            return;

        link.setActive(true);

        PacketFactory pf = new PacketFactory();
        pf.setIpProtocol(IpProtocolNumbers.LINK_UP_PACKET);
        pf.setSrcIp(link.getLinkInterface());
        pf.setDstIp(link.getTargetInterface());
        pf.setTTL(1);
        link.sendFrame(pf.getPacketData(), pf.getPacketSize());

        // TODO: announce link up.
        this.sendDistantVectorPackets();
        System.out.println("done.");
    }

    private void deActivateLink(String ip) {
        Link link = Link.getLinkByInterface(ip, links);
        if (link == null) {
            System.out.println("Invalid interface.");
            return;
        }
        if (!link.isActive())
            return;

        PacketFactory pf = new PacketFactory();
        pf.setIpProtocol(IpProtocolNumbers.LINK_DOWN_PACKET);
        pf.setSrcIp(link.getLinkInterface());
        pf.setDstIp(link.getTargetInterface());
        pf.setTTL(1);
        link.sendFrame(pf.getPacketData(), pf.getPacketSize());
        link.setActive(false);

        // TODO: announce link down.
        this.sendDistantVectorPackets();
        System.out.println("done.");
    }

    private void sendPacket(String ip, String ipProtocol, String message) {
        Link link = forwardingTable.getLink(ip);
        if (link == null) {
            System.out.println("Dst ip not found.");
            return;
        }

        PacketFactory pf = new PacketFactory();
        pf.setIpProtocol(Integer.parseInt(ipProtocol));
        pf.setSrcIp(link.getLinkInterface());
        pf.setDstIp(ip);
        pf.setPayload(message);
        pf.setTTL(MAX_NODE_NUMBER - 1);

        link.sendFrame(pf.getPacketData(), pf.getPacketSize());
    }

    public void registerHandler(Method method, int protocolNum) {
        this.ipProtocolHandler.put(protocolNum, method);
    }

    private void quietNode() {
        for (Link link: links) {
            if (!link.isActive())
                continue;

            PacketFactory pf = new PacketFactory();
            pf.setIpProtocol(IpProtocolNumbers.LINK_DOWN_PACKET);
            pf.setSrcIp(link.getLinkInterface());
            pf.setDstIp(link.getTargetInterface());
            pf.setTTL(1);
            link.sendFrame(pf.getPacketData(), pf.getPacketSize());

            link.close();
        }
        listener.close();
    }

    private void sendICMPTimeExceeded(PacketFactory packetParser) {
        PacketFactory pf = new PacketFactory();
        Link link = forwardingTable.getLink(packetParser.getSrcIp());
        if (link == null) {
            System.out.println("Time Exceeded Packet, packet SRC unknown.");
            return;
        }

        pf.setSrcIp(link.getLinkInterface());
        pf.setDstIp(packetParser.getSrcIp());
        pf.setIpProtocol(IpProtocolNumbers.ICMP_TIME_EXEEDED_PACKETS);
        link.sendFrame(pf.getPacketData(), pf.getPacketSize());
    }

    public void traceRouteInitializer(String ip) {
        traceRouteTTL = 0;
        traceRouteTragetIP = ip;
        System.out.println("Traceroute to " + traceRouteTragetIP);
        sendTraceRoutePacket();
    }

    private void traceRouteHandler(PacketFactory pf) {
        if (traceRouteTTL < 0)
            return;

        System.out.println((traceRouteTTL + 1) + "  " + pf.getSrcIp());
        if (pf.getSrcIp().equals(traceRouteTragetIP)) {
            System.out.println("Traceroute finished in " + (traceRouteTTL + 1) +" hops");
            traceRouteTTL = -1;
            traceRouteTragetIP = "";
        } else {
            traceRouteTTL++;
            sendTraceRoutePacket();
        }
    }

    private void sendTraceRoutePacket() {
        PacketFactory pf = new PacketFactory();
        Link link = forwardingTable.getLink(traceRouteTragetIP);

        pf.setIpProtocol(IpProtocolNumbers.ROUTING_PACKET);
        pf.setDstIp(traceRouteTragetIP);
        pf.setSrcIp(link.getLinkInterface());
        pf.setTTL(traceRouteTTL);

        link.sendFrame(pf.getPacketData(), pf.getPacketSize());
    }
}
