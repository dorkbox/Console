package dorkbox.console.output;

/**
 *
 */
class AnsiCode {
    Enum anEnum;
    String formalName;
    boolean isColorForBackground;

    public
    AnsiCode(final Enum anEnum, final String formalName, final boolean isColorForBackground) {
        this.anEnum = anEnum;
        this.formalName = formalName;
        this.isColorForBackground = isColorForBackground;
    }

    public
    boolean isColor() {
        return anEnum instanceof Color;
    }

    public
    boolean isBackgroundColor() {
        return isColorForBackground;
    }

    public
    Color getColor() {
        return (Color) anEnum;
    }

    public
    boolean isAttribute() {
        return anEnum instanceof Attribute;
    }

    public
    Attribute getAttribute() {
        return (Attribute) anEnum;
    }
}
