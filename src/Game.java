import enums.Difficulty;
import enums.Direction;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import static helpers.InputHandler.*;
import static helpers.TerminalUtils.*;

public class Game {
    private Difficulty difficulty;
    private Field[][] map;
    private final String[] gameMenu = {"Play", "Rankings", "Quit"};
    private final String[] difficultyMenu = {"Beginner", "Intermediate", "Expert"};
    private int rows, cols, totalFields, mines;
    private int gameColCursorPos;
    private int gameRowCursorPos;
    private int minesRemaining, fieldsRevealed;
    private boolean isFirstMove;

    private volatile boolean isGameRunning;
    private Thread timerThread;
    private final AtomicInteger secondsElapsed = new AtomicInteger(0);

    private final int GAME_TOP_UI_ROWS = 2;
    private final int COL_WIDTH = 2;
    private final int GAME_BOTTOM_UI_ROWS = 2;
    private final RankingDB rankingDB;
    private final String[] logo = {
            "        _                                                   ",
            "  /\\/\\ (_)_ __   ___  _____      _____  ___ _ __   ___ _ __  ",
            " /    \\| | '_ \\ / _ \\/ __\\ \\ /\\ / / _ \\/ _ \\ '_ \\ / _ \\ '__|",
            "/ /\\/\\ \\ | | | |  __/\\__ \\\\ V  V /  __/  __/ |_) |  __/ |   ",
            "\\/    \\/_|_| |_|\\___||___/ \\_/\\_/ \\___|\\___| .__/ \\___|_|   ",
            "                                           |_|              "
    };

    public Game() {
        rankingDB = new RankingDB();
    }

    void run() {
        IO.print("\033[?25l"); // hide cursor
        renderLogo();
        enableRawMode();
        rankingDB.createTables();
        playMenu();
    }

    private void renderLogo() {
        for (String s : logo) {
            IO.println(s);
        }
    }

    private void playMenu() {
        int menuSelectedIdx = 0;

        renderMenu(gameMenu, menuSelectedIdx, true);

        boolean waiting = true;

        while (waiting) {
            int key = readKey();

            switch (key) {
                case 'q', -1, 3 -> quitGame();
                case '\n', '\r' -> {
                    switch (menuSelectedIdx) {
                        case 0 -> {
                            selectDifficulty(false);
                            playGame();
                        }
                        case 1 -> selectDifficulty(true);
                        case 2 -> quitGame();
                    }
                    waiting = false;
                }
                case 'w', 'A', 'k' -> {
                    if (menuSelectedIdx >= 1) {
                        menuSelectedIdx--;
                        renderMenu(gameMenu, menuSelectedIdx, false);
                    }
                }
                case 's', 'B', 'j' -> {
                    if (menuSelectedIdx <= 1) {
                        menuSelectedIdx++;
                        renderMenu(gameMenu, menuSelectedIdx, false);
                    }
                }
            }
        }
    }

    private void selectDifficulty(boolean isRanking) {
        int difficultySelectedIdx = 0;

        renderMenu(difficultyMenu, difficultySelectedIdx, false);

        boolean waiting = true;

        while (waiting) {
            int key = readKey();

            switch (key) {
                case 'q', -1, 3 -> quitGame();
                case 'm' -> {
                    IO.print("\033[3F");
                    IO.print("\033[0J");
                    playMenu();
                    waiting = false;
                }
                case 'w', 'A', 'k' -> {
                    if (difficultySelectedIdx >= 1) {
                        difficultySelectedIdx--;
                        renderMenu(difficultyMenu, difficultySelectedIdx, false);
                    }
                }
                case 's', 'B', 'j' -> {
                    if (difficultySelectedIdx <= 1) {
                        difficultySelectedIdx++;
                        renderMenu(difficultyMenu, difficultySelectedIdx, false);
                    }
                }
                case '\n', '\r' -> {
                    if(isRanking) {
                        String tableName = switch (difficultySelectedIdx) {
                            case 1 -> "intermediate";
                            case 2 -> "expert";
                            default -> "beginner";
                        };
                        renderRankings(tableName);
                        return;
                    }

                    difficulty = switch (difficultySelectedIdx) {
                        case 0 -> Difficulty.BEGINNER;
                        case 1 -> Difficulty.INTERMEDIATE;
                        case 2 -> Difficulty.EXPERT;
                    };
                    waiting = false;
                }
            }
        }
    }

