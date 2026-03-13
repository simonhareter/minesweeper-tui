import enums.Direction;

import java.io.IOException;

public class InputHandler {
    public static int readKey() {
        try {
            int key = System.in.read();
            if(key != '\033') {
                return key;
            }

            int key2 = System.in.read();
            if(key2 != '[') {
                return key2;
            }
            return System.in.read();
        } catch (IOException e) {
            System.err.println("IO error while reading input");
            return -1;
        }
    }

    public static void handleKeyGame(int key) {
        switch(key) {
            case 'q':
            case -1:
                System.out.print("\033[3B");    // move cursor 3 rows down
                System.out.print("\033[1G");    // move cursor to first column
                System.out.print("\033[?25h");  // show cursor
                TerminalUtils.disableRawMode();
                System.exit(0);
                break;
            case 'w':
            case 'A':
                moveCursor(Direction.UP, 1);
                break;
            case 's':
            case 'B':
                moveCursor(Direction.DOWN, 1);
                break;
            case 'a':
            case 'D':
                moveCursor(Direction.LEFT, 1);
                break;
            case 'd':
            case 'C':
                moveCursor(Direction.RIGHT, 1);
                break;
        }
    }

    public static void moveCursor(Direction dir, int n) {
        System.out.printf("\033[%d%c", n,  dir.getCode());
    }
}
