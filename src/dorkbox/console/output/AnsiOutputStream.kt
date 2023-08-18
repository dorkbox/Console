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
 * Copyright (C) 2009, Progress Software Corporation and/or its
 * subsidiaries or affiliates.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a asValue of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.console.output;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * A ANSI output stream extracts ANSI escape codes written to an output stream.
 *
 * For more information about ANSI escape codes, see:
 * http://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * This class just filters out the escape codes so that they are not sent out to the underlying OutputStream.  Subclasses should
 * actually perform the ANSI escape behaviors.
 *
 * @author dorkbox, llc
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author Joris Kuipers
 */
@SuppressWarnings({"NumericCastThatLosesPrecision", "WeakerAccess"})
public
class AnsiOutputStream extends FilterOutputStream {
    private static final Charset CHARSET = Charset.forName("UTF-8");

    static final int BLACK   = 0;
    static final int RED     = 1;
    static final int GREEN   = 2;
    static final int YELLOW  = 3;
    static final int BLUE    = 4;
    static final int MAGENTA = 5;
    static final int CYAN    = 6;
    static final int WHITE   = 7;

    static final char CURSOR_UP        = 'A'; // Moves the cursor n (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
    static final char CURSOR_DOWN      = 'B';
    static final char CURSOR_FORWARD   = 'C';
    static final char CURSOR_BACK      = 'D';

    static final char CURSOR_DOWN_LINE = 'E'; // Moves cursor to beginning of the line n (default 1) lines down.
    static final char CURSOR_UP_LINE   = 'F'; // Moves cursor to beginning of the line n (default 1) lines up.

    static final char CURSOR_TO_COL    = 'G'; // Moves the cursor to column n (default 1).

    static final char CURSOR_POS       = 'H'; // Moves the cursor to row n, column m. The values are 1-based, and default to 1 (top left corner) if omitted.
    static final char CURSOR_POS_ALT   = 'f'; // Moves the cursor to row n, column m. Both default to 1 if omitted. Same as CUP

    static final char CURSOR_ERASE_SCREEN = 'J'; // Clears part of the screen. If n is 0 (or missing), clear from cursor to end of screen. If n is 1, clear from cursor to beginning of the screen. If n is 2, clear entire screen (and moves cursor to upper left on DOS ANSI.SYS).
    static final char CURSOR_ERASE_LINE   = 'K'; // Erases part of the line. If n is zero (or missing), clear from cursor to the end of the line. If n is one, clear from cursor to beginning of the line. If n is two, clear entire line. Cursor position does not change.


    static final char SCROLL_UP           = 'S'; // Scroll whole page up by n (default 1) lines. New lines are added at the bottom. (not ANSI.SYS)
    static final char SCROLL_DOWN         = 'T'; // Scroll whole page down by n (default 1) lines. New lines are added at the top. (not ANSI.SYS)

    static final char SAVE_CURSOR_POS     = 's'; // Saves the cursor position.
    static final char RESTORE_CURSOR_POS  = 'u'; // Restores the cursor position.
    static final char TEXT_ATTRIBUTE      = 'm'; // Sets SGR parameters, including text color. After CSI can be zero or more parameters separated with ;. With no parameters, CSI m is treated as CSI 0 m (reset / normal), which is typical of most of the ANSI escape sequences.

    static final int ATTRIBUTE_RESET             = 0; // Reset / Normal - all attributes off
    static final int ATTRIBUTE_BOLD              = 1; // Intensity: Bold
    static final int ATTRIBUTE_FAINT             = 2; // Intensity; Faint (not widely supported)
    static final int ATTRIBUTE_ITALIC            = 3; // Italic; (on not widely supported. Sometimes treated as inverse)
    static final int ATTRIBUTE_UNDERLINE         = 4; // Underline; Single
    static final int ATTRIBUTE_BLINK_SLOW        = 5; // Blink; Slow  less than 150 per minute
    static final int ATTRIBUTE_BLINK_FAST        = 6; // Blink; Rapid 150 per minute or more
    static final int ATTRIBUTE_NEGATIVE_ON       = 7; // Negative inverse or reverse; swap foreground and background
    static final int ATTRIBUTE_CONCEAL_ON        = 8; // Conceal on
    static final int ATTRIBUTE_STRIKETHROUGH_ON  = 9;  // Crossed-out

