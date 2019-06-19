package ipOverUdp;

import ipOverUdp.LinkLayer.Link;
import ipOverUdp.LinkLayer.Listener;
import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Node {
    static final int MAX_NODE_NUMBER = 64;

    private Listener listener;
    private CommandHandler commandHandler;
    private ArrayList<Link> links;

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

        this.links = new ArrayList<>();
        while (input.nextLine()) {
            Link link = new Link(input.getLinkHost(), input.getLinkPort(), input.getLinkIpSrc());
            this.links.add(link);
        }

        commandHandler = new CommandHandler();
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
                handelNewFrame(newFrame.getKey(), newFrame.getValue());
        }
    }

    private void handelCommand(CommandName commandType, String[] args) {
        switch (commandType) {
            case UP:
            case DOWN:
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
    private void handelNewFrame(byte[] frameData, int frameSize) {
        System.out.println("given Frame :\n" + Arrays.toString(frameData));
        System.out.println("given size: " + frameSize);

        PacketFactory pf = new PacketFactory(frameData, frameSize);
        System.out.println("IP-protocol is " + pf.getIpProtocol());
        System.out.println(pf.getSrcIp());
        System.out.println(pf.getDstIp());
        System.out.println(pf.getPayload());

    }
}
