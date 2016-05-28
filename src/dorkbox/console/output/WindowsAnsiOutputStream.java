/*
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

import static dorkbox.console.util.windows.Kernel32.ASSERT;

import java.io.IOException;
import java.io.OutputStream;

import dorkbox.console.util.windows.CONSOLE_SCREEN_BUFFER_INFO;
import dorkbox.console.util.windows.COORD;
import dorkbox.console.util.windows.HANDLE;
import dorkbox.console.util.windows.Kernel32;

/**
 * A Windows ANSI escape processor, uses JNA direct-mapping to access native platform API's to change the console attributes.
 *
 * See: https://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * @author dorkbox, llc
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author Joris Kuipers
 */
@SuppressWarnings("NumericCastThatLosesPrecision")
final class WindowsAnsiOutputStream extends AnsiOutputStream {
    private static final short ANSI_FOREGROUND_COLOR_MAP[];
    private static final short ANSI_BACKGROUND_COLOR_MAP[];

    static {
        ANSI_FOREGROUND_COLOR_MAP = new short[8];
        ANSI_FOREGROUND_COLOR_MAP[BLACK] = Kernel32.FOREGROUND_BLACK;
        ANSI_FOREGROUND_COLOR_MAP[RED] = Kernel32.FOREGROUND_RED;
        ANSI_FOREGROUND_COLOR_MAP[GREEN] = Kernel32.FOREGROUND_GREEN;
        ANSI_FOREGROUND_COLOR_MAP[YELLOW] = Kernel32.FOREGROUND_YELLOW;
        ANSI_FOREGROUND_COLOR_MAP[BLUE] = Kernel32.FOREGROUND_BLUE;
        ANSI_FOREGROUND_COLOR_MAP[MAGENTA] = Kernel32.FOREGROUND_MAGENTA;
        ANSI_FOREGROUND_COLOR_MAP[CYAN] = Kernel32.FOREGROUND_CYAN;
        ANSI_FOREGROUND_COLOR_MAP[WHITE] = Kernel32.FOREGROUND_GREY;

        ANSI_BACKGROUND_COLOR_MAP = new short[8];
        ANSI_BACKGROUND_COLOR_MAP[BLACK] = Kernel32.BACKGROUND_BLACK;
        ANSI_BACKGROUND_COLOR_MAP[RED] = Kernel32.BACKGROUND_RED;
        ANSI_BACKGROUND_COLOR_MAP[GREEN] = Kernel32.BACKGROUND_GREEN;
        ANSI_BACKGROUND_COLOR_MAP[YELLOW] = Kernel32.BACKGROUND_YELLOW;
        ANSI_BACKGROUND_COLOR_MAP[BLUE] = Kernel32.BACKGROUND_BLUE;
        ANSI_BACKGROUND_COLOR_MAP[MAGENTA] = Kernel32.BACKGROUND_MAGENTA;
        ANSI_BACKGROUND_COLOR_MAP[CYAN] = Kernel32.BACKGROUND_CYAN;
        ANSI_BACKGROUND_COLOR_MAP[WHITE] = Kernel32.BACKGROUND_GREY;
    }

    private final HANDLE console;
    private final CONSOLE_SCREEN_BUFFER_INFO originalInfo = new CONSOLE_SCREEN_BUFFER_INFO();
    private volatile CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();

    private volatile boolean negative;
    private volatile short savedX = (short) -1;
    private volatile short savedY = (short) -1;

    WindowsAnsiOutputStream(final OutputStream os, int fileHandle) throws IOException {
        super(os);

        if (fileHandle == AnsiConsole.STDOUT_FILENO) {
            fileHandle = Kernel32.STD_OUTPUT_HANDLE;
        } else if (fileHandle == AnsiConsole.STDERR_FILENO) {
            fileHandle = Kernel32.STD_ERROR_HANDLE;
        } else {
            throw new IllegalArgumentException("Invalid file handle " + fileHandle);
        }

        console = Kernel32.GetStdHandle(fileHandle);
        if (console == HANDLE.INVALID_HANDLE_VALUE) {
            throw new IOException("Unable to get input console handle.");
        }

        out.flush();
        ASSERT(Kernel32.GetConsoleScreenBufferInfo(console, originalInfo), "Could not get the screen info");
    }

    private
    void getConsoleInfo() throws IOException {
        out.flush();
        ASSERT(Kernel32.GetConsoleScreenBufferInfo(console, info), "Could not get the screen info:");
    }

    private
    void applyAttributes() throws IOException {
        out.flush();

        short attributes = info.attributes;
        if (negative) {
            // Swap the the Foreground and Background bits.
            int fg = 0x000F & attributes;
            fg <<= 8;
            int bg = 0X00F0 * attributes;
            bg >>= 8;
            attributes = (short) (attributes & 0xFF00 | fg | bg);
        }

        ASSERT(Kernel32.SetConsoleTextAttribute(console, attributes), "Could not set text attributes");
    }

