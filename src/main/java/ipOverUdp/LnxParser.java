package ipOverUdp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class LnxParser {
    private String selfHost;
    private int selfPort;

    private String linkHost;
    private int linkPort;
    // TODO: convert to IP Object?
    private String linkIpSrc;
    private String linkIpDst;

    private Scanner sc;


    LnxParser(String fileName) throws FileNotFoundException {
        File file = new File(fileName);
        sc = new Scanner(file);

        String line = sc.nextLine();
        String[] args = line.split(" ");
        selfHost = args[0];
        selfPort = Integer.parseInt(args[1]);
    }

    boolean nextLine() {
        if (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] args = line.split(" ");

            if (args[0].equals("#"))
                return nextLine();

            linkHost = args[0];
            linkPort = Integer.parseInt(args[1]);
            linkIpSrc = args[2];
            linkIpDst = args[3];

            return true;
        }
        return false;
    }

    public String getSelfHost() {
        return selfHost;
    }

    public int getSelfPort() {
        return selfPort;
    }

    public String getLinkHost() {
        return linkHost;
    }

    public int getLinkPort() {
        return linkPort;
    }

    public String getLinkIpSrc() {
        return linkIpSrc;
    }

    public String getLinkIpDst() {
        return linkIpDst;
    }
}
