package game.model;

public enum CardColor {
    RED("Red"),
    YELLOW("Yellow"),
    GREEN("Green"),
    BLUE("Blue"),
    BLACK("Black");

    private final String displayName;

    CardColor(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
