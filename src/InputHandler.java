import java.io.IOException;

public class InputHandler {
    public static int readKey() {
        int key = -1;
        try {
            key = System.in.read();
        } catch (IOException e) {
            System.err.println("IO error while reading input");
        }
        return key;
    }

    public static void handleKey(int key) {
        if (key == -1 || key == 'q') {
            TerminalUtils.disableRawMode();
            System.exit(0);
        }
        IO.println("key = " + (char) key + "\r");
    }
}
