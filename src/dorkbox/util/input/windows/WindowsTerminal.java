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
package dorkbox.util.input.windows;

import java.io.IOException;
import java.io.PrintStream;

import org.fusesource.jansi.internal.Kernel32.INPUT_RECORD;
import org.fusesource.jansi.internal.Kernel32.KEY_EVENT_RECORD;
import org.fusesource.jansi.internal.WindowsSupport;

import dorkbox.util.input.Terminal;

/**
 * Terminal implementation for Microsoft Windows. Terminal initialization in {@link #init} is accomplished by calling the Win32 APIs <a
 * href="http://msdn.microsoft.com/library/default.asp? url=/library/en-us/dllproc/base/setconsolemode.asp">SetConsoleMode</a> and <a
 * href="http://msdn.microsoft.com/library/default.asp? url=/library/en-us/dllproc/base/getconsolemode.asp">GetConsoleMode</a> to disable
 * character echoing.
 * <p/>
 *
 * @since 2.0 (customized)
 */
public class WindowsTerminal extends Terminal {

    private volatile int originalMode;
    private final PrintStream out;

    public WindowsTerminal() {
        this.out = System.out;
    }

    @Override
    public final void init() throws IOException {
        this.originalMode = WindowsSupport.getConsoleMode();

        // Must set these four modes at the same time to make it work fine.
        WindowsSupport.setConsoleMode(this.originalMode |
                                  ConsoleMode.ENABLE_LINE_INPUT.code |
                                  ConsoleMode.ENABLE_ECHO_INPUT.code |
                                  ConsoleMode.ENABLE_PROCESSED_INPUT.code |
                                  ConsoleMode.ENABLE_WINDOW_INPUT.code);
    }

    /**
     * Restore the original terminal configuration, which can be used when shutting down the console reader. The ConsoleReader cannot be
     * used after calling this method.
     */
    @Override
    public final void restore() throws IOException {
        // restore the old console mode
        WindowsSupport.setConsoleMode(this.originalMode);
    }

    @Override
    public final int getWidth() {
        int w = WindowsSupport.getWindowsTerminalWidth();
        return w < 1 ? DEFAULT_WIDTH : w;
    }

    @Override
    public final int getHeight() {
        int h = WindowsSupport.getWindowsTerminalHeight();
        return h < 1 ? DEFAULT_HEIGHT : h;
    }

    @Override
    public final int read() {
        int input = readInput();

        if (isEchoEnabled()) {
            char asChar = (char) input;
            if (asChar == '\n') {
                this.out.println();
            } else {
                this.out.print(asChar);
            }
            // have to flush, otherwise we'll never see the chars on screen
            this.out.flush();
        }

        return input;
    }

    private int readInput() {
        // this HOOKS the input event, and prevents it from going to the console "proper"
        try {
            INPUT_RECORD[] events;
            while (true) {
                // we ALWAYS read until we have an event we care about!
                events = WindowsSupport.readConsoleInput(1);

                if (events != null) {
                    for (int i = 0; i < events.length; i++) {
                        KEY_EVENT_RECORD keyEvent = events[i].keyEvent;
                        // Log.trace(keyEvent.keyDown? "KEY_DOWN" : "KEY_UP", "key code:", keyEvent.keyCode, "char:", (long)keyEvent.uchar);
                        if (keyEvent.keyDown) {
                            if (keyEvent.uchar > 0) {
                                char uchar = keyEvent.uchar;
                                if (uchar == '\r') {
                                    // we purposefully swallow input after \r, and substitute it with \n
                                    return '\n';
                                }

                                return uchar;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            this.logger.error("Windows console input error: ", e);
        }

        return -1;
    }
}
