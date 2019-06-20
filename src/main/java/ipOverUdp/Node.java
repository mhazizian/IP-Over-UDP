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

        for(Link link: this.links) {
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
            case DOWN:
                this.sendDistantVectorPackets();
                break;
            case INTERFACES:
            case ROUTES:
                break;
            case SEND:
                System.out.println(args[0]);
                System.out.println(args[1]);
                System.out.println(args[2]);
                PacketFactory pf = new PacketFactory();
                pf.setIpProtocol(200);
                pf.setSrcIp("17.34.51.68");
                pf.setDstIp("255.255.128.192");
                pf.setPayload("salam bar hame :D");

                this.links.get(0).sendFrame(pf.getPacketData(), pf.getPacketSize());

                break;
            case QUIT:
                this.runProgram = false;
                break;
        }
    }
    private void handelNewPacket(byte[] frameData, int frameSize) {
        PacketFactory packetParser = new PacketFactory(frameData, frameSize);

//        Runnable r = ipProtocolHandler.get(packetParser.getIpProtocol());
//        if (r != null)
//            r.run(packetParser);
//        else {
//            System.out.println("Invalid IP Protocol Number.");
//            System.out.println("Dropping Packet.");
//        }

        if (packetParser.getIpProtocol() == 17) {
            // update forwarding table
            // send distantVector Packets if necessary,
            this.handelDistantVectorPacket(packetParser);
        }

        if (isSelfInterface(packetParser.getDstIp())) {
            // TODO: pass to upper layers
            System.out.println("Packet received and passed to upper layer.");
        } else {
            System.out.println("re sending packet.");

            Link link = forwardingTable.getLink(packetParser.getDstIp());
            link.sendFrame(packetParser.getPacketData(), packetParser.getPacketSize());
        }
    }

    private void createLinksFromInputFile(LnxParser input) throws SocketException {
        while (input.nextLine()) {
            Link link = new Link(input.getLinkHost(), input.getLinkPort(), input.getLinkIpSrc(), input.getLinkIpDst());
            this.links.add(link);
        }
    }

    private boolean isSelfInterface(String interfaceIp) {
        for(Link link : links) {
            if (link.getLinkInterface().equals(interfaceIp))
                return true;
        }
        return false;
    }

    private void sendDistantVectorPackets() {
        for (Link link : links) {
            PacketFactory pf = new PacketFactory();
            pf.setIpProtocol(17);
            pf.setSrcIp(link.getLinkInterface());
            pf.setDstIp(link.getTargetInterface());
            pf.setPayload(forwardingTable.getRoutingPacketPayload(link.getTargetInterface()));

            link.sendFrame(pf.getPacketData(), pf.getPacketSize());
        }
    }

    private void handelDistantVectorPacket(PacketFactory packetParser) {
        boolean isChanged = this.forwardingTable.updateRoutingTable(
                packetParser.getDstIp(),
                packetParser.getPayloadInByteArray(),
                packetParser.getPayloadSize(),
                this.links
        );
        if (isChanged)
            this.sendDistantVectorPackets();
    }
}
