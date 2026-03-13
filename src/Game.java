import enums.Difficulty;

public class Game {
    private Difficulty difficulty;
    private Field[][] map;
    private int rows, cols;
    private int menuCursorPos;

    public Game() {
        this.menuCursorPos = 0;
    }

    void run() {
        IO.println("\033[38;5;41mMinesweeper\033[37m\n");
        TerminalUtils.enableRawMode();
        setDifficulty();
        setupMap();
        playGame();
    }

    private void setDifficulty() {
        System.out.print("\033[?25l"); // hide cursor

        IO.println("Select a difficulty:\r");
        IO.println("[■] Beginner\r");
        IO.println("[ ] Intermediate\r");
        IO.println("[ ] Expert\r");

        while (true) {
            int key = InputHandler.readKey();

            switch (key) {
                case 'q':
                case -1:
                    System.out.print("\033[?25h");  // show cursor
                    TerminalUtils.disableRawMode();
                    System.exit(0);
                    break;
                case 'w':
                case 'A':
                case 'k':
                    if (menuCursorPos >= 1) {
                        menuCursorPos--;
                        renderDifficulty(menuCursorPos);
                    }
                    break;
                case 's':
                case 'B':
                case 'j':
                    if (menuCursorPos <= 1) {
                        menuCursorPos++;
                        renderDifficulty(menuCursorPos);
                    }
                    break;
                case '\n':
                case '\r':
                    switch (menuCursorPos) {
                        case 0:
                            difficulty = Difficulty.BEGINNER;
                            break;
                        case 1:
                            difficulty = Difficulty.INTERMEDIATE;
                            break;
                        case 2:
                            difficulty = Difficulty.EXPERT;
                            break;
                    }
                    break;
            }

            // exit loop after enter press
            if (key == '\n' || key == '\r') {
                IO.println();
                break;
            }
        }
    }

    private void renderDifficulty(int selection) {
        IO.print("\033[3F");

        IO.print("\033[1G");
        IO.print("\033[2K");
        IO.println(selection == 0 ? "[■] Beginner" : "[ ] Beginner");
        IO.print("\033[1G");
        IO.print("\033[2K");
        IO.println(selection == 1 ? "[■] Intermediate" : "[ ] Intermediate");
        IO.print("\033[1G");
        IO.print("\033[2K");
        IO.println(selection == 2 ? "[■] Expert" : "[ ] Expert");
        IO.print("\033[1G");
    }

    private void setupMap() {
        initMap();

        int mines = difficulty.getMines();
        while (mines > 0) {
            boolean isMinePlaced = placeRandomMine();
            if(isMinePlaced) mines--;
        }

        //printBoolField();

        IO.print("┌");
        for(int h = 0; h < rows * 2 + 3; h++) {
            IO.print("─");
        }
        IO.print("┐");

        IO.println("\r");

        int mineCounter = 0;
        // Calculate minesNearby
        for(int i = 0; i < rows; i++) {
            IO.print("│  ");
            for(int j = 0; j < cols; j++) {
                if(map[i][j].isMine()) {
                    IO.print("*");
                    if (j < cols - 1) {
                        IO.print(" ");
                    }
                    continue;
                }

                for(int k = -1; k < 2; k++) {
                    for(int l = -1; l < 2; l++) {
                        if((k == 0 && l == 0) || (i + k < 0 || i + k > rows - 1) || (j + l < 0 || j + l > cols - 1)) continue;
                        if(map[i + k][j + l].isMine()) {
                            mineCounter++;
                        }
                    }
                }

                if(mineCounter != 0) {
                    IO.print(mineCounter);
                } else {
                    IO.print(" ");
                }
                if (j < cols - 1) {
                    IO.print(" ");
                }
                mineCounter = 0;
            }
            IO.print("  │");
            IO.println("\r");
        }

        IO.print("└");
        for(int h = 0; h < rows * 2 + 3; h++) {
            IO.print("─");
        }
        IO.print("┘");

        IO.println("\r");



        System.out.print("\033[?25h");  // show cursor
        TerminalUtils.disableRawMode();
    }

    private void initMap() {
        rows = difficulty.getRows();
        cols = difficulty.getCols();
        map =  new Field[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                map[i][j] = new Field();
            }
        }
    }

    private boolean placeRandomMine() {
        int row = (int) (Math.random() * rows);
        int col = (int) (Math.random() * cols);

        if (!map[row][col].isMine()) {
            map[row][col].setMine(true);
            return true;
        }
        return false;
    }

    private void printBoolField() {
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                IO.print(map[i][j].isMine() + " ");
            }
            IO.println("\r");
        }
    }

    private void playGame() {

    }
}
