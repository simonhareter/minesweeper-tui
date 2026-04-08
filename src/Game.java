import enums.Difficulty;
import enums.Direction;
import java.util.concurrent.atomic.AtomicInteger;
import static helpers.InputHandler.*;
import static helpers.TerminalUtils.*;

public class Game {
    private Difficulty difficulty;
    private Field[][] map;
    private int rows, cols;
    private int menuCursorPos, gameColCursorPos, gameRowCursorPos;
    private int minesRemaining;
    private boolean isFirstMove;

    private volatile boolean isGameRunning;
    private Thread timerThread;
    private final AtomicInteger secondsElapsed = new AtomicInteger(0);

    private final int GAME_TOP_UI_ROWS = 2;
    private final int COL_WIDTH = 2;

    void run() {
        //IO.print("\033[?25l"); // hide cursor
        IO.println("\033[38;5;40mMinesweeper\033[37m\n");
        initCursorPos();
        enableRawMode();
        playMenu();
        playGame();
    }

    private void initCursorPos() {
        menuCursorPos = 0;
        gameColCursorPos = 0;
        gameRowCursorPos = 0;
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
                    quitGame();
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

            Direction dir = getDirection(key);
            if(dir != null) handleMovement(dir);
            
            switch (key) {
                case 'q', -1, 3 -> {
                    // move TermCursor down to not override game prints
                    moveTermCursor(Direction.DOWN, rows + 3 - gameRowCursorPos);
                    quitGame();
                }
                case 'm' -> moveToMenu();
                case 'r' -> resetGame();
                case 'f' -> {
                    placeFlag();
                    renderMinesRemaining();
                }
                case '\r', '\n' -> revealField();
            }
        }
    }
    
    private Direction getDirection(int key) {
        return switch (key) {
            case 'w', 'A', 'k' -> Direction.UP;
            case 's', 'B', 'j' -> Direction.DOWN;
            case 'a', 'D', 'h' -> Direction.LEFT;
            case 'd', 'C', 'l' -> Direction.RIGHT;
            default -> null;
        };
    }

    private void handleMovement(Direction dir) {
        if(hasAnyValidMove()) moveToNextHiddenField(dir);
        else {
            // find the next possible field depending on directional input
            findNearestPossibleField(dir);
        }
    }

    private boolean hasAnyValidMove() {
        return stepsToNextHiddenField(Direction.UP) > 0 ||
                stepsToNextHiddenField(Direction.DOWN) > 0 ||
                stepsToNextHiddenField(Direction.LEFT) > 0 ||
                stepsToNextHiddenField(Direction.RIGHT) > 0;
    }

    private boolean canMove(Direction dir) {
        return switch (dir) {
            case UP -> gameRowCursorPos >= 1;
            case DOWN -> gameRowCursorPos < rows - 1;
            case LEFT -> gameColCursorPos >= 1;
            case RIGHT -> gameColCursorPos < cols - 1;
        };
    }

    private void findNearestPossibleField(Direction dir) {
        int bestScore = Integer.MAX_VALUE;
        int nearestRow = -1;
        int nearestCol = -1;
        for(int row = 0; row < rows; row++) {
            for(int col = 0; col < cols; col++) {
                if(map[row][col].isVisible()) continue;
                int distance = Math.abs(row - gameRowCursorPos) + Math.abs(col - gameColCursorPos);
                int penalty = calcPenalty(dir, row, col);
                int score = distance + penalty;
                if(score < bestScore) {
                    bestScore = score;
                    nearestRow = row;
                    nearestCol = col;
                }
            }
        }

        if(nearestRow != -1) {
            IO.print("\033[s");
            moveTermCursor(Direction.DOWN, 13);
            IO.println(nearestRow + " / " + nearestCol);
            IO.print("\033[u");
            moveCursorTo(nearestRow, nearestCol);
            gameRowCursorPos = nearestRow;
            gameColCursorPos = nearestCol;
        }
    }

    private int calcPenalty(Direction dir, int row, int col) {
        int penalty = 0;

        switch (dir) {
            case DOWN -> {
                if (row <= gameRowCursorPos) penalty += 1000;
            }
            case UP -> {
                if (row >= gameRowCursorPos) penalty += 1000;
            }
            case RIGHT -> {
                if (col <= gameColCursorPos) penalty += 1000;
            }
            case LEFT -> {
                if (col >= gameColCursorPos) penalty += 1000;
            }
        }

        return penalty;
    }

    private void quitGame() {
        IO.print("\033[?25h");  // show cursor
        disableRawMode();
        System.exit(0);
    }

    private void moveToNextHiddenField(Direction dir) {
        clearCursor();
        int steps = stepsToNextHiddenField(dir);
        if (steps != 0) {
            moveCursor(dir, steps);
        }
        renderCursor();
    }

    private int stepsToNextHiddenField(Direction dir) {
        int currPosY = gameRowCursorPos;
        int currPosX = gameColCursorPos;
        int steps = 1;

        return switch (dir) {
            case UP -> {
                while (currPosY >= 1 && map[currPosY - 1][currPosX].isVisible()) {
                    currPosY--;
                    steps++;
                }
                if(gameRowCursorPos - steps < 0) yield 0;
                yield steps;
            }
            case DOWN -> {
                while (currPosY < rows - 1 && map[currPosY + 1][currPosX].isVisible()) {
                    currPosY++;
                    steps++;
                }
                if(gameRowCursorPos + steps > rows - 1) yield 0;
                yield steps;
            }
            case LEFT -> {
                while (currPosX >= 1 && map[currPosY][currPosX - 1].isVisible()) {
                    currPosX--;
                    steps++;
                }
                if(gameColCursorPos - steps < 0) yield 0;
                yield steps;
            }
            case RIGHT -> {
                while (currPosX < cols - 1 && map[currPosY][currPosX + 1].isVisible()) {
                    currPosX++;
                    steps++;
                }
                if(gameColCursorPos + steps > cols - 1) yield 0;
                yield steps;
            }
        };
    }

    private void moveCursor(Direction dir, int steps) {
        // moving game state
        int newRow = gameRowCursorPos;
        int newCol = gameColCursorPos;

        switch (dir) {
            case UP -> newRow -= steps;
            case DOWN ->  newRow += steps;
            case LEFT ->  newCol -= steps;
            case RIGHT -> newCol += steps;
        }

        if(isOutOfBounds(newCol, newRow)) return;

        gameRowCursorPos = newRow;
        gameColCursorPos = newCol;

        // moving visual cursor
        int stepMulti = (dir == Direction.LEFT || dir == Direction.RIGHT) ? COL_WIDTH : 1; // accounting for space horizontally between fields
        moveTermCursor(dir, steps * stepMulti);
    }

    private boolean isOutOfBounds(int row, int col) {
        return row < 0 || row >= rows || col < 0 || col >= cols;
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

    private void renderTimer() {
        IO.print("\033[s"); // save cursor pos
        moveTermCursor(Direction.UP, GAME_TOP_UI_ROWS + gameRowCursorPos);
        moveTermCursor(Direction.RIGHT, COL_WIDTH * (cols - gameColCursorPos) - 2);
        System.out.printf("%04d", secondsElapsed.get());
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

        moveTermCursor(Direction.UP, rows + 1);
        moveTermCursor(Direction.RIGHT, 3);
    }

    private void initMap() {
        rows = difficulty.getRows();
        cols = difficulty.getCols();
        map = new Field[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                map[row][col] = new Field();
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
        moveTermCursor(Direction.UP, GAME_TOP_UI_ROWS  + gameRowCursorPos);

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
        moveTermCursor(Direction.DOWN, rows - gameRowCursorPos + GAME_BOTTOM_UI_ROWS);
        IO.print("\033[1G");
        System.out.printf("Cursor: (%d,%d)", gameRowCursorPos + 1, gameColCursorPos + 1);
        IO.print("\033[u");
    }


    private void clearCursor() {
        Field f = map[gameRowCursorPos][gameColCursorPos];

        if (f.isVisible()) return;

        if (f.isFlagged()) {
            IO.print("\033[38;5;210m⚑\033[37m");
        } else {
            IO.print("□");
        }
        moveTermCursor(Direction.LEFT, 1);
    }

    private void renderCursor() {
        Field f = map[gameRowCursorPos][gameColCursorPos];

        if (f.isVisible()) return;

        if (f.isFlagged()) {
            IO.print("\033[37m⚑");
        } else {
            IO.print("■");
        }
        moveTermCursor(Direction.LEFT, 1);
    }

    private void placeFlag() {
        Field f = map[gameRowCursorPos][gameColCursorPos];

        if (f.isVisible()) return;

        if (!f.isFlagged()) {
            f.setFlagged(true);
            IO.print("⚑");
            moveTermCursor(Direction.LEFT, 1);
            minesRemaining--;
        } else {
            f.setFlagged(false);
            renderCursor();
            minesRemaining++;
        }
    }

    private void revealField() {
        Field f = map[gameRowCursorPos][gameColCursorPos];
        if (f.isFlagged()) return;

        int minesNearby = f.getAdjacentMines();

        if (isFirstMove) {
            if (f.isMine()) {
                swapMine();
            }
            isFirstMove = false;
        }

        if (f.isMine()) {
            gameOver(gameRowCursorPos, gameColCursorPos);
            return;
        }

        if(minesNearby == 0) {
            revealEmptyRegion(gameRowCursorPos, gameColCursorPos);
            return;
        }

        f.setVisible(true);

        IO.print(getColoredMineCount(minesNearby));
        moveTermCursor(Direction.LEFT, 1);
    }

    private void revealEmptyRegion(int row, int col) {
        if(isOutOfBounds(row, col)) return;

        Field currField = map[row][col];

        if(currField.isVisible() || currField.isMine()) return;

        currField.setVisible(true);

        int minesNearby = currField.getAdjacentMines();

        IO.print("\033[s");
        moveCursorTo(row, col);
        IO.print(getColoredMineCount(currField.getAdjacentMines()));
        moveTermCursor(Direction.LEFT, 1);
        IO.print("\033[u");

        if (minesNearby != 0) return;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                revealEmptyRegion(row + i, col + j);
            }
        }
    }

    private void moveCursorTo(int row, int col) {
        // calculate how many fields the cursor has to move to reach destination: map[row][col]
        int moveRow = row - gameRowCursorPos;
        int moveCol = col - gameColCursorPos;

        Direction vertical = moveRow < 0 ? Direction.UP : Direction.DOWN;
        moveTermCursor(vertical, Math.abs(moveRow));

        Direction horizontal = moveCol < 0 ? Direction.LEFT : Direction.RIGHT;
        moveTermCursor(horizontal, Math.abs(moveCol * COL_WIDTH));
    }

    private void swapMine() {
        int row, col;

        do {
            row = (int) (Math.random() * rows);
            col = (int) (Math.random() * cols);
        } while(map[row][col].isMine());

        map[row][col].setMine(true);
        map[gameRowCursorPos][gameColCursorPos].setMine(false);

        updateNeighbours(row, col, 1);
        updateNeighbours(gameRowCursorPos, gameColCursorPos, -1);
    }

    private void updateNeighbours(int row, int col, int adjacentMineChange) {
        for(int i = -1; i <= 1; i++) {
            for(int j = -1; j <= 1; j++) {
                if(isOutOfBounds(row + i, col + j)) continue;
                if(i == 0 && j == 0) continue;

                int adjacentMines = map[row + i][col + j].getAdjacentMines();
                map[row + i][col + j].setAdjacentMines(adjacentMines + adjacentMineChange);
            }
        }
    }

    private void moveToMenu() {
        int MENU_TOP_UI_ROWS = 9;
        moveTermCursor(Direction.UP, MENU_TOP_UI_ROWS + gameRowCursorPos);

        IO.print("\033[1G");    // move cursor to first column
        IO.print("\033[0J");
        disableRawMode();
        resetTimer();
        run();
    }

    private void gameOver(int mineY, int mineX) {
        isGameRunning = false;

        // Move to 0/0
        if(gameRowCursorPos > 0) moveTermCursor(Direction.UP, gameRowCursorPos);
        if(gameColCursorPos > 0 ) moveTermCursor(Direction.LEFT, gameColCursorPos * COL_WIDTH);
        gameRowCursorPos = 0;
        gameColCursorPos = 0;

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
                    moveTermCursor(Direction.LEFT, 1);
                }
                if (col < cols - 1) {
                    moveTermCursor(Direction.RIGHT, 2);
                    gameColCursorPos++;
                }
            }
            if(row < rows - 1) {
                moveTermCursor(Direction.DOWN, 1);
                moveTermCursor(Direction.LEFT, (cols - 1) * COL_WIDTH);
                gameRowCursorPos++;
                gameColCursorPos = 0;
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
                    moveTermCursor(Direction.DOWN, rows + 3 - gameRowCursorPos);
                    IO.print("\033[1G");    // move cursor to first column
                   quitGame();
                }
            }
        }
    }

    private void resetGame() {
        resetFlagCounter();
        resetCursorPosition();
        resetTimer();
        playGame();
    }

    private void resetFlagCounter() {
        IO.print("\033[s");
        moveTermCursor(Direction.UP, GAME_TOP_UI_ROWS  + gameRowCursorPos);
        IO.print("\033[1G");
        minesRemaining = difficulty.getMines();
        IO.print(minesRemaining);
        moveTermCursor(Direction.LEFT, 1);
        IO.print("\033[u");
    }

    private void resetCursorPosition() {
        int GAME_LEFT_UI_COLS = 3;
        moveTermCursor(Direction.LEFT, gameColCursorPos * COL_WIDTH + GAME_LEFT_UI_COLS);
        moveTermCursor(Direction.UP, gameRowCursorPos + GAME_TOP_UI_ROWS);
    }

    private void resetTimer() {
        isGameRunning = false;
        if (timerThread != null) {
            timerThread.interrupt();
        }

        secondsElapsed.set(0);
        isGameRunning = true;
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
