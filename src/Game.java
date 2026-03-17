import enums.Difficulty;
import enums.Direction;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {
    private Difficulty difficulty;
    private Field[][] map;
    private int rows, cols;
    private int menuCursorPos, gameXCursorPos, gameYCursorPos;
    private int minesRemaining;

    private volatile boolean isGameRunning;
    private Thread timerThread;
    private final AtomicInteger secondsElapsed = new AtomicInteger(0);
    private final int COL_WIDTH = 2;

    void run() {
        //IO.print("\033[?25l"); // hide cursor
        IO.println("\033[38;5;40mMinesweeper\033[37m\n");
        initCursorPos();
        TerminalUtils.enableRawMode();
        playMenu();
        playGame();
    }

    private void initCursorPos() {
        menuCursorPos = 0;
        gameXCursorPos = 0;
        gameYCursorPos = 0;
    }

    private void playMenu() {
        IO.println("Select a difficulty:\r");
        IO.println("[■] Beginner\r");
        IO.println("[ ] Intermediate\r");
        IO.println("[ ] Expert\r");

        while (true) {
            int key = InputHandler.readKey();

            switch (key) {
                case 'q', -1, 3 -> {
                    IO.print("\033[?25h");  // show cursor
                    TerminalUtils.disableRawMode();
                    System.exit(0);
                }

                case 'w', 'A', 'k' -> {
                    if (menuCursorPos >= 1) {
                        menuCursorPos--;
                        renderDifficulty(menuCursorPos);
                    }
                }

                case 's', 'B', 'j' -> {
                    if (menuCursorPos <= 1) {
                        menuCursorPos++;
                        renderDifficulty(menuCursorPos);
                    }
                }

                case '\n', '\r' -> {
                    switch (menuCursorPos) {
                        case 0 -> {
                            difficulty = Difficulty.BEGINNER;
                        }
                        case 1 -> {
                            difficulty = Difficulty.INTERMEDIATE;
                        }
                        case 2 -> {
                            difficulty = Difficulty.EXPERT;
                        }
                    }
                    minesRemaining = difficulty.getMines();
                }
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

        IO.print("\033[2K");
        IO.print("\033[1G");
        IO.println(selection == 0 ? "[■] Beginner" : "[ ] Beginner\r");
        IO.print("\033[2K");
        IO.print("\033[1G");
        IO.println(selection == 1 ? "[■] Intermediate" : "[ ] Intermediate\r");
        IO.print("\033[2K");
        IO.print("\033[1G");
        IO.println(selection == 2 ? "[■] Expert" : "[ ] Expert\r");
        IO.print("\033[1G");
    }

    private void playGame() {
        isGameRunning = true;
        InputHandler.moveCursor(Direction.DOWN, 2);
        startTimer();
        initCursorPos();
        setupMap();

        // ■ □ ⚑

        while (true) {
            int key = InputHandler.readKey();

            switch (key) {
                case 'q', -1, 3 -> {
                    InputHandler.moveCursor(Direction.DOWN, rows + 1 - gameYCursorPos);
                    IO.print("\033[1G");    // move cursor to first column
                    IO.print("\033[?25h");  // show cursor
                    TerminalUtils.disableRawMode();
                    System.exit(0);
                }

                case 'm' -> moveToMenu();

                case 'r' -> resetGame();

                case 'w', 'A', 'k' -> {
                    if (gameYCursorPos >= 1) {
                        clearCursor();
                        int steps = findNextHiddenField(Direction.UP);
                        if (steps != 0) {
                            InputHandler.moveCursor(Direction.UP, steps);
                            gameYCursorPos -= steps;
                        }
                        renderCursor();
                    }
                }

                case 's', 'B', 'j' -> {
                    if (gameYCursorPos < rows - 1) {
                        clearCursor();
                        int steps = findNextHiddenField(Direction.DOWN);
                        if (steps != 0) {
                            InputHandler.moveCursor(Direction.DOWN, steps);
                            gameYCursorPos += steps;
                        }
                        renderCursor();
                    }
                }

                case 'a', 'D', 'h' -> {
                    if (gameXCursorPos >= 1) {
                        clearCursor();
                        int steps = findNextHiddenField(Direction.LEFT);
                        if (steps != 0) {
                            InputHandler.moveCursor(Direction.LEFT, steps * 2);
                            gameXCursorPos -= steps;
                        }
                        renderCursor();
                    }
                }

                case 'd', 'C', 'l' -> {
                    if (gameXCursorPos < cols - 1) {
                        clearCursor();
                        int steps = findNextHiddenField(Direction.RIGHT);
                        if (steps != 0) {
                            InputHandler.moveCursor(Direction.RIGHT, steps * COL_WIDTH);
                            gameXCursorPos += steps;
                        }
                        renderCursor();
                    }
                }

                case 'f' -> {
                    placeFlag();
                    renderMinesRemaining();
                }

                case '\r', '\n' -> unveilField();
            }
        }
    }

    private void startTimer() {
        renderTimer();

        timerThread = new Thread(() -> {
            while (isGameRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
                secondsElapsed.incrementAndGet();
                renderTimer();
            }
        });
        timerThread.setDaemon(true); // exits automatically when main thread exits
        timerThread.start();
    }

    private void resetTimer() {
        isGameRunning = false;
        if (timerThread != null) {
            timerThread.interrupt();
        }

        secondsElapsed.set(0);
        isGameRunning = true;
    }

    private void renderTimer() {
        int TOP_UI_ROWS = 2;
        IO.print("\033[s"); // save cursor pos
        InputHandler.moveCursor(Direction.UP, TOP_UI_ROWS + gameYCursorPos);
        InputHandler.moveCursor(Direction.RIGHT, COL_WIDTH * (cols - gameXCursorPos) - 1);
        System.out.printf("%03d", secondsElapsed.get());
        IO.print("\033[u"); // resume to saved cursor pos
    }

    private void setupMap() {
        initMap();

        int mines = difficulty.getMines();
        while (mines > 0) {
            boolean isMinePlaced = placeRandomMine();
            if (isMinePlaced) mines--;
        }

        IO.println(minesRemaining + "\r");

        IO.print("┌");
        for (int h = 0; h < cols * 2 + 3; h++) {
            IO.print("─");
        }
        IO.print("┐");

        IO.println("\r");

        int mineCounter = 0;
        // Calculate minesNearby
        for (int i = 0; i < rows; i++) {
            IO.print("│  ");
            for (int j = 0; j < cols; j++) {

                if (i == 0 && j == 0) {
                    IO.print("■");
                } else {
                    IO.print("□");
                }

                if (j < cols - 1) {
                    IO.print(" ");
                }

                if (map[i][j].isMine()) {
                    continue;
                }

                for (int k = -1; k < 2; k++) {
                    for (int l = -1; l < 2; l++) {
                        boolean isItself = (k == 0 && l == 0);
                        boolean isOutOfBoundRow = (i + k < 0 || i + k > rows - 1);
                        boolean isOutOfBoundCol = (j + l < 0 || j + l > cols - 1);
                        if (isItself || isOutOfBoundRow || isOutOfBoundCol) continue;
                        if (map[i + k][j + l].isMine()) {
                            mineCounter++;
                        }
                    }
                }

                map[i][j].setAdjacentMines(mineCounter);

                mineCounter = 0;
            }
            IO.print("  │");
            IO.println("\r");
        }

        IO.print("└");
        for (int h = 0; h < cols * 2 + 3; h++) {
            IO.print("─");
        }
        IO.print("┘");

        IO.println("\r");

        InputHandler.moveCursor(Direction.UP, rows + 1);
        InputHandler.moveCursor(Direction.RIGHT, 3);
    }

    private void initMap() {
        rows = difficulty.getRows();
        cols = difficulty.getCols();
        map = new Field[rows][cols];

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

    private void renderMinesRemaining() {
        int TOP_UI_ROWS = 2;
        IO.print("\033[s"); // save cursor pos
        InputHandler.moveCursor(Direction.UP, TOP_UI_ROWS + gameYCursorPos);

        if (minesRemaining >= 9 || minesRemaining <= -9 || minesRemaining == 0) {
            IO.print("\033[1G");
            IO.print("\033[2C");
            IO.print("\033[1K");
            IO.print("\033[1G");
        } else IO.print("\033[1G");

        IO.print(minesRemaining);
        IO.print("\033[u"); // resume to saved cursor pos
    }

    private int findNextHiddenField(Direction dir) {
        int currPosY = gameYCursorPos;
        int currPosX = gameXCursorPos;
        int steps = 1;

        return switch (dir) {
            case UP -> {
                while (currPosY >= 1 && map[currPosY - 1][currPosX].isVisible()) {
                    currPosY--;
                    steps++;
                }
                if(gameYCursorPos - steps < 0) yield 0;
                yield steps;
            }
            case DOWN -> {
                while (currPosY < rows - 1 && map[currPosY + 1][currPosX].isVisible()) {
                    currPosY++;
                    steps++;
                }
                if(gameYCursorPos + steps > rows - 1) yield 0;
                yield steps;
            }
            case LEFT -> {
                while (currPosX >= 1 && map[currPosY][currPosX - 1].isVisible()) {
                    currPosX--;
                    steps++;
                }
                if(gameXCursorPos - steps < 0) yield 0;
                yield steps;
            }
            case RIGHT -> {
                while (currPosX < cols - 1 && map[currPosY][currPosX + 1].isVisible()) {
                    currPosX++;
                    steps++;
                }
                if(gameXCursorPos + steps > cols - 1) yield 0;
                yield steps;
            }
        };
    }

    private void clearCursor() {
        Field f = map[gameYCursorPos][gameXCursorPos];

        if (f.isVisible()) return;

        if (f.isFlagged()) {
            IO.print("\033[38;5;210m⚑\033[37m");
        } else {
            IO.print("□");
        }
        InputHandler.moveCursor(Direction.LEFT, 1);
    }

    private void renderCursor() {
        Field f = map[gameYCursorPos][gameXCursorPos];

        if (f.isVisible()) return;

        if (f.isFlagged()) {
            IO.print("\033[37m⚑");
        } else {
            IO.print("■");
        }
        InputHandler.moveCursor(Direction.LEFT, 1);
    }

    private void placeFlag() {
        Field f = map[gameYCursorPos][gameXCursorPos];

        if (f.isVisible()) return;

        if (!f.isFlagged()) {
            f.setFlagged(true);
            IO.print("⚑");
            InputHandler.moveCursor(Direction.LEFT, 1);
            minesRemaining--;
        } else {
            f.setFlagged(false);
            renderCursor();
            minesRemaining++;
        }
    }

    private void unveilField() {
        Field f = map[gameYCursorPos][gameXCursorPos];

        if (f.isFlagged()) return;

        f.setVisible(true);

        int minesNearby = f.getAdjacentMines();

        if (f.isMine()) {
            gameOver(gameYCursorPos, gameXCursorPos);
        } else {
            IO.print(getColoredMineCount(minesNearby));
        }

        InputHandler.moveCursor(Direction.LEFT, 1);
    }

    private void moveToMenu() {
        int TOP_UI_ROWS = 9;

        InputHandler.moveCursor(Direction.UP, TOP_UI_ROWS + gameYCursorPos);

        IO.print("\033[1G");    // move cursor to first column
        IO.print("\033[0J");
        TerminalUtils.disableRawMode();
        resetTimer();
        run();
    }

    private void gameOver(int mineY, int mineX) {
        isGameRunning = false;

        // Move to 0/0
        if(gameYCursorPos != 0 && gameXCursorPos != 0) {
            InputHandler.moveCursor(Direction.UP, gameYCursorPos);
            InputHandler.moveCursor(Direction.LEFT, gameXCursorPos * COL_WIDTH);
            gameYCursorPos = 0;
            gameXCursorPos = 0;
        }

        // show all mines
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++) {
                Field f = map[row][col];
                if(f.isMine()) {
                    if(row == mineY && col == mineX) {
                        IO.print("\033[48;5;160m*\033[49m"); // red background
                    } else {
                        IO.print("*");
                    }
                    InputHandler.moveCursor(Direction.LEFT, 1);
                }
                InputHandler.moveCursor(Direction.RIGHT, 2);
                gameXCursorPos++;
            }
            InputHandler.moveCursor(Direction.DOWN, 1);
            InputHandler.moveCursor(Direction.LEFT, cols * COL_WIDTH);
            gameYCursorPos++;
        }
        IO.print("\033[1G");    // move cursor to first column
        InputHandler.moveCursor(Direction.UP, 1);


        int TOP_UI_ROWS = 2;
        while(true) {
            int key = InputHandler.readKey();

            switch (key) {
                case 'r' -> {
                    InputHandler.moveCursor(Direction.UP, rows - 1 + TOP_UI_ROWS);
                    resetTimer();
                    playGame();
                }
                case 'm' -> moveToMenu();
                case 'q', 3 -> {
                    InputHandler.moveCursor(Direction.DOWN, rows + 2 - gameYCursorPos);
                    IO.print("\033[1G");    // move cursor to first column
                    IO.print("\033[?25h");  // show cursor
                    TerminalUtils.disableRawMode();
                    System.exit(0);
                }
            }
        }
    }

    private void resetGame() {
        int LEFT_UI_COLS = 3, TOP_UI_ROWS = 2;
        InputHandler.moveCursor(Direction.LEFT, gameXCursorPos * COL_WIDTH + LEFT_UI_COLS);
        InputHandler.moveCursor(Direction.UP, gameYCursorPos + TOP_UI_ROWS);

        resetTimer();
        playGame();
    }

    private String getColoredMineCount(int minesNearby) {
        return switch (minesNearby) {
            case 1 -> "\033[38;5;33m1\033[37m";
            case 2 -> "\033[38;5;40m2\033[37m";
            case 3 -> "\033[38;5;196m3\033[37m";
            case 4 -> "\033[38;5;226m4\033[37m";
            case 5 -> "\033[38;5;208m5\033[37m";
            case 6 -> "\033[38;5;51m6\033[37m";
            case 7 -> "\033[38;5;200m7\033[37m";
            case 8 -> "\033[38;5;8m8\033[37m";
            default -> " ";
        };
    }
}
