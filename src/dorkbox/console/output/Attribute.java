package dorkbox.console.output;

import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_BLINK_FAST;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_BLINK_OFF;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_BLINK_SLOW;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_BOLD;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_CONCEAL_OFF;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_CONCEAL_ON;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_FAINT;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_ITALIC;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_ITALIC_OFF;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_NEGATIVE_OFF;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_NEGATIVE_ON;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_NORMAL;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_RESET;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_STRIKETHROUGH_OFF;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_STRIKETHROUGH_ON;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_UNDERLINE;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_UNDERLINE_DOUBLE;
import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_UNDERLINE_OFF;

public
enum Attribute {
    RESET(ATTRIBUTE_RESET, "RESET"),

    BOLD(ATTRIBUTE_BOLD, "BOLD"),
    BOLD_OFF(ATTRIBUTE_NORMAL, "BOLD_OFF"),

    FAINT(ATTRIBUTE_FAINT, "FAINT"),
    FAINT_OFF(ATTRIBUTE_NORMAL, "FAINT_OFF"),

    ITALIC(ATTRIBUTE_ITALIC, "ITALIC"),
    ITALIC_OFF(ATTRIBUTE_ITALIC_OFF, "ITALIC_OFF"),

    UNDERLINE(ATTRIBUTE_UNDERLINE, "UNDERLINE"),
    UNDERLINE_DOUBLE(ATTRIBUTE_UNDERLINE_DOUBLE, "UNDERLINE_DOUBLE"),
    UNDERLINE_OFF(ATTRIBUTE_UNDERLINE_OFF, "UNDERLINE_OFF"),

    BLINK_SLOW(ATTRIBUTE_BLINK_SLOW, "BLINK_SLOW"),
    BLINK_FAST(ATTRIBUTE_BLINK_FAST, "BLINK_FAST"),
    BLINK_OFF(ATTRIBUTE_BLINK_OFF, "BLINK_OFF"),

    NEGATIVE(ATTRIBUTE_NEGATIVE_ON, "NEGATIVE"),
    NEGATIVE_OFF(ATTRIBUTE_NEGATIVE_OFF, "NEGATIVE_OFF"),

    CONCEAL(ATTRIBUTE_CONCEAL_ON, "CONCEAL"),
    CONCEAL_OFF(ATTRIBUTE_CONCEAL_OFF, "CONCEAL_OFF"),

    STRIKETHROUGH(ATTRIBUTE_STRIKETHROUGH_ON, "STRIKETHROUGH"),
    STRIKETHROUGH_OFF(ATTRIBUTE_STRIKETHROUGH_OFF, "STRIKETHROUGH_OFF"),
    ;

    private final int value;
    private final String name;

    Attribute(final int index, final String name) {
        this.value = index;
        this.name = name;

        // register code names with the ANSI renderer
        AnsiRenderer.reg(this, name);
    }

    @Override
    public
    String toString() {
        return name;
    }

    public
    int value() {
        return value;
    }
}
