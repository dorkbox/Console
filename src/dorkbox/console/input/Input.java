/*
 * Copyright 2010 dorkbox, llc
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
package dorkbox.console.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import dorkbox.console.Console;
import dorkbox.util.OS;

public
class Input {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Console.class);

    public final static Terminal terminal;
    static {
        String type = Console.INPUT_CONSOLE_TYPE.toUpperCase(Locale.ENGLISH);

        Throwable didFallbackE = null;
        Terminal term;
        try {
            if (type.equals("UNIX")) {
                term = new PosixTerminal();
            }
            else if (type.equals("WINDOWS")) {
                term = new WindowsTerminal();
            }
            else if (type.equals("NONE")) {
                term = new UnsupportedTerminal();
            }
            else {
                // if these cannot be created, because we are in an IDE, an error will be thrown
                if (OS.isWindows()) {
                    term = new WindowsTerminal();
                }
                else {
                    term = new PosixTerminal();
                }
            }
        } catch (Exception e) {
            didFallbackE = e;
            term = new UnsupportedTerminal();
        }

        terminal = term;

        boolean debugEnabled = logger.isDebugEnabled();
        if (didFallbackE != null && !didFallbackE.getMessage().equals(Terminal.CONSOLE_ERROR_INIT)) {
            logger.error("Failed to construct terminal, falling back to unsupported.", didFallbackE);
        } else if (debugEnabled && term instanceof UnsupportedTerminal) {
            logger.debug("Terminal is UNSUPPORTED (best guess). Unable to support single key input. Only line input available.");
        } else if (debugEnabled) {
            logger.debug("Created Terminal: {} ({}w x {}h)", Input.terminal.getClass().getSimpleName(),
                         Input.terminal.getWidth(), Input.terminal.getHeight());
        }


        if (term instanceof SupportedTerminal) {
            // echo and backspace
            term.setEchoEnabled(Console.ENABLE_ECHO);
            term.setInterruptEnabled(Console.ENABLE_INTERRUPT);

            Thread consoleThread = new Thread((SupportedTerminal)term);
            consoleThread.setDaemon(true);
            consoleThread.setName("Console Input Reader");
            consoleThread.start();


            // has to be NOT DAEMON thread, since it must run before the app closes.
            // alternatively, shut everything down when the JVM closes.
            Thread shutdownThread = new Thread() {
                @Override
                public
                void run() {
                    // called when the JVM is shutting down.
                    terminal.close();

                    try {
                        terminal.restore();
                        // this will 'hang' our shutdown, and honestly, who cares? We're shutting down anyways.
                        // inputConsole.reader.close(); // hangs on shutdown
                    } catch (IOException ignored) {
                        ignored.printStackTrace();
                    }
                }
            };
            shutdownThread.setName("Console Input Shutdown");
            Runtime.getRuntime().addShutdownHook(shutdownThread);
        }
    }

    public final static InputStream wrappedInputStream = new InputStream() {
        @Override
        public
        int read() throws IOException {
            return terminal.read();
        }

        @Override
        public
        void close() throws IOException {
            terminal.close();
        }
    };

    private
    Input() {
    }
}