    static final int ATTRIBUTE_UNDERLINE_DOUBLE  = 21; // Underline; Double not widely supported
    static final int ATTRIBUTE_NORMAL            = 22; // Intensity; Normal not bold and not faint
    static final int ATTRIBUTE_ITALIC_OFF        = 23; // Not italic
    static final int ATTRIBUTE_UNDERLINE_OFF     = 24; // Underline; None
    static final int ATTRIBUTE_BLINK_OFF         = 25; // Blink; off
    static final int ATTRIBUTE_NEGATIVE_OFF      = 27; // Image; Positive
    static final int ATTRIBUTE_CONCEAL_OFF       = 28; // Reveal conceal off
    static final int ATTRIBUTE_STRIKETHROUGH_OFF = 29; // Not crossed out


    static final int ATTRIBUTE_DEFAULT_FG = 39; //  Default text color (foreground)
    static final int ATTRIBUTE_DEFAULT_BG = 49; //  Default background color


    // for Erase Screen/Line
    static final int ERASE_TO_END = 0;
    static final int ERASE_TO_BEGINNING = 1;
    static final int ERASE_ALL = 2;



    private final static int MAX_ESCAPE_SEQUENCE_LENGTH = 100;

    private static final int LOOKING_FOR_FIRST_ESC_CHAR = 0;
    private static final int LOOKING_FOR_SECOND_ESC_CHAR = 1;

    private static final int LOOKING_FOR_NEXT_ARG = 2;
    private static final int LOOKING_FOR_STR_ARG_END = 3;
    private static final int LOOKING_FOR_INT_ARG_END = 4;
    private static final int LOOKING_FOR_OSC_COMMAND = 5;
    private static final int LOOKING_FOR_OSC_COMMAND_END = 6;
    private static final int LOOKING_FOR_OSC_PARAM = 7;
    private static final int LOOKING_FOR_ST = 8;

    private int state = LOOKING_FOR_FIRST_ESC_CHAR;

    private static final int FIRST_ESC_CHAR = 27;
    private static final int SECOND_ESC_CHAR = '[';
    private static final int SECOND_OSC_CHAR = ']';
    private static final int BEL = 7;
    private static final int SECOND_ST_CHAR = '\\';


    public static final byte[] RESET_CODE = new StringBuilder(3).append((char)FIRST_ESC_CHAR)
                                                                .append((char)SECOND_ESC_CHAR)
                                                                .append(AnsiOutputStream.TEXT_ATTRIBUTE)
                                                                .toString()
                                                                .getBytes(CHARSET);

    public
    AnsiOutputStream(OutputStream os) {
        super(os);
    }


    private byte buffer[] = new byte[MAX_ESCAPE_SEQUENCE_LENGTH];
    private int pos = 0;
    private int startOfValue;
    private final ArrayList<Object> options = new ArrayList<Object>();


