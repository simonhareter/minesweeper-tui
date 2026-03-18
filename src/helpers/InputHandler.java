package helpers;

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

    public static void moveCursor(Direction dir, int n) {
        System.out.printf("\033[%d%c", n,  dir.getCode());
    }
}
