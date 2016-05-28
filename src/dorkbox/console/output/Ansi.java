/**
 * Copyright (C) 2009, Progress Software Corporation and/or its
 * subsidiaries or affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a asValue of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Copyright 2016 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.console.output;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * Provides a fluent API for generating ANSI escape sequences.
 *
 * See: https://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * @author Dorkbox, LLC
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Ansi {

    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final char FIRST_ESC_CHAR = 27;
    private static final char SECOND_ESC_CHAR = '[';

    private static int installed;

    /**
     * Override System.err and System.out with an ANSI capable {@link java.io.PrintStream}.
     */
    public static synchronized
    void systemInstall() {
        installed++;
        if (installed == 1) {
            System.setOut(AnsiConsole.out);
            System.setErr(AnsiConsole.err);
        }
    }

    /**
     * un-does a previous {@link #systemInstall()}.
     *
     * If {@link #systemInstall()} was called multiple times, then {@link #systemUninstall()} must be called the same number of
     * times before it is uninstalled.
     */
    public static synchronized
    void systemUninstall() {
        installed--;
        if (installed == 0) {
            if (AnsiConsole.out != AnsiConsole.system_out) {
                AnsiConsole.out.close();
            }

            if (AnsiConsole.err != AnsiConsole.system_err) {
                AnsiConsole.err.close();
            }

            System.setOut(AnsiConsole.system_out);
            System.setErr(AnsiConsole.system_err);
        }
    }

    public static final String DISABLE = Ansi.class.getName() + ".disable";

    private static Callable<Boolean> detector = new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
            return !Boolean.getBoolean(DISABLE);
        }
    };

    public static void setDetector(final Callable<Boolean> detector) {
        if (detector == null) {
            throw new IllegalArgumentException();
        }
        Ansi.detector = detector;
    }

    public static boolean isDetected() {
        try {
            return detector.call();
        }
        catch (Exception e) {
            return true;
        }
    }

    private static final InheritableThreadLocal<Boolean> holder = new InheritableThreadLocal<Boolean>()
    {
        @Override
        protected Boolean initialValue() {
            return isDetected();
        }
    };

    public static void setEnabled(final boolean flag) {
        holder.set(flag);
    }

    public static boolean isEnabled() {
        return holder.get();
    }

    private static class NoAnsi extends Ansi
    {
        public
        NoAnsi() {
            super();
        }

        public
        NoAnsi(final StringBuilder builder) {
            super(builder);
        }

        public
        NoAnsi(final int size) {
            super(size);
        }

        @Override
        public Ansi fg(Color color) {
            return this;
        }

        @Override
        public Ansi bg(Color color) {
            return this;
        }

        @Override
        public Ansi fgBright(Color color) {
            return this;
        }

        @Override
        public Ansi bgBright(Color color) {
            return this;
        }

        @Override
        public Ansi fgBrightDefault() { return this; }

        @Override
        public Ansi bgBrightDefault() { return this; }

        @Override
        public Ansi fgDefault() { return this; }

        @Override
        public Ansi bgDefault() { return this; }


        @Override
        public Ansi a(Attribute attribute) {
            return this;
        }

        @Override
        public Ansi cursor(int x, int y) {
            return this;
        }

        @Override
        public Ansi cursorToColumn(int x) {
            return this;
        }

        @Override
        public Ansi cursorUp(int y) {
            return this;
        }

        @Override
        public Ansi cursorRight(int x) {
            return this;
        }

        @Override
        public Ansi cursorDown(int y) {
            return this;
        }

        @Override
        public Ansi cursorLeft(int x) {
            return this;
        }

        @Override
        public Ansi cursorDownLine() {
            return this;
        }

        @Override
        public Ansi cursorDownLine(final int n) {
            return this;
        }

        @Override
        public Ansi cursorUpLine() {
            return this;
        }

        @Override
        public Ansi cursorUpLine(final int n) {
            return this;
        }

        @Override
        public Ansi eraseScreen() {
            return this;
        }

        @Override
        public Ansi eraseScreen(Erase kind) {
            return this;
        }

        @Override
        public Ansi eraseLine() {
            return this;
        }

        @Override
        public Ansi eraseLine(Erase kind) {
            return this;
        }

        @Override
        public Ansi scrollUp(int rows) {
            return this;
        }

        @Override
        public Ansi scrollDown(int rows) {
            return this;
        }

        @Override
        public Ansi saveCursorPosition() {
            return this;
        }

        @Override
        public Ansi restoreCursorPosition() {
            return this;
        }

        @Override
        public Ansi reset() {
            return this;
        }
    }

    /**
     * Creates a new Ansi object and resets the output to the default.
     */
    public static Ansi ansi() {
        if (isEnabled()) {
            return new Ansi();
        }
        else {
            return new NoAnsi();
        }
    }

    /**
     * Creates a new Ansi object from the specified StringBuilder. This does NOT reset the output back to default.
     */
    public static
    Ansi ansi(StringBuilder builder) {
        if (isEnabled()) {
            return new Ansi(builder);
        }
        else {
            return new NoAnsi(builder);
        }
    }

    /**
     * Creates a new Ansi object of the specified length and reset the output back to default.
     */
    public static
    Ansi ansi(int size) {

        if (isEnabled()) {
            return new Ansi(size);
        }
        else {
            return new NoAnsi(size);
        }
    }




    private final StringBuilder builder;
    private final ArrayList<Integer> attributeOptions = new ArrayList<Integer>(5);

    /**
     * Creates a new Ansi object and resets the output to the default.
     */
    public
    Ansi() {
        this(new StringBuilder());
        reset(); // always reset a NEW Ansi object (w/ no parent)
    }

    /**
     * Creates a new Ansi object from the parent. This does NOT reset the output back to default.
     */
    public
    Ansi(Ansi parent) {
        this(new StringBuilder(parent.builder));
        attributeOptions.addAll(parent.attributeOptions);
    }

    /**
     * Creates a new Ansi object of the specified length and reset the output back to default.
     */
    public
    Ansi(int size) {
        this(new StringBuilder(size));
        reset(); // always reset a NEW Ansi object (w/ no parent)
    }

    /**
     * Creates a new Ansi object from the specified StringBuilder. This does NOT reset the output back to default.
     */
    public
    Ansi(StringBuilder builder) {
        this.builder = builder;
        // don't know if there is a parent or not, so we don't reset()
    }

    public
    Ansi fg(Color color) {
        attributeOptions.add(color.fg());
        return this;
    }

    public
    Ansi fgDefault() {
        attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_FG);
        return this;
    }

    public
    Ansi fgBlack() {
        return this.fg(Color.BLACK);
    }

    public
    Ansi fgBlue() {
        return this.fg(Color.BLUE);
    }

    public
    Ansi fgCyan() {
        return this.fg(Color.CYAN);
    }

    public
    Ansi fgGreen() {
        return this.fg(Color.GREEN);
    }

    public
    Ansi fgMagenta() {
        return this.fg(Color.MAGENTA);
    }

    public
    Ansi fgRed() {
        return this.fg(Color.RED);
    }

    public
    Ansi fgYellow() {
        return this.fg(Color.YELLOW);
    }

    public
    Ansi fgWhite() {
        return this.fg(Color.WHITE);
    }

    public
    Ansi fgBright(Color color) {
        attributeOptions.add(color.fgBright());
        return this;
    }

    public
    Ansi fgBrightDefault() {
        attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_FG);
        attributeOptions.add(AnsiOutputStream.ATTRIBUTE_BOLD);
        return this;
    }

    public
    Ansi fgBrightBlack() {
        return this.fgBright(Color.BLACK);
    }

    public
    Ansi fgBrightBlue() {
        return this.fgBright(Color.BLUE);
    }

    public
    Ansi fgBrightCyan() {
        return this.fgBright(Color.CYAN);
    }

    public
    Ansi fgBrightGreen() {
        return this.fgBright(Color.GREEN);
    }

    public
    Ansi fgBrightMagenta() {
        return this.fgBright(Color.MAGENTA);
    }

    public
    Ansi fgBrightRed() {
        return this.fgBright(Color.RED);
    }

    public
    Ansi fgBrightYellow() {
        return this.fgBright(Color.YELLOW);
    }

    public
    Ansi fgBrightWhite() {
        return this.fgBright(Color.WHITE);
    }


    public
    Ansi bg(Color color) {
        attributeOptions.add(color.bg());
        return this;
    }

    public
    Ansi bgDefault() {
        attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_BG);
        return this;
    }

    public
    Ansi bgBlack() {
        return this.bg(Color.BLACK);
    }

    public
    Ansi bgBlue() {
        return this.bg(Color.BLUE);
    }

    public
    Ansi bgCyan() {
        return this.bg(Color.CYAN);
    }

    public
    Ansi bgGreen() {
        return this.bg(Color.GREEN);
    }

    public
    Ansi bgMagenta() {
        return this.bg(Color.MAGENTA);
    }

    public
    Ansi bgRed() {
        return this.bg(Color.RED);
    }

    public
    Ansi bgYellow() {
        return this.bg(Color.YELLOW);
    }

    public
    Ansi bgWhite() {
        return this.bg(Color.WHITE);
    }

    public
    Ansi bgBright(Color color) {
        attributeOptions.add(color.bgBright());
        return this;
    }

    public
    Ansi bgBrightDefault() {
        attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_BG);
        attributeOptions.add(AnsiOutputStream.ATTRIBUTE_BOLD);
        return this;
    }


    public
    Ansi bgBrightBlack() {
        return this.bgBright(Color.BLACK);
    }

    public
    Ansi bgBrightBlue() {
        return this.bgBright(Color.BLUE);
    }

    public
    Ansi bgBrightCyan() {
        return this.bgBright(Color.CYAN);
    }

    public
    Ansi bgBrightGreen() {
        return this.bgBright(Color.GREEN);
    }

    public
    Ansi bgBrightMagenta() {
        return this.bgBright(Color.MAGENTA);
    }

    public
    Ansi bgBrightRed() {
        return this.bgBright(Color.RED);
    }

    public
    Ansi bgBrightYellow() {
        return this.bgBright(Color.YELLOW);
    }

    public
    Ansi bgBrightWhite() {
        return this.bgBright(Color.WHITE);
    }

    public
    Ansi a(Attribute attribute) {
        attributeOptions.add(attribute.value());
        return this;
    }

    /**
     * @param x is 1 indexed (the very first value is 1, not 0)
     * @param y is 1 indexed (the very first value is 1, not 0)
     */
    public
    Ansi cursor(final int x, final int y) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_POS, x, y);
    }

    /**
     * @param x is 1 indexed (the very first value is 1, not 0)
     */
    public
    Ansi cursorToColumn(final int x) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_TO_COL, x);
    }

    public
    Ansi cursorUp(final int y) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP, y);
    }

    public
    Ansi cursorDown(final int y) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN, y);
    }

    public
    Ansi cursorRight(final int x) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_RIGHT, x);
    }

    public
    Ansi cursorLeft(final int x) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_LEFT, x);
    }

    public
    Ansi cursorDownLine() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN_LINE);
    }

    public
    Ansi cursorDownLine(final int n) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN_LINE, n);
    }

    public
    Ansi cursorUpLine() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP_LINE);
    }

    public
    Ansi cursorUpLine(final int n) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP_LINE, n);
    }

    public
    Ansi eraseScreen() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_SCREEN, Erase.ALL.value());
    }

    public
    Ansi eraseScreen(final Erase kind) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_SCREEN, kind.value());
    }

    public
    Ansi eraseLine() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_LINE);
    }

    public
    Ansi eraseLine(final Erase kind) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_LINE, kind.value());
    }

    public
    Ansi scrollUp(final int rows) {
        return appendEscapeSequence(AnsiOutputStream.PAGE_SCROLL_UP, rows);
    }

    public
    Ansi scrollDown(final int rows) {
        return appendEscapeSequence(AnsiOutputStream.PAGE_SCROLL_DOWN, rows);
    }

    public
    Ansi saveCursorPosition() {
        return appendEscapeSequence(AnsiOutputStream.SAVE_CURSOR_POS);
    }

    public
    Ansi restoreCursorPosition() {
        return appendEscapeSequence(AnsiOutputStream.RESTORE_CURSOR_POS);
    }

    public
    Ansi reset() {
        return a(Attribute.RESET);
    }

    public
    Ansi bold() {
        return a(Attribute.BOLD);
    }

    public
    Ansi boldOff() {
        return a(Attribute.BOLD_OFF);
    }

    public
    Ansi faint() {
        return a(Attribute.FAINT);
    }

    public
    Ansi faintOff() {
        return a(Attribute.FAINT_OFF);
    }

    public
    Ansi italic() {
        return a(Attribute.ITALIC);
    }

    public
    Ansi italicOff() {
        return a(Attribute.ITALIC_OFF);
    }

    public
    Ansi underline() {
        return a(Attribute.UNDERLINE);
    }

    public
    Ansi underlineDouble() {
        return a(Attribute.UNDERLINE_DOUBLE);
    }

    public
    Ansi underlineOff() {
        return a(Attribute.UNDERLINE_OFF);
    }

    public
    Ansi blinkSlow() {
        return a(Attribute.BLINK_SLOW);
    }

    public
    Ansi blinkFast() {
        return a(Attribute.BLINK_FAST);
    }

    public
    Ansi blinkOff() {
        return a(Attribute.BLINK_OFF);
    }

    public
    Ansi negative() {
        return a(Attribute.NEGATIVE);
    }

    public
    Ansi negativeOff() {
        return a(Attribute.NEGATIVE_OFF);
    }

    public
    Ansi conceal() {
        return a(Attribute.CONCEAL);
    }

    public
    Ansi concealOff() {
        return a(Attribute.CONCEAL_OFF);
    }

    public
    Ansi strikethrough() {
        return a(Attribute.STRIKETHROUGH);
    }

    public
    Ansi strikethroughOff() {
        return a(Attribute.STRIKETHROUGH_OFF);
    }

    public
    Ansi a(final String value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final boolean value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final char value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final char[] value, final int offset, final int len) {
        flushAttributes();
        builder.append(value, offset, len);
        return this;
    }

    public
    Ansi a(final char[] value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final CharSequence value, final int start, final int end) {
        flushAttributes();
        builder.append(value, start, end);
        return this;
    }

    public
    Ansi a(final CharSequence value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final double value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final float value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final int value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final long value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final Object value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final StringBuilder value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi a(final StringBuffer value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public
    Ansi newline() {
        flushAttributes();
        builder.append(NEW_LINE);
        return this;
    }

    public
    Ansi format(final String pattern, final Object... args) {
        flushAttributes();
        builder.append(String.format(pattern, args));
        return this;
    }

    /**
     * Uses the {@link AnsiRenderer} to generate the ANSI escape sequences for the supplied text.
     */
    public
    Ansi render(final String text) {
        a(AnsiRenderer.render(text));
        return this;
    }

    /**
     * String formats and renders the supplied arguments.
     * Uses the {@link AnsiRenderer} to generate the ANSI escape sequences.
     */
    public
    Ansi render(final String text, final Object... args) {
        a(String.format(AnsiRenderer.render(text), args));
        return this;
    }

    @Override
    public String toString() {
        flushAttributes();
        return builder.toString();
    }

    ///////////////////////////////////////////////////////////////////
    // Private Helper Methods
    ///////////////////////////////////////////////////////////////////

    private
    Ansi appendCommandSequence(final char command) {
        flushAttributes();
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        builder.append(command);
        return this;
    }

    private
    Ansi appendEscapeSequence(final char command) {
        flushAttributes();
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        builder.append(command);
        return this;
    }

    private
    Ansi appendEscapeSequence(final char command, final int option) {
        flushAttributes();
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        builder.append(option);
        builder.append(command);
        return this;
    }

    private
    Ansi appendEscapeSequence(final char command, final Object... options) {
        flushAttributes();
        return _appendEscapeSequence(command, options);
    }

    private
    void flushAttributes() {
        if( attributeOptions.isEmpty() ) {
            return;
        }
        if (attributeOptions.size() == 1 && attributeOptions.get(0) == 0) {
            builder.append(FIRST_ESC_CHAR);
            builder.append(SECOND_ESC_CHAR);
            builder.append(AnsiOutputStream.TEXT_ATTRIBUTE);
        } else {
            _appendEscapeSequence(AnsiOutputStream.TEXT_ATTRIBUTE, attributeOptions.toArray());
        }
        attributeOptions.clear();
    }

    private
    Ansi _appendEscapeSequence(final char command, final Object... options) {
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        int size = options.length;
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                builder.append(';');
            }
            if (options[i] != null) {
                builder.append(options[i]);
            }
        }
        builder.append(command);
        return this;
    }
}
