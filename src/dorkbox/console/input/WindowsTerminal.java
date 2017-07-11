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


import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinNT.HANDLE;
import static com.sun.jna.platform.win32.Wincon.STD_INPUT_HANDLE;
import static com.sun.jna.platform.win32.Wincon.STD_OUTPUT_HANDLE;
import static dorkbox.util.jna.windows.Kernel32.ASSERT;
import static dorkbox.util.jna.windows.Kernel32.CloseHandle;
import static dorkbox.util.jna.windows.Kernel32.GetConsoleMode;
import static dorkbox.util.jna.windows.Kernel32.GetConsoleScreenBufferInfo;
import static dorkbox.util.jna.windows.Kernel32.GetStdHandle;
import static dorkbox.util.jna.windows.Kernel32.ReadConsoleInput;
import static dorkbox.util.jna.windows.Kernel32.SetConsoleMode;

import java.io.IOException;
import java.io.PrintStream;

import com.sun.jna.ptr.IntByReference;

import dorkbox.util.jna.windows.structs.CONSOLE_SCREEN_BUFFER_INFO;
import dorkbox.util.jna.windows.structs.INPUT_RECORD;
import dorkbox.util.jna.windows.structs.KEY_EVENT_RECORD;

/**
 * Terminal implementation for Microsoft Windows.
 */
public
class WindowsTerminal extends SupportedTerminal {

    // Console mode constants copied <tt>wincon.h</tt>.
    // There are OTHER options, however they DO NOT work with unbuffered input or we just don't care about them.
    /**
     * CTRL+C is processed by the system and is not placed in the input buffer. If the input buffer is being read by ReadFile or
     * ReadConsole, other control keys are processed by the system and are not returned in the ReadFile or ReadConsole buffer. If the
     * ENABLE_LINE_INPUT mode is also enabled, backspace, carriage return, and linefeed characters are handled by the system.
     */
    private static final int PROCESSED_INPUT = 1;

    // output stream for "echo" to goto
    private static final PrintStream OUT = System.out;

    private final HANDLE console;
    private final HANDLE outputConsole;

    private final CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
    private final INPUT_RECORD.ByReference inputRecords = new INPUT_RECORD.ByReference();
    private final IntByReference reference = new IntByReference();

    private volatile int originalMode;
    private boolean echoEnabled = false;

    public
    WindowsTerminal() throws IOException {
        console = GetStdHandle(STD_INPUT_HANDLE);
        if (console == INVALID_HANDLE_VALUE) {
            throw new IOException("Unable to get input console handle.");
        }

        outputConsole = GetStdHandle(STD_OUTPUT_HANDLE);
        if (outputConsole == INVALID_HANDLE_VALUE) {
            throw new IOException("Unable to get output console handle.");
        }

        IntByReference mode = new IntByReference();
        if (GetConsoleMode(console, mode) == 0) {
            throw new IOException(CONSOLE_ERROR_INIT);
        }

        this.originalMode = mode.getValue();

        int newMode = 0; // this is raw everything, not ignoring ctrl-c

        ASSERT(SetConsoleMode(console, newMode), Terminal.CONSOLE_ERROR_INIT);
    }

    /**
     * Restore the original terminal configuration, which can be used when shutting down the console reader.
     * The ConsoleReader cannot be used after calling this method.
     */
    @Override
    public final
    void restore() throws IOException {
        ASSERT(SetConsoleMode(console, this.originalMode), Terminal.CONSOLE_ERROR_INIT);

        CloseHandle(console);
        CloseHandle(outputConsole);
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
    protected
    void doSetEchoEnabled(final boolean enabled) {
        // only way to do this, console modes DO NOT work
        echoEnabled = enabled;
    }

    @Override
    protected
    void doSetInterruptEnabled(final boolean enabled) {
        IntByReference mode = new IntByReference();
        GetConsoleMode(console, mode);

        int newMode;
        if (enabled) {
            // Enable  Ctrl+C
            newMode = mode.getValue() | PROCESSED_INPUT;
        } else {
            // Disable Ctrl+C
            newMode = mode.getValue() & ~PROCESSED_INPUT;
        }

      ASSERT(SetConsoleMode(console, newMode), Terminal.CONSOLE_ERROR_INIT);
    }

    @Override
    protected final
    int doRead() {
        int input = readInput();

        if (echoEnabled) {
            char asChar = (char) input;
            if (asChar == '\n') {
                OUT.println();
            }
            else {
                OUT.write(asChar);
            }
            // have to flush, otherwise we'll never see the chars on screen
            OUT.flush();
        }

        return input;
    }

    private
    int readInput() {
        // keep reading input events until we find one that we are interested in (ie: keyboard input)
        while (true) {
            // blocks until there is (at least) 1 event on the buffer
            ReadConsoleInput(console, inputRecords, 1, reference);

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
                            } else if (uChar == '\n') {
                                continue;
                            }

                            return uChar;
                        }
                    }
                }
            }
        }
    }
}