    private void renderMenu(String[] items, int selectedIdx, boolean skipRowChange) {
        StringBuilder sb = new StringBuilder();

        if(!skipRowChange) sb.append("\033[3F");

        int menuLength = items.length;

        for (int idx = 0; idx < menuLength; idx++) {
            String menuOption = items[idx];
            sb.append("\033[2K");
            sb.append("\033[1G");
            if (idx == selectedIdx) {
                sb.append("\u001B[38;5;223m❯ ")
                        .append(menuOption)
                        .append("\u001B[37m\n");
            } else {
                sb.append("  ")
                        .append(menuOption)
                        .append("\n");
            }
        }
        IO.print(sb.toString());
    }

    private void renderRankings(String tableName) {
        int DEFAULT_UI_ROWS = 6;
        IO.print("\033[3F");
        IO.print("\033[0J");
        IO.println("Top 10 Players: " + tableName);
        rankingDB.printTable(tableName);
        IO.println("Total Entries: " + rankingDB.getRows(tableName));
        IO.print("\033[1G");

        boolean waiting = true;

        while (waiting) {
            int key = readKey();

            switch (key) {
                case 'q', -1, 3 -> quitGame();
                case 'm' -> {
                    moveTermCursor(Direction.UP, DEFAULT_UI_ROWS + 10);
                    IO.print("\033[0J");
                    playMenu();
                    waiting = false;
                }
            }
        }
    }

    private void playGame() {
        IO.print("\033[3F");
        IO.print("\033[0J");

        isGameRunning = true;
        isFirstMove = true;
        fieldsRevealed = 0;
        initCursorPos();
        setupMap();
        startTimer();
        renderSmiley("\uD83D\uDE42");
        // ■ □ ⚑

        while (isGameRunning) {
            renderGameCursorCoordinates();

            if (fieldsRevealed == totalFields - mines) gameWon();

            int key = readKey();

            Direction dir = getDirection(key);
            if (dir != null) handleMovement(dir);

            switch (key) {
                case 'q', -1, 3 -> {
                    // move TermCursor down to not override game prints
                    moveTermCursor(Direction.DOWN, rows + 3 - gameRowCursorPos);
                    quitGame();
                }
                case 'm' -> {
                    moveToMenu();
                }
                case 'r' -> resetGame();
                case 'f' -> {
                    placeFlag();
                    renderMinesRemaining();
                }
                case '\r', '\n' -> revealField();
            }
        }
    }

