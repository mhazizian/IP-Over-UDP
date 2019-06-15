package ipOverUdp;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

enum CommandName {
    INTERFACES, ROUTES, DOWN, UP, SEND, QUIT;
}

public class CommandHandler {
    private BufferedReader br;

    public CommandHandler() {
        InputStreamReader r = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(r);
    }

    public boolean hasNewCommand() {
        try {
            return br.ready();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Pair<CommandName, String[]> getCommand() throws IOException {
        if (!this.hasNewCommand())
            return null;
        String line = br.readLine();

        int argsDividerIndex = ordinalIndexOf(line, " ", 0);
        String command = line.substring(0, argsDividerIndex);

        switch (command) {
            case "interfaces":
                return new Pair<>(CommandName.INTERFACES, null);
            case "routes":
                return new Pair<>(CommandName.ROUTES, null);
            case "q":
                return new Pair<>(CommandName.QUIT, null);

            case "down":
                String[] argsDown = new String[1];
                argsDown[0] = line.substring(argsDividerIndex + 1);
                return new Pair<>(CommandName.DOWN, argsDown);

            case "up":
                String[] argsUp = new String[1];
                argsUp[0] = line.substring(argsDividerIndex + 1);
                return new Pair<>(CommandName.UP, argsUp);

            case "send":
                String[] argsSend = new String[3];
                argsSend[0] = line.substring(ordinalIndexOf(line, " ", 0) + 1, ordinalIndexOf(line, " ", 1));
                argsSend[1] = line.substring(ordinalIndexOf(line, " ", 1) + 1, ordinalIndexOf(line, " ", 2));
                argsSend[2] = line.substring(ordinalIndexOf(line, " ", 2) + 1);
                return new Pair<>(CommandName.SEND, argsSend);
        }
        return null;
    }

    public static int ordinalIndexOf(String str, String substr, int n) {
        int pos = -1;
        do {
            pos = str.indexOf(substr, pos + 1);
        } while (n-- > 0 && pos != -1);
        return pos;
    }
}