    private
    void applyCursorPosition() throws IOException {
        ASSERT(Kernel32.SetConsoleCursorPosition(console, info.cursorPosition.asValue()), "Could not set cursor position");
    }

    @Override
    protected
    void processRestoreCursorPosition() throws IOException {
        // restore only if there was a save operation first
        if (savedX != -1 && savedY != -1) {
            out.flush();
            info.cursorPosition.x = savedX;
            info.cursorPosition.y = savedY;
            applyCursorPosition();
        }
    }

    @Override
    protected
    void processSaveCursorPosition() throws IOException {
        getConsoleInfo();
        savedX = info.cursorPosition.x;
        savedY = info.cursorPosition.y;
    }

    @Override
    protected
    void processEraseScreen(final int eraseOption) throws IOException {
        getConsoleInfo();
        int[] written = new int[1];
        switch (eraseOption) {
            case ERASE_ALL:
                COORD topLeft = new COORD();
                topLeft.x = (short) 0;
                topLeft.y = info.window.top;
                int screenLength = info.window.height() * info.size.x;

                ASSERT(Kernel32.FillConsoleOutputAttribute(console, originalInfo.attributes, screenLength, topLeft.asValue(), written), "Could not fill console");
                ASSERT(Kernel32.FillConsoleOutputCharacterW(console, ' ', screenLength, topLeft.asValue(), written), "Could not fill console");
                break;
            case ERASE_TO_BEGINNING:
                COORD topLeft2 = new COORD();
                topLeft2.x = (short) 0;
                topLeft2.y = info.window.top;
                int lengthToCursor = (info.cursorPosition.y - info.window.top) * info.size.x + info.cursorPosition.x;

                ASSERT(Kernel32.FillConsoleOutputAttribute(console, originalInfo.attributes, lengthToCursor, topLeft2.asValue(), written), "Could not fill console");
                ASSERT(Kernel32.FillConsoleOutputCharacterW(console, ' ', lengthToCursor, topLeft2.asValue(), written), "Could not fill console");
                break;
            case ERASE_TO_END:
                int lengthToEnd = (info.window.bottom - info.cursorPosition.y) * info.size.x + info.size.x - info.cursorPosition.x;

                ASSERT(Kernel32.FillConsoleOutputAttribute(console, originalInfo.attributes, lengthToEnd, info.cursorPosition.asValue(), written), "Could not fill console");
                ASSERT(Kernel32.FillConsoleOutputCharacterW(console, ' ', lengthToEnd, info.cursorPosition.asValue(), written), "Could not fill console");
        }
    }

    @Override
    protected
    void processEraseLine(final int eraseOption) throws IOException {
        getConsoleInfo();
        int[] written = new int[1];
        switch (eraseOption) {
            case ERASE_ALL:
                COORD currentRow = info.cursorPosition.asValue();
                currentRow.x = (short) 0;

                ASSERT(Kernel32.FillConsoleOutputAttribute(console, originalInfo.attributes, info.size.x, currentRow.asValue(), written), "Could not fill console");
                ASSERT(Kernel32.FillConsoleOutputCharacterW(console, ' ', info.size.x, currentRow.asValue(), written), "Could not fill console");
                break;
            case ERASE_TO_BEGINNING:
                COORD leftColCurrRow2 = info.cursorPosition.asValue();
                leftColCurrRow2.x = (short) 0;

                ASSERT(Kernel32.FillConsoleOutputAttribute(console, originalInfo.attributes, info.cursorPosition.x, leftColCurrRow2.asValue(), written), "Could not fill console");
                ASSERT(Kernel32.FillConsoleOutputCharacterW(console, ' ', info.cursorPosition.x, leftColCurrRow2.asValue(), written), "Could not fill console");
                break;
            case ERASE_TO_END:
                int lengthToLastCol = info.size.x - info.cursorPosition.x;

                ASSERT(Kernel32.FillConsoleOutputAttribute(console, originalInfo.attributes, lengthToLastCol, info.cursorPosition.asValue(), written), "Could not fill console");
                ASSERT(Kernel32.FillConsoleOutputCharacterW(console, ' ', lengthToLastCol, info.cursorPosition.asValue(), written), "Could not fill console");
        }
    }