    private void setupMap() {
        initMap();

        int minesToPlace = mines;
        while (minesToPlace > 0) {
            boolean isMinePlaced = placeRandomMine();
            if (isMinePlaced) minesToPlace--;
        }

        StringBuilder sb = new StringBuilder();

        sb.append(minesRemaining).append("\n");

        // top border
        sb.append("┌");
        sb.repeat("─", Math.max(0, cols * 2 + 3));
        sb.append("┐\n");

        // calculate minesNearby + render grid
        for (int i = 0; i < rows; i++) {
            sb.append("│  ");

            for (int j = 0; j < cols; j++) {

                if (i == 0 && j == 0) {
                    sb.append("■");
                } else {
                    sb.append("□");
                }

                if (j < cols - 1) {
                    sb.append(" ");
                }

                if (map[i][j].isMine()) {
                    continue;
                }

                int mineCounter = 0;

                for (int k = -1; k <= 1; k++) {
                    for (int l = -1; l <= 1; l++) {

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
            }

            sb.append("  │\n");
        }

        // bottom border
        sb.append("└");
        sb.repeat("─", Math.max(0, cols * 2 + 3));
        sb.append("┘\n");

        IO.print(sb.toString());

        moveTermCursor(Direction.UP, rows + 1);
        moveTermCursor(Direction.RIGHT, 3);
    }

    private void initMap() {
        rows = difficulty.getRows();
        cols = difficulty.getCols();
        totalFields = rows * cols;
        mines = difficulty.getMines();
        minesRemaining = mines;
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

    private void initCursorPos() {
        gameColCursorPos = 0;
        gameRowCursorPos = 0;
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
        if (hasAnyValidMove(dir)) moveToNextHiddenField(dir);
        else {
            // find the next possible field depending on directional input
            findNearestPossibleField(dir);
        }
    }

    private boolean hasAnyValidMove(Direction dir) {
        return stepsToNextHiddenField(dir) > 0;
    }

    private void findNearestPossibleField(Direction dir) {
        int bestScore = Integer.MAX_VALUE;
        int nearestRow = -1;
        int nearestCol = -1;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (map[row][col].isVisible()) continue;
                if (row == gameRowCursorPos && col == gameColCursorPos) continue;
                int distance = Math.abs(row - gameRowCursorPos) + Math.abs(col - gameColCursorPos);
                int penalty = calcPenalty(dir, row, col);
                int score = distance + penalty;
                if (score < bestScore) {
                    bestScore = score;
                    nearestRow = row;
                    nearestCol = col;
                }
            }
        }

        if (nearestRow != -1) {
            clearCursor();
            moveCursorTo(nearestRow, nearestCol);
            renderCursor();
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
        IO.print("\033[1G");
        rankingDB.closeConnection();
        disableRawMode();
        IO.print("\033[?25h");  // show cursor
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
                if (gameRowCursorPos - steps < 0) yield 0;
                yield steps;
            }
            case DOWN -> {
                while (currPosY < rows - 1 && map[currPosY + 1][currPosX].isVisible()) {
                    currPosY++;
                    steps++;
                }
                if (gameRowCursorPos + steps > rows - 1) yield 0;
                yield steps;
            }
            case LEFT -> {
                while (currPosX >= 1 && map[currPosY][currPosX - 1].isVisible()) {
                    currPosX--;
                    steps++;
                }
                if (gameColCursorPos - steps < 0) yield 0;
                yield steps;
            }
            case RIGHT -> {
                while (currPosX < cols - 1 && map[currPosY][currPosX + 1].isVisible()) {
                    currPosX++;
                    steps++;
                }
                if (gameColCursorPos + steps > cols - 1) yield 0;
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
            case DOWN -> newRow += steps;
            case LEFT -> newCol -= steps;
            case RIGHT -> newCol += steps;
        }

        if (isOutOfBounds(newCol, newRow)) return;

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
        IO.print("\033[0K");
        System.out.printf("%03d", secondsElapsed.get());
        IO.print("\033[u"); // resume to saved cursor pos
    }

    private void renderMinesRemaining() {
        IO.print("\033[s");
        moveTermCursor(Direction.UP, GAME_TOP_UI_ROWS + gameRowCursorPos);

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
        moveTermCursor(Direction.DOWN, rows - gameRowCursorPos + GAME_BOTTOM_UI_ROWS);
        IO.print("\033[1G");
        System.out.printf("Cursor: (%d,%d)", gameRowCursorPos + 1, gameColCursorPos + 1);
        IO.print("\033[u");
    }

    private void renderSmiley(String smiley) {
        IO.print("\033[s");
        moveTermCursor(Direction.UP, GAME_TOP_UI_ROWS + gameRowCursorPos);
        System.out.printf("\033[%dG", cols + 2);
        // 🙂 😎 😢
        IO.print(smiley);
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

        int minesNearby = f.getAdjacentMines();

        if (minesNearby == 0) {
            revealEmptyRegion(gameRowCursorPos, gameColCursorPos);
            handleMovement(Direction.LEFT);
            renderCursor();
        } else {
            IO.print(getColoredMineCount(minesNearby));
            moveTermCursor(Direction.LEFT, 1);
        }

        if (!f.isVisible()) {
            f.setVisible(true);
            fieldsRevealed++;

            if (fieldsRevealed >= (totalFields - mines)) gameWon();
        }
    }

    private void revealEmptyRegion(int row, int col) {
        if (isOutOfBounds(row, col)) return;

        Field currField = map[row][col];

        if (currField.isVisible() || currField.isMine()) return;

        currField.setVisible(true);
        fieldsRevealed++;

        if (fieldsRevealed >= (totalFields - mines)) gameWon();

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
        } while (map[row][col].isMine());

        map[row][col].setMine(true);
        map[gameRowCursorPos][gameColCursorPos].setMine(false);

        updateNeighbours(row, col, 1);
        updateNeighbours(gameRowCursorPos, gameColCursorPos, -1);
    }

