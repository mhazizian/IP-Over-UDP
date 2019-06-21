package ipOverUdp.protocolNumHandler;

import ipOverUdp.Node;
import ipOverUdp.PacketFactory;

public class TestHandler {
    private static final int protocolNum = 102;

    public static void handler(PacketFactory pf) {
        System.out.println("### we are handling it :D");
        pf.print();
    }

    public static void registerHandler(Node node) {
        try {
            node.registerHandler(TestHandler.class.getMethod("handler", PacketFactory.class), protocolNum);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }
}
