/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
package dorkbox.console.input;

import static dorkbox.console.util.windows.Kernel32.GetConsoleScreenBufferInfo;
import static dorkbox.console.util.windows.Kernel32.STD_INPUT_HANDLE;
import static dorkbox.console.util.windows.Kernel32.STD_OUTPUT_HANDLE;

import java.io.IOException;

import com.sun.jna.ptr.IntByReference;

import dorkbox.console.util.windows.CONSOLE_SCREEN_BUFFER_INFO;
import dorkbox.console.util.windows.ConsoleMode;
import dorkbox.console.util.windows.HANDLE;
import dorkbox.console.util.windows.INPUT_RECORD;
import dorkbox.console.util.windows.KEY_EVENT_RECORD;
import dorkbox.console.util.windows.Kernel32;

/**
 * Terminal implementation for Microsoft Windows.
 */
public
class WindowsTerminal extends Terminal {

    private final HANDLE console;
    private final HANDLE outputConsole;

    private final CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
    private final INPUT_RECORD.ByReference inputRecords = new INPUT_RECORD.ByReference();
    private final IntByReference reference = new IntByReference();

    private volatile int originalMode;

    public
    WindowsTerminal() throws IOException {
        console = Kernel32.GetStdHandle(STD_INPUT_HANDLE);
        if (console == HANDLE.INVALID_HANDLE_VALUE) {
            throw new IOException("Unable to get input console handle.");
        }

        outputConsole = Kernel32.GetStdHandle(STD_OUTPUT_HANDLE);
        if (outputConsole == HANDLE.INVALID_HANDLE_VALUE) {
            throw new IOException("Unable to get output console handle.");
        }

        IntByReference mode = new IntByReference();
        if (Kernel32.GetConsoleMode(console, mode) == 0) {
            throw new IOException(CONSOLE_ERROR_INIT);
        }

        this.originalMode = mode.getValue();

        int newMode = this.originalMode |
                      ConsoleMode.ENABLE_LINE_INPUT.code |
                      ConsoleMode.ENABLE_ECHO_INPUT.code |
                      ConsoleMode.ENABLE_PROCESSED_INPUT.code |
                      ConsoleMode.ENABLE_WINDOW_INPUT.code;

        // Disable input echo
        newMode = newMode & ~ConsoleMode.ENABLE_ECHO_INPUT.code;

        // Must set these four modes at the same time to make it work fine.
        Kernel32.SetConsoleMode(console, newMode);
    }

    /**
     * Restore the original terminal configuration, which can be used when shutting down the console reader.
     * The ConsoleReader cannot be used after calling this method.
     */
    @Override
    public final
    void restore() throws IOException {
        // restore the old console mode
        Kernel32.SetConsoleMode(console, this.originalMode);

        Kernel32.CloseHandle(console);
        Kernel32.CloseHandle(outputConsole);
    }

    @Override
    public final
    int getWidth() {
        GetConsoleScreenBufferInfo(outputConsole, info);
        int w = info.window.width() + 1;
        return w < 1 ? DEFAULT_WIDTH : w;
    }

    @Override
    public final
    int getHeight() {
        GetConsoleScreenBufferInfo(outputConsole, info);
        int h = info.window.height() + 1;
        return h < 1 ? DEFAULT_HEIGHT : h;
    }

    @Override
    public
    void setEchoEnabled(final boolean enabled) {
        IntByReference mode = new IntByReference();
        Kernel32.GetConsoleMode(console, mode);

        int newMode;
        if (enabled) {
            // Enable  Ctrl+C
            newMode = mode.getValue() | ConsoleMode.ENABLE_ECHO_INPUT.code;
        } else {
            // Disable Ctrl+C
            newMode = mode.getValue() & ~ConsoleMode.ENABLE_ECHO_INPUT.code;
        }

        Kernel32.SetConsoleMode(console, newMode);
    }

    @Override
    public
    void setInterruptEnabled(final boolean enabled) {
        IntByReference mode = new IntByReference();
        Kernel32.GetConsoleMode(console, mode);

        int newMode;
        if (enabled) {
            // Enable  Ctrl+C
            newMode = mode.getValue() | ConsoleMode.ENABLE_PROCESSED_INPUT.code;
        } else {
            // Disable Ctrl+C
            newMode = mode.getValue() & ~ConsoleMode.ENABLE_PROCESSED_INPUT.code;
        }

        Kernel32.SetConsoleMode(console, newMode);
    }

    @Override
    public final
    int read() {
        int input = readInput();

//        if (Console.ENABLE_ECHO) {
//            char asChar = (char) input;
//            if (asChar == '\n') {
//                System.out.println();
//            }
//            else {
//                System.out.print(asChar);
//            }
//
//            // have to flush, otherwise we'll never see the chars on screen
//            System.out.flush();
//        }

        return input;
    }

    private
    int readInput() {
        // keep reading input events until we find one that we are interested in (ie: keyboard input)
        while (true) {
            // blocks until there is (at least) 1 event on the buffer
            Kernel32.ReadConsoleInputW(console, inputRecords, 1, reference);

            for (int i = 0; i < reference.getValue(); ++i) {
                if (inputRecords.EventType == INPUT_RECORD.KEY_EVENT) {
                    KEY_EVENT_RECORD keyEvent = inputRecords.Event.KeyEvent;

                    //logger.trace(keyEvent.bKeyDown ? "KEY_DOWN" : "KEY_UP", "key code:", keyEvent.wVirtualKeyCode, "char:", (long)keyEvent.uChar.unicodeChar);
                    if (keyEvent.keyDown) {
                        final char uChar = keyEvent.uChar.unicodeChar;
                        if (uChar > 0) {
                            if (uChar == '\r') {
                                // we purposefully swallow input after \r, and substitute it with \n
                                return '\n';
                            }

                            return uChar;
                        }
                    }
                }
            }
        }
    }
}