    @Override
    public
    void write(int data) throws IOException {
        switch (state) {
            case LOOKING_FOR_FIRST_ESC_CHAR:
                if (data == FIRST_ESC_CHAR) {
                    buffer[pos++] = (byte) data;
                    state = LOOKING_FOR_SECOND_ESC_CHAR;
                }
                else {
                    out.write(data);
                }
                break;

            case LOOKING_FOR_SECOND_ESC_CHAR:
                buffer[pos++] = (byte) data;

                if (data == SECOND_ESC_CHAR) {
                    state = LOOKING_FOR_NEXT_ARG;
                }
                else if (data == SECOND_OSC_CHAR) {
                    state = LOOKING_FOR_OSC_COMMAND;
                }
                else {
                    reset(false);
                }
                break;

            case LOOKING_FOR_NEXT_ARG:
                buffer[pos++] = (byte) data;

                if ('"' == data) {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_STR_ARG_END;
                }
                else if ('0' <= data && data <= '9') {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_INT_ARG_END;
                }
                else if (';' == data) {
                    options.add(null);
                }
                else if ('?' == data) {
                    options.add('?');
                }
                else if ('=' == data) {
                    options.add('=');
                }
                else {
                    reset(processEscapeCommand(options, data));
                }
                break;

            case LOOKING_FOR_INT_ARG_END:
                buffer[pos++] = (byte) data;

                if (!('0' <= data && data <= '9')) {
                    String strValue = new String(buffer, startOfValue, (pos - 1) - startOfValue, CHARSET);
                    Integer value = Integer.valueOf(strValue);
                    options.add(value);
                    if (data == ';') {
                        state = LOOKING_FOR_NEXT_ARG;
                    }
                    else {
                        reset(processEscapeCommand(options, data));
                    }
                }
                break;

            case LOOKING_FOR_STR_ARG_END:
                buffer[pos++] = (byte) data;

                if ('"' != data) {
                    String value = new String(buffer, startOfValue, (pos - 1) - startOfValue, CHARSET);
                    options.add(value);
                    if (data == ';') {
                        state = LOOKING_FOR_NEXT_ARG;
                    }
                    else {
                        reset(processEscapeCommand(options, data));
                    }
                }
                break;

            case LOOKING_FOR_OSC_COMMAND:
                buffer[pos++] = (byte) data;

                if ('0' <= data && data <= '9') {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_OSC_COMMAND_END;
                }
                else {
                    reset(false);
                }
                break;

            case LOOKING_FOR_OSC_COMMAND_END:
                buffer[pos++] = (byte) data;

                if (';' == data) {
                    String strValue = new String(buffer, startOfValue, (pos - 1) - startOfValue, CHARSET);
                    Integer value = Integer.valueOf(strValue);
                    options.add(value);
                    startOfValue = pos;
                    state = LOOKING_FOR_OSC_PARAM;
                }
                else if ('0' <= data && data <= '9') {
                    // already pushed digit to buffer, just keep looking
                }
                else {
                    // oops, did not expect this
                    reset(false);
                }
                break;

            case LOOKING_FOR_OSC_PARAM:
                buffer[pos++] = (byte) data;

                if (BEL == data) {
                    String value = new String(buffer, startOfValue, (pos - 1) - startOfValue, CHARSET);
                    options.add(value);
                    reset(processOperatingSystemCommand(options));
                }
                else if (FIRST_ESC_CHAR == data) {
                    state = LOOKING_FOR_ST;
                }
                else {
                    // just keep looking while adding text
                }
                break;

            case LOOKING_FOR_ST:
                buffer[pos++] = (byte) data;

                if (SECOND_ST_CHAR == data) {
                    String value = new String(buffer, startOfValue, (pos - 2) - startOfValue, CHARSET);
                    options.add(value);
                    reset(processOperatingSystemCommand(options));
                }
                else {
                    state = LOOKING_FOR_OSC_PARAM;
                }
                break;

        }

        // Is it just too long?
        if (pos >= buffer.length) {
            reset(false);
        }
    }

    /**
     * Resets all state to continue with regular parsing
     * @param skipBuffer if current buffer should be skipped or written to out
     * @throws IOException
     */
    private
    void reset(boolean skipBuffer) throws IOException {
        if (!skipBuffer) {
            out.write(buffer, 0, pos);
        }

        pos = 0;
        startOfValue = 0;
        options.clear();
        state = LOOKING_FOR_FIRST_ESC_CHAR;
    }

    /**
     * @return true if the escape command was processed.
     */
    private
    boolean processEscapeCommand(ArrayList<Object> options, int command) throws IOException {
        try {
            switch (command) {
                case CURSOR_UP:
                    processCursorUp(optionInt(options, 0, 1));
                    return true;
                case CURSOR_DOWN:
                    processCursorDown(optionInt(options, 0, 1));
                    return true;
                case CURSOR_FORWARD:
                    processCursorRight(optionInt(options, 0, 1));
                    return true;
                case CURSOR_BACK:
                    processCursorLeft(optionInt(options, 0, 1));
                    return true;
                case CURSOR_DOWN_LINE:
                    processCursorDownLine(optionInt(options, 0, 1));
                    return true;
                case CURSOR_UP_LINE:
                    processCursorUpLine(optionInt(options, 0, 1));
                    return true;
                case CURSOR_TO_COL:
                    processCursorToColumn(optionInt(options, 0));
                    return true;
                case CURSOR_POS:
                case CURSOR_POS_ALT:
                    processCursorTo(optionInt(options, 0, 1), optionInt(options, 1, 1));
                    return true;
                case CURSOR_ERASE_SCREEN:
                    processEraseScreen(optionInt(options, 0, 0));
                    return true;
                case CURSOR_ERASE_LINE:
                    processEraseLine(optionInt(options, 0, 0));
                    return true;
                case SCROLL_UP:
                    processScrollUp(optionInt(options, 0, 1));
                    return true;
                case SCROLL_DOWN:
                    processScrollDown(optionInt(options, 0, 1));
                    return true;
                case TEXT_ATTRIBUTE:
                    int count = 0;
                    for (Object next : options) {
                        if (next != null) {
                            count++;

                            // will throw a ClassCast exception IF NOT an int.
                            int value = (Integer) next;

                            if (30 <= value && value <= 37) {
                                // foreground
                                processSetForegroundColor(value - 30);
                            }
                            else if (40 <= value && value <= 47) {
                                // background
                                processSetBackgroundColor(value - 40);
                            }
                            else {
                                switch (value) {
                                    case ATTRIBUTE_DEFAULT_FG:
                                        processDefaultTextColor();
                                        break;
                                    case ATTRIBUTE_DEFAULT_BG:
                                        processDefaultBackgroundColor();
                                        break;
                                    case ATTRIBUTE_RESET:
                                        processAttributeReset();
                                        break;
                                    default:
                                        processSetAttribute(value);
                                }
                            }
                        }
                    }

                    if (count == 0) {
                        processAttributeReset();
                    }
                    return true;
                case SAVE_CURSOR_POS:
                    processSaveCursorPosition();
                    return true;
                case RESTORE_CURSOR_POS:
                    processRestoreCursorPosition();
                    return true;

                default:
                    if ('a' <= command && command <= 'z') {
                        processUnknownExtension(options, command);
                        return true;
                    }
                    if ('A' <= command && command <= 'Z') {
                        processUnknownExtension(options, command);
                        return true;
                    }
                    return false;
            }
        } catch (IllegalArgumentException ignore) {
        }

        return false;
    }