    private void updateNeighbours(int row, int col, int adjacentMineChange) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (isOutOfBounds(row + i, col + j)) continue;
                if (i == 0 && j == 0) continue;

                int adjacentMines = map[row + i][col + j].getAdjacentMines();
                map[row + i][col + j].setAdjacentMines(adjacentMines + adjacentMineChange);
            }
        }
    }

    private void moveToMenu() {
        isGameRunning = false;

        moveTermCursor(Direction.UP, gameRowCursorPos + GAME_TOP_UI_ROWS);

        IO.print("\033[1G");
        IO.print("\033[0J");
        resetTimer();
        playMenu();
    }

    private void gameWon() {
        isGameRunning = false;
        timerThread.interrupt();
        try {
            timerThread.join();
        } catch (InterruptedException ignored) {}

        renderSmiley("\uD83D\uDE0E");

        IO.print("\033[s");
        saveGameResult();
        IO.print("\033[u");

        boolean waiting = true;

        while (waiting) {
            int key = readKey();

            switch (key) {
                case 'r' -> {
                    waiting = false;
                    resetGame();
                }
                case 'm' -> {
                    waiting = false;
                    moveToMenu();
                }
                case 'q', 3 -> {
                    waiting = false;
                    moveTermCursor(Direction.DOWN, rows + 3 - gameRowCursorPos);
                    quitGame();
                }
            }
        }
    }

    private void saveGameResult() {
        moveTermCursor(Direction.DOWN, rows - gameRowCursorPos + GAME_BOTTOM_UI_ROWS);
        IO.print("\033[1G");
        IO.print("\033[0K");

        Scanner in = new Scanner(System.in);

        IO.println("Save your score? (y/n)");

        boolean waiting = true;

        while(waiting) {
            int key = readKey();

            if (key == 'y' || key == 'Y') {
                waiting = false;
            }
            if (key == 'n' || key == 'N') {
                return;
            }
        }

        disableRawMode();
        IO.println("Enter your name:");
        IO.print("> ");

        String name = in .nextLine();

        rankingDB.saveGameResult("beginner", name, secondsElapsed.get());
        enableRawMode();
        IO.print("> Result saved!");
    }

    private void gameOver(int mineY, int mineX) {
        isGameRunning = false;
        renderSmiley("\uD83D\uDE22");

        // Move to 0/0
        if (gameRowCursorPos > 0) moveTermCursor(Direction.UP, gameRowCursorPos);
        if (gameColCursorPos > 0) moveTermCursor(Direction.LEFT, gameColCursorPos * COL_WIDTH);
        gameRowCursorPos = 0;
        gameColCursorPos = 0;

        // show all mines
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Field f = map[row][col];
                if (f.isMine()) {
                    if (row == mineY && col == mineX) {
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
            if (row < rows - 1) {
                moveTermCursor(Direction.DOWN, 1);
                moveTermCursor(Direction.LEFT, (cols - 1) * COL_WIDTH);
                gameRowCursorPos++;
                gameColCursorPos = 0;
            }
        }

        boolean waiting = true;

        while (waiting) {
            int key = readKey();

            switch (key) {
                case 'r' -> {
                    waiting = false;
                    resetGame();
                }
                case 'm' -> {
                    waiting = false;
                    moveToMenu();
                }
                case 'q', 3 -> {
                    waiting = false;
                    moveTermCursor(Direction.DOWN, rows + 3 - gameRowCursorPos);
                    quitGame();
                }
            }
        }
    }

    private void resetGame() {
        IO.print("\033[3B");
        IO.print("\033[0J");
        resetFlagCounter();
        resetCursorPosition();
        resetTimer();
        playGame();
    }

    private void resetFlagCounter() {
        IO.print("\033[s");
        moveTermCursor(Direction.UP, GAME_TOP_UI_ROWS + gameRowCursorPos);
        IO.print("\033[1G");
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