    @Override
    protected
    void processSetAttribute(final int attribute) throws IOException {
        if (90 <= attribute && attribute <= 97) {
            // foreground bright
            info.attributes = (short) (info.attributes & ~0x000F | ANSI_FOREGROUND_COLOR_MAP[attribute - 90]);
            info.attributes = (short) (info.attributes | Kernel32.FOREGROUND_INTENSITY);
            applyAttributes();
            return;
        } else if (100 <= attribute && attribute <= 107) {
            // background bright
            info.attributes = (short) (info.attributes & ~0x00F0 | ANSI_BACKGROUND_COLOR_MAP[attribute - 100]);
            info.attributes = (short) (info.attributes | Kernel32.BACKGROUND_INTENSITY);
            applyAttributes();
            return;
        }

        switch (attribute) {
            case ATTRIBUTE_BOLD:
                info.attributes = (short) (info.attributes | Kernel32.FOREGROUND_INTENSITY);
                applyAttributes();
                break;
            case ATTRIBUTE_NORMAL:
                info.attributes = (short) (info.attributes & ~Kernel32.FOREGROUND_INTENSITY);
                applyAttributes();
                break;

            // Yeah, setting the background intensity is not underlining.. but it's best we can do using the Windows console API
            case ATTRIBUTE_UNDERLINE:
                info.attributes = (short) (info.attributes | Kernel32.BACKGROUND_INTENSITY);
                applyAttributes();
                break;
            case ATTRIBUTE_UNDERLINE_OFF:
                info.attributes = (short) (info.attributes & ~Kernel32.BACKGROUND_INTENSITY);
                applyAttributes();
                break;

            case ATTRIBUTE_NEGATIVE_ON:
                negative = true;
                applyAttributes();
                break;
            case ATTRIBUTE_NEGATIVE_OFF:
                negative = false;
                applyAttributes();
                break;
        }
    }

    @Override
    protected
    void processSetForegroundColor(final int color) throws IOException {
        info.attributes = (short) (info.attributes & ~0x000F | ANSI_FOREGROUND_COLOR_MAP[color]);
        applyAttributes();
    }

    @Override
    protected
    void processSetBackgroundColor(final int color) throws IOException {
        info.attributes = (short) (info.attributes & ~0x00F0 | ANSI_BACKGROUND_COLOR_MAP[color]);
        applyAttributes();
    }

    @Override
    protected
    void processDefaultTextColor() throws IOException {
        info.attributes = (short) (info.attributes & ~0x000F | originalInfo.attributes & 0x000F);
        applyAttributes();
    }

    @Override
    protected
    void processDefaultBackgroundColor() throws IOException {
        info.attributes = (short) (info.attributes & ~0x00F0 | originalInfo.attributes & 0x00F0);
        applyAttributes();
    }

    @Override
    protected
    void processAttributeReset() throws IOException {
        //info.attributes = originalInfo.attributes;
        info.attributes = (short)((info.attributes & ~0x00FF ) | originalInfo.attributes);
        this.negative = false;
        applyAttributes();
    }

    @Override
    protected
    void processScrollDown(final int optionInt) throws IOException {
    }

    @Override
    protected
    void processScrollUp(final int optionInt) throws IOException {
    }

    protected
    void processCursorUpLine(final int count) throws IOException {
    }

    protected
    void processCursorDownLine(final int count) throws IOException {
    }

    @Override
    protected
    void processCursorTo(final int row, final int col) throws IOException {
        getConsoleInfo();
        info.cursorPosition.y = (short) Math.max(info.window.top, Math.min(info.size.y, info.window.top + row - 1));
        info.cursorPosition.x = (short) Math.max(0, Math.min(info.window.width(), col - 1));
        applyCursorPosition();
    }

    @Override
    protected
    void processCursorToColumn(final int x) throws IOException {
        getConsoleInfo();
        info.cursorPosition.x = (short) Math.max(0, Math.min(info.window.width(), x - 1));
        applyCursorPosition();
    }

    @Override
    protected
    void processCursorLeft(final int count) throws IOException {
        getConsoleInfo();
        info.cursorPosition.x = (short) Math.max(0, info.cursorPosition.x - count);
        applyCursorPosition();
    }

    @Override
    protected
    void processCursorRight(final int count) throws IOException {
        getConsoleInfo();
        info.cursorPosition.x = (short) Math.min(info.window.width(), info.cursorPosition.x + count);
        applyCursorPosition();
    }

    @Override
    protected
    void processCursorDown(final int count) throws IOException {
        getConsoleInfo();
        info.cursorPosition.y = (short) Math.min(info.size.y, info.cursorPosition.y + count);
        applyCursorPosition();
    }

    @Override
    protected
    void processCursorUp(final int count) throws IOException {
        getConsoleInfo();
        info.cursorPosition.y = (short) Math.max(info.window.top, info.cursorPosition.y - count);
        applyCursorPosition();
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (console != null) {
            Kernel32.CloseHandle(console);
        }
    }
}
