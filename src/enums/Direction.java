package enums;

public enum Direction {
    UP('A'),
    DOWN('B'),
    RIGHT('C'),
    LEFT('D');

    private final char code;

    Direction(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }
}
