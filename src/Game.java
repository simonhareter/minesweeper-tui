import enums.Difficulty;
import enums.Direction;
import java.util.concurrent.atomic.AtomicInteger;
import static helpers.InputHandler.*;
import static helpers.TerminalUtils.*;

public class Game {
    private Difficulty difficulty;
    private Field[][] map;
    private int rows, cols;
    private int menuCursorPos, gameXCursorPos, gameYCursorPos;
    private int minesRemaining;
    private boolean isFirstMove;

    private volatile boolean isGameRunning;
    private Thread timerThread;
    private final AtomicInteger secondsElapsed = new AtomicInteger(0);

    private final int GAME_TOP_UI_ROWS = 2;
    private final int COL_WIDTH = 2;

    void run() {
        IO.print("\033[?25l"); // hide cursor
        IO.println("\033[38;5;40mMinesweeper\033[37m\n");
        initCursorPos();
        enableRawMode();
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
            int key = readKey();

            switch (key) {
                case 'q', -1, 3 -> {
                    IO.print("\033[?25h");  // show cursor
                    disableRawMode();
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
        isFirstMove = true;
        initCursorPos();
        setupMap();
        startTimer();
        // ■ □ ⚑

        while (true) {
            renderGameCursorCoordinates();
            int key = readKey();

            switch (key) {
                case 'q', -1, 3 -> {
                    moveCursor(Direction.DOWN, rows + 3 - gameYCursorPos);
                    IO.print("\033[1G");    // move cursor to first column
                    IO.print("\033[?25h");  // show cursor
                    disableRawMode();
                    System.exit(0);
                }

                case 'm' -> moveToMenu();

                case 'r' -> resetGame();

                case 'w', 'A', 'k' -> {
                    if (gameYCursorPos >= 1) {
                        clearCursor();
                        int steps = findNextHiddenField(Direction.UP);
                        if (steps != 0) {
                            moveCursor(Direction.UP, steps);
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
                            moveCursor(Direction.DOWN, steps);
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
                            moveCursor(Direction.LEFT, steps * 2);
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
                            moveCursor(Direction.RIGHT, steps * COL_WIDTH);
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
        IO.print("\033[s"); // save cursor pos
        moveCursor(Direction.UP, GAME_TOP_UI_ROWS + gameYCursorPos);
        moveCursor(Direction.RIGHT, COL_WIDTH * (cols - gameXCursorPos) - 1);
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

        moveCursor(Direction.UP, rows + 1);
        moveCursor(Direction.RIGHT, 3);
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
        IO.print("\033[s");
        moveCursor(Direction.UP, GAME_TOP_UI_ROWS  + gameYCursorPos);

        if (minesRemaining >= 9 || minesRemaining <= -9 || minesRemaining == 0) {
            IO.print("\033[1G");
            IO.print("\033[2C");
            IO.print("\033[1K");
            IO.print("\033[1G");
        } else IO.print("\033[1G");

        IO.print(minesRemaining);
        IO.print("\033[u");
    }

    private void renderGameCursorCoordinates() {
        IO.print("\033[s");
        int GAME_BOTTOM_UI_ROWS = 2;
        moveCursor(Direction.DOWN, rows - gameYCursorPos + GAME_BOTTOM_UI_ROWS);
        IO.print("\033[1G");
        System.out.printf("Cursor: (%d,%d)", gameYCursorPos + 1, gameXCursorPos + 1);
        IO.print("\033[u");
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
        moveCursor(Direction.LEFT, 1);
    }

    private void renderCursor() {
        Field f = map[gameYCursorPos][gameXCursorPos];

        if (f.isVisible()) return;

        if (f.isFlagged()) {
            IO.print("\033[37m⚑");
        } else {
            IO.print("■");
        }
        moveCursor(Direction.LEFT, 1);
    }

    private void placeFlag() {
        Field f = map[gameYCursorPos][gameXCursorPos];

        if (f.isVisible()) return;

        if (!f.isFlagged()) {
            f.setFlagged(true);
            IO.print("⚑");
            moveCursor(Direction.LEFT, 1);
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

        if (isFirstMove) {
            if (f.isMine()) {
                swapMine();
            }
            isFirstMove = false;
        }

        if (f.isMine()) {
            gameOver(gameYCursorPos, gameXCursorPos);
            return;
        }

        if(minesNearby == 0) {
            unveilEmptyRegion();
            return;
        }

        IO.print(getColoredMineCount(minesNearby));
        moveCursor(Direction.LEFT, 1);
    }

    private void unveilEmptyRegion() {
        IO.print(getColoredMineCount(0));
        moveCursor(Direction.LEFT, 1);

        // need to design an algorithm that finds all adjacent empty fields till each direction encounters a field non-zero.

    }

    private void swapMine() {
        int row, col;

        do {
            row = (int) (Math.random() * rows);
            col = (int) (Math.random() * cols);
        } while(map[row][col].isMine());

        map[row][col].setMine(true);
        map[gameYCursorPos][gameXCursorPos].setMine(false);
    }

    private void moveToMenu() {
        int MENU_TOP_UI_ROWS = 9;
        moveCursor(Direction.UP, MENU_TOP_UI_ROWS + gameYCursorPos);

        IO.print("\033[1G");    // move cursor to first column
        IO.print("\033[0J");
        disableRawMode();
        resetTimer();
        run();
    }

    private void gameOver(int mineY, int mineX) {
        isGameRunning = false;

        // Move to 0/0
        if(gameYCursorPos > 0) moveCursor(Direction.UP, gameYCursorPos);
        if(gameXCursorPos > 0 ) moveCursor(Direction.LEFT, gameXCursorPos * COL_WIDTH);
        gameYCursorPos = 0;
        gameXCursorPos = 0;


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
                    moveCursor(Direction.LEFT, 1);
                }
                if (col < cols - 1) {
                    moveCursor(Direction.RIGHT, 2);
                    gameXCursorPos++;
                }
            }
            if(row < rows - 1) {
                moveCursor(Direction.DOWN, 1);
                moveCursor(Direction.LEFT, (cols - 1) * COL_WIDTH);
                gameYCursorPos++;
                gameXCursorPos = 0;
            }
        }

        while(true) {
            int key = readKey();

            switch (key) {
                case 'r' -> {
                    resetGame();
                }
                case 'm' -> moveToMenu();
                case 'q', 3 -> {
                    moveCursor(Direction.DOWN, rows + 3 - gameYCursorPos);
                    IO.print("\033[1G");    // move cursor to first column
                    IO.print("\033[?25h");  // show cursor
                    disableRawMode();
                    System.exit(0);
                }
            }
        }
    }

    private void resetGame() {
        int GAME_LEFT_UI_COLS = 3;
        moveCursor(Direction.LEFT, gameXCursorPos * COL_WIDTH + GAME_LEFT_UI_COLS);
        moveCursor(Direction.UP, gameYCursorPos + GAME_TOP_UI_ROWS);

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
