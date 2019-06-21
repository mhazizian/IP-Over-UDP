package ipOverUdp;

import ipOverUdp.LinkLayer.Link;
import ipOverUdp.LinkLayer.Listener;
import ipOverUdp.routing.ForwardingTable;
import ipOverUdp.routing.RoutingEntity;
import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Hashtable;

public class Node {
    static final int MAX_NODE_NUMBER = 64;

    private Listener listener;
    private CommandHandler commandHandler;
    private ArrayList<Link> links;
    private ForwardingTable forwardingTable;
    private Hashtable<Integer, Runnable> ipProtocolHandler;

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
        //  3-forwarding table,
        //  4-DV routing algorithm,
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

        // TODO: close sockets. close open files.
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

            case QUIT:
//                this.runProgram = false;
                this.sendDistantVectorPackets();
                break;
        }
    }

    private void handelNewPacket(byte[] frameData, int frameSize) {
        PacketFactory packetParser = new PacketFactory(frameData, frameSize);
        switch (packetParser.getIpProtocol()) {
            case IpProtocolNumbers.DISTANT_VECTOR_PACKETS:
                this.handelDistantVectorPacket(packetParser);
                break;

            case IpProtocolNumbers.ROUTING_PACKET:
                if (isSelfInterface(packetParser.getDstIp())) {
                    // TODO: implement this part.
                    System.out.println("i got my packet :D");
                    packetParser.print();
                } else {
                    Link link = forwardingTable.getLink(packetParser.getDstIp());
                    link.sendFrame(packetParser.getPacketData(), packetParser.getPacketSize());
                }
                break;

            case IpProtocolNumbers.LINK_DOWN_PACKET:
//                deActivateLink(packetParser.getDstIp());
                Link link = Link.getLinkByInterface(packetParser.getDstIp(), links);
                if (link != null && link.getTargetInterface().equals(packetParser.getSrcIp()))
                    link.setActive(false);
                    this.sendDistantVectorPackets();
                break;

            case IpProtocolNumbers.TEST_PACKET:
                packetParser.print();
                break;

            default:

//                Runnable r = ipProtocolHandler.get(packetParser.getIpProtocol());
//                if (r != null)
//                    r.run(packetParser);
//                else {
//                    System.out.println("Invalid IP Protocol Number.");
//                    System.out.println("Dropping Packet.");
//                }
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

            System.out.println("Sending DV packet.");
            PacketFactory pf = new PacketFactory();
            pf.setIpProtocol(17);
            pf.setSrcIp(link.getLinkInterface());
            pf.setDstIp(link.getTargetInterface());
            pf.setPayload(forwardingTable.getRoutingPacketPayload(link.getTargetInterface()));

            link.sendFrame(pf.getPacketData(), pf.getPacketSize());
        }
    }

    private void handelDistantVectorPacket(PacketFactory packetParser) {
        System.out.println("Got DV Packet.");
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

        link.sendFrame(pf.getPacketData(), pf.getPacketSize());
    }
}