    /**
     * @return true if the operating system command was processed.
     */
    private
    boolean processOperatingSystemCommand(final ArrayList<Object> options) throws IOException {
        final int command = optionInt(options, 0);
        final String label = (String) options.get(1);

        // for command > 2 label could be composed (i.e. contain ';'), but we'll leave
        // it to processUnknownOperatingSystemCommand implementations to handle that
        try {
            switch (command) {
                default:
                    // not exactly unknown, but not supported through dedicated process methods
                    processUnknownOperatingSystemCommand(command, label);
                    return true;
            }
        } catch (IllegalArgumentException ignore) {
        }

        return false;
    }

    protected
    void processRestoreCursorPosition() throws IOException {
    }

    protected
    void processSaveCursorPosition() throws IOException {
    }

    protected
    void processScrollDown(int optionInt) throws IOException {
    }

    protected
    void processScrollUp(int optionInt) throws IOException {
    }

    protected
    void processEraseScreen(int eraseOption) throws IOException {
    }

    protected
    void processEraseLine(int eraseOption) throws IOException {
    }

    protected
    void processSetAttribute(int attribute) throws IOException {
    }

    protected
    void processSetForegroundColor(int color) throws IOException {
    }

    protected
    void processSetBackgroundColor(int color) throws IOException {
    }

    protected
    void processDefaultTextColor() throws IOException {
    }

    protected
    void processDefaultBackgroundColor() throws IOException {
    }

    protected
    void processAttributeReset() throws IOException {
    }

    protected
    void processCursorTo(int row, int col) throws IOException {
    }

    protected
    void processCursorToColumn(int x) throws IOException {
    }

    protected
    void processCursorUpLine(int count) throws IOException {
    }

    protected
    void processCursorDownLine(int count) throws IOException {
    }

    protected
    void processCursorLeft(int count) throws IOException {
    }

    protected
    void processCursorRight(int count) throws IOException {
    }

    protected
    void processCursorDown(int count) throws IOException {
    }

    protected
    void processCursorUp(int count) throws IOException {
    }

    protected
    void processUnknownExtension(ArrayList<Object> options, int command) {
    }

    protected
    void processUnknownOperatingSystemCommand(int command, String param) {
    }

    private
    int optionInt(final ArrayList<Object> options, final int index) {
        if (options.size() <= index) {
            throw new IllegalArgumentException();
        }
        Object value = options.get(index);
        if (value == null) {
            throw new IllegalArgumentException();
        }
        if (!value.getClass()
                  .equals(Integer.class)) {
            throw new IllegalArgumentException();
        }
        return (Integer) value;
    }

    private
    int optionInt(final ArrayList<Object> options, final int index, final int defaultValue) {
        if (options.size() > index) {
            Object value = options.get(index);
            if (value == null) {
                return defaultValue;
            }
            return (Integer) value;
        }
        return defaultValue;
    }

    @Override
    public
    void close() throws IOException {
        flush();
        super.close();
    }
}
