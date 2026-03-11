import java.io.IOException;

public class Game {

    void run() {
        IO.println("Welcome to minesweeper");
        TerminalUtils.enableRawMode();

        while (true) {
            int key = InputHandler.readKey();
            InputHandler.handleKey(key);
        }
    }

    



}
