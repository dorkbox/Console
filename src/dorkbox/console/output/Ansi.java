/*
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
 *
 *
 * Copyright (C) 2009 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.console.output;

import static dorkbox.console.output.AnsiOutputStream.ATTRIBUTE_RESET;

import java.util.ArrayList;

import dorkbox.console.Console;

/**
 * Provides a fluent API for generating ANSI escape sequences and providing access to streams that support it.
 * <p>
 * See: https://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * @author dorkbox, llc
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@SuppressWarnings("unused")
public
class Ansi {
    static {
        // make SURE that our console in/out/err are correctly setup BEFORE accessing methods in this class
        Console.getVersion();
    }

    private static final String NEW_LINE = System.getProperty("line.separator");

    private final StringBuilder builder;
    private final ArrayList<Integer> attributeOptions = new ArrayList<Integer>(8);

    /**
     * Creates a new Ansi object
     */
    public static
    Ansi ansi() {
        return new Ansi();
    }

    /**
     * Creates a new Ansi object from the specified StringBuilder
     */
    public static
    Ansi ansi(StringBuilder builder) {
        return new Ansi(builder);
    }

    /**
     * Creates a new Ansi object of the specified length
     */
    public static
    Ansi ansi(int size) {
        return new Ansi(size);
    }

    /**
     * Creates a new Ansi object from the specified parent
     */
    public static
    Ansi ansi(Ansi ansi) {
        return new Ansi(ansi);
    }

    /**
     * Creates a new Ansi object
     */
    public
    Ansi() {
        this(new StringBuilder());
    }

    /**
     * Creates a new Ansi object from the parent.
     */
    public
    Ansi(Ansi parent) {
        this(new StringBuilder(parent.builder));
        attributeOptions.addAll(parent.attributeOptions);
    }

    /**
     * Creates a new Ansi object of the specified length
     */
    public
    Ansi(int size) {
        this(new StringBuilder(size));
        reset(); // always reset a NEW Ansi object (w/ no parent)
    }

    /**
     * Creates a new Ansi object from the specified StringBuilder
     */
    public
    Ansi(StringBuilder builder) {
        this.builder = builder;
    }

    /**
     * Sets the foreground color of the ANSI output. BRIGHT_* colors are a brighter version of that color. DEFAULT is the color from
     * the beginning before any other color was applied.
     *
     * @param color foreground color to set
     */
    public
    Ansi fg(Color color) {
        if (color.isNormal()) {
            if (color != Color.DEFAULT) {
                attributeOptions.add(color.fg());
            }
            else {
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_FG);
            }
        }
        else {
            if (color != Color.BRIGHT_DEFAULT) {
                attributeOptions.add(color.fgBright());
            }
            else {
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_FG);
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_BOLD);
            }
        }

        return this;
    }

    /**
     * Sets the background color of the ANSI output. BRIGHT_* colors are a brighter version of that color. DEFAULT is the color from
     * the beginning before any other color was applied.
     *
     * @param color background color to set
     */
    public
    Ansi bg(Color color) {
        if (color.isNormal()) {
            if (color != Color.DEFAULT) {
                attributeOptions.add(color.bg());
            }
            else {
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_BG);
            }
        }
        else {
            if (color != Color.BRIGHT_DEFAULT) {
                attributeOptions.add(color.bgBright());
            }
            else {
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_BG);
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_BOLD);
            }
        }
        return this;
    }

    /**
     * Moves the cursor y (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    public
    Ansi cursorUp(final int y) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP, y);
    }

    /**
     * Moves the cursor y (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    public
    Ansi cursorDown(final int y) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN, y);
    }

    /**
     * Moves the cursor x (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    public
    Ansi cursorRight(final int x) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_FORWARD, x);
    }

    /**
     * Moves the cursor x (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    public
    Ansi cursorLeft(final int x) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_BACK, x);
    }


    /**
     * Moves the cursor 1 cell cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    public
    Ansi cursorUp() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP);
    }

    /**
     * Moves the cursor 1 cell cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    public
    Ansi cursorDown() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN);
    }

    /**
     * Moves the cursor 1 cell cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    public
    Ansi cursorRight() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_FORWARD);
    }

    /**
     * Moves the cursor 1 cell cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    public
    Ansi cursorLeft() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_BACK);
    }

    /**
     * Moves cursor to beginning of the line n (default 1) lines down.
     */
    public
    Ansi cursorDownLine(final int n) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN_LINE, n);
    }

    /**
     * Moves cursor to beginning of the line n (default 1) lines up.
     */
    public
    Ansi cursorUpLine(final int n) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP_LINE, n);
    }


    /**
     * Moves cursor to beginning of the line 1 line down.
     */
    public
    Ansi cursorDownLine() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN_LINE);
    }


    /**
     * Moves cursor to beginning of the line 1 lines up.
     */
    public
    Ansi cursorUpLine() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP_LINE);
    }

    /**
     * Moves the cursor to column n (default 1).
     * @param n is 1 indexed (the very first value is 1, not 0)
     */
    public
    Ansi cursorToColumn(final int n) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_TO_COL, n);
    }

    /**
     * Moves the cursor to column 1. The very first value is 1, not 0.
     */
    public
    Ansi cursorToColumn() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_TO_COL);
    }

    /**
     * Moves the cursor to row x, column y.
     *
     * The values are 1-based, the top left corner.
     *
     * @param x is 1 indexed (the very first value is 1, not 0)
     * @param y is 1 indexed (the very first value is 1, not 0)
     */
    public
    Ansi cursor(final int x, final int y) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_POS, x, y);
    }

    /**
     * Moves the cursor to row 1, column 1 (top left corner).
     */
    public
    Ansi cursor() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_POS);
    }

    /**
     * Clears part of the screen, by default clear everything forwards of the cursor
     *
     * @param kind
     *          - {@link Erase#FORWARD} (or missing), clear from cursor to end of screen.
     *          - {@link Erase#BACKWARD}, clear from cursor to beginning of the screen.
     *          - {@link Erase#ALL}, clear entire screen (and moves cursor to upper left on DOS ANSI.SYS).
     */
    public
    Ansi eraseScreen(final Erase kind) {
        if (kind == null) {
            return eraseScreen();
        }

        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_SCREEN, kind.value());
    }

    /**
     * Clears everything forwards of the cursor
     */
    public
    Ansi eraseScreen() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_SCREEN, Erase.ALL.value());
    }

    /**
     * Erases part of the line.
     *
     * @param kind
     *      - {@link Erase#FORWARD} (or missing), clear from cursor to the end of the line.
     *      - {@link Erase#BACKWARD}, clear from cursor to beginning of the line.
     *      - {@link Erase#ALL}, clear entire line. Cursor position does not change.
     */
    public
    Ansi eraseLine(final Erase kind) {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_LINE, kind.value());
    }

    /**
     * Erases from cursor to the end of the line.
     */
    public
    Ansi eraseLine() {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_LINE);
    }



    /**
     * Scroll whole page up by n (default 1) lines. New lines are added at the bottom.
     */
    public
    Ansi scrollUp(final int n) {
        return appendEscapeSequence(AnsiOutputStream.SCROLL_UP, n);
    }

    /**
     * Scroll whole page up by 1 line. New lines are added at the bottom.
     */
    public
    Ansi scrollUp() {
        return appendEscapeSequence(AnsiOutputStream.SCROLL_UP);
    }

    /**
     * Scroll whole page down by n (default 1) lines. New lines are added at the top.
     */
    public
    Ansi scrollDown(final int n) {
        return appendEscapeSequence(AnsiOutputStream.SCROLL_DOWN, n);
    }

    /**
     * Scroll whole page down by 1 line. New lines are added at the top.
     */
    public
    Ansi scrollDown() {
        return appendEscapeSequence(AnsiOutputStream.SCROLL_DOWN);
    }

    /**
     * Saves the cursor position.
     */
    public
    Ansi saveCursorPosition() {
        return appendEscapeSequence(AnsiOutputStream.SAVE_CURSOR_POS);
    }

    /**
     * Restores the cursor position.
     */
    public
    Ansi restoreCursorPosition() {
        return appendEscapeSequence(AnsiOutputStream.RESTORE_CURSOR_POS);
    }

    /**
     * Resets all of the attributes on the ANSI stream
     */
    public
    Ansi reset() {
        return a(Attribute.RESET);
    }

    /**
     * Bold enabled
     */
    public
    Ansi bold() {
        return a(Attribute.BOLD);
    }

    /**
     * Bold disabled
     */
    public
    Ansi boldOff() {
        return a(Attribute.BOLD_OFF);
    }

    /**
     * Faint enabled (not widely supported)
     */
    public
    Ansi faint() {
        return a(Attribute.FAINT);
    }

    /**
     * Faint disabled (not widely supported)
     */
    public
    Ansi faintOff() {
        return a(Attribute.FAINT_OFF);
    }

    /**
     * Italic enabled (not widely supported. Sometimes treated as inverse)
     */
    public
    Ansi italic() {
        return a(Attribute.ITALIC);
    }

    /**
     * Italic disabled (not widely supported. Sometimes treated as inverse)
     */
    public
    Ansi italicOff() {
        return a(Attribute.ITALIC_OFF);
    }

    /**
     *  Underline; Single
     */
    public
    Ansi underline() {
        return a(Attribute.UNDERLINE);
    }

    /**
     *  Underline; Double
     */
    public
    Ansi underlineDouble() {
        return a(Attribute.UNDERLINE_DOUBLE);
    }

    /**
     *  Underline disabled
     */
    public
    Ansi underlineOff() {
        return a(Attribute.UNDERLINE_OFF);
    }

    /**
     * Blink; Slow  less than 150 per minute
     */
    public
    Ansi blinkSlow() {
        return a(Attribute.BLINK_SLOW);
    }

    /**
     * Blink; Rapid 150 per minute or more
     */
    public
    Ansi blinkFast() {
        return a(Attribute.BLINK_FAST);
    }

    /**
     * Blink disabled
     */
    public
    Ansi blinkOff() {
        return a(Attribute.BLINK_OFF);
    }

    /**
     * Negative inverse or reverse; swap foreground and background
     */
    public
    Ansi negative() {
        return a(Attribute.NEGATIVE);
    }

    /**
     * Negative disabled (back to normal)
     */
    public
    Ansi negativeOff() {
        return a(Attribute.NEGATIVE_OFF);
    }

    /**
     * Conceal on
     */
    public
    Ansi conceal() {
        return a(Attribute.CONCEAL);
    }

    /**
     * Conceal off
     */
    public
    Ansi concealOff() {
        return a(Attribute.CONCEAL_OFF);
    }

    /**
     * Strikethrough enabled
     */
    public
    Ansi strikethrough() {
        return a(Attribute.STRIKETHROUGH);
    }

    /**
     * Strikethrough disabled
     */
    public
    Ansi strikethroughOff() {
        return a(Attribute.STRIKETHROUGH_OFF);
    }

    /**
     * Appends an attribute (color/etc)
     *
     * @param attribute the Attribute (color/etc) to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(Attribute attribute) {
        attributeOptions.add(attribute.value());
        return this;
    }

    /**
     * Appends a String
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final String value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a boolean
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final boolean value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a char
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final char value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a char array + offset + length
     *
     * @param valueArray value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final char[] valueArray, final int offset, final int length) {
        flushAttributes();
        builder.append(valueArray, offset, length);
        return this;
    }

    /**
     * Appends a char array
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final char[] value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a CharSequence + start + end
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final CharSequence value, final int start, final int end) {
        flushAttributes();
        builder.append(value, start, end);
        return this;
    }

    /**
     * Appends a CharSequence
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final CharSequence value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a double
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final double value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a float
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final float value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a int
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final int value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a long
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final long value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a Object
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final Object value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a StringBuilder
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final StringBuilder value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a StringBuffer
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    public
    Ansi a(final StringBuffer value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * Appends a new line
     *
     * @return this
     */
    public
    Ansi newline() {
        flushAttributes();
        builder.append(NEW_LINE);
        return this;
    }

    /**
     * Appends a formatted string
     *
     * @param pattern String.format pattern to use
     * @param args arguments to use in the formatted string
     * @return this
     */
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
    private static final char FIRST_ESC_CHAR = 27;
    private static final char SECOND_ESC_CHAR = '[';

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
        if (attributeOptions.size() == 1 && attributeOptions.get(0) == ATTRIBUTE_RESET) {
            builder.append(FIRST_ESC_CHAR);
            builder.append(SECOND_ESC_CHAR);
            builder.append(ATTRIBUTE_RESET);
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
