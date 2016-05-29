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
package dorkbox.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;

import dorkbox.console.input.PosixTerminal;
import dorkbox.console.input.Terminal;
import dorkbox.console.input.UnsupportedTerminal;
import dorkbox.console.input.WindowsTerminal;
import dorkbox.console.output.Ansi;
import dorkbox.console.util.CharHolder;
import dorkbox.objectPool.ObjectPool;
import dorkbox.objectPool.PoolableObject;
import dorkbox.util.OS;
import dorkbox.util.bytes.ByteBuffer2;
import dorkbox.util.bytes.ByteBuffer2Poolable;

class Input {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Input.class);
    private static final char[] emptyLine = new char[0];


    private static final List<CharHolder> charInputBuffers = new ArrayList<CharHolder>();
    private final static ObjectPool<CharHolder> charInputPool = ObjectPool.NonBlocking(new PoolableObject<CharHolder>() {
        @Override
        public
        CharHolder create() {
            return new CharHolder();
        }

        @Override
        public
        void onReturn(final CharHolder object) {
            // dump the chars in the buffer (safer for passwords, etc)
            object.character = (char) 0;
            charInputBuffers.remove(object);
        }

        @Override
        public
        void onTake(final CharHolder object) {
            charInputBuffers.add(object);
        }
    });


    private static final List<ByteBuffer2> lineInputBuffers = new ArrayList<ByteBuffer2>();
    private final static ObjectPool<ByteBuffer2> lineInputPool = ObjectPool.NonBlocking(new ByteBuffer2Poolable() {
        @Override
        public
        void onReturn(final ByteBuffer2 object) {
            // dump the chars in the buffer (safer for passwords, etc)
            object.clearSecure();
            lineInputBuffers.remove(object);
        }

        @Override
        public
        void onTake(final ByteBuffer2 object) {
            lineInputBuffers.add(object);
        }
    });



    private final static Terminal terminal;

    private static final Object inputLock = new Object();
    private static final Object inputLockSingle = new Object();
    private static final Object inputLockLine = new Object();

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


        if (logger.isDebugEnabled()) {
            logger.debug("Created Terminal: {} ({}w x {}h)", Input.terminal.getClass().getSimpleName(),
                         Input.terminal.getWidth(), Input.terminal.getHeight());
        }

        if (didFallbackE != null && !didFallbackE.getMessage().equals(Terminal.CONSOLE_ERROR_INIT)) {
            logger.error("Failed to construct terminal, falling back to unsupported.", didFallbackE);
        } else if (term instanceof UnsupportedTerminal) {
            logger.debug("Terminal is in UNSUPPORTED (best guess). Unable to support single key input. Only line input available.");
        }

        // echo and backspace
        term.setEchoEnabled(Console.ENABLE_ECHO);
        term.setInterruptEnabled(Console.ENABLE_INTERRUPT);

        Thread consoleThread = new Thread(new Runnable() {
            @Override
            public
            void run() {
                Input.run();
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.setName("Console Input Reader");

        consoleThread.start();

        // has to be NOT DAEMON thread, since it must run before the app closes.

        // don't forget we have to shut down the ansi console as well
        // alternatively, shut everything down when the JVM closes.
        Thread shutdownThread = new Thread() {
            @Override
            public
            void run() {
                // called when the JVM is shutting down.
                release0();

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

    static
    void setInterruptEnabled(final boolean enabled) {
        terminal.setInterruptEnabled(enabled);
    }

    static
    void setEchoEnabled(final boolean enabled) {
        terminal.setEchoEnabled(enabled);
    }

    private static InputStream wrappedInputStream = new InputStream() {
        @Override
        public
        int read() throws IOException {
            return Input.read();
        }

        @Override
        public
        void close() throws IOException {
            Input.release0();
        }
    };

    static
    InputStream getInputStream() {
        return wrappedInputStream;
    }

    private
    Input() {
    }

    /**
     * Reads single character input from the console.
     *
     * @return -1 if no data or problems
     */
    static
    int read() {
        CharHolder holder;

        synchronized (inputLock) {
            // don't want to register a read() WHILE we are still processing the current input.
            // also adds it to the global list of char inputs
            holder = charInputPool.take();
        }

        synchronized (inputLockSingle) {
            try {
                inputLockSingle.wait();
            } catch (InterruptedException e) {
                return -1;
            }
        }

        char c = holder.character;

        // also clears and removes from the global list of char inputs
        charInputPool.put(holder);

        return c;
    }

    /**
     * @return empty char[] if no data or problems
     */
    static
    char[] readLineChars() {
        ByteBuffer2 buffer;

        synchronized (inputLock) {
            // don't want to register a readLine() WHILE we are still processing the current line info.
            // also adds it to the global list of line inputs
            buffer = lineInputPool.take();
        }

        synchronized (inputLockLine) {
            try {
                inputLockLine.wait();
            } catch (InterruptedException e) {
                return emptyLine;
            }
        }

        int len = buffer.position();
        if (len == 0) {
            return emptyLine;
        }

        buffer.rewind();
        char[] readChars = buffer.readChars(len / 2); // java always stores chars in 2 bytes

        // also clears and removes from the global list of line inputs
        lineInputPool.put(buffer);

        return readChars;
    }

    /**
     * Reads line input from the console
     *
     * @return empty char[] if no data
     */
    static
    char[] readLinePassword() {
        // don't bother in an IDE. it won't work.
        boolean echoEnabled = Console.ENABLE_ECHO;
        Console.ENABLE_ECHO = false;
        char[] readLine0 = readLineChars();
        Console.ENABLE_ECHO = echoEnabled;

        return readLine0;
    }

    /**
     * Reads a single line of characters, defined as everything before the 'ENTER' key is pressed
     * @return null if no data
     */
    static
    String readLine() {
        char[] line = Input.readLineChars();
        if (line == null) {
            return null;
        }
        return new String(line);
    }



    /**
     * releases any thread still waiting.
     */
    private static
    void release0() {
        synchronized (inputLockSingle) {
            inputLockSingle.notifyAll();
        }

        synchronized (inputLockLine) {
            inputLockLine.notifyAll();
        }
    }

    private static
    void run() {
        final Logger logger2 = logger;
        final PrintStream out = System.out;
        final char overWriteChar = ' ';

        Ansi ansi = null;
        int typedChar;
        char asChar;

        while ((typedChar = terminal.read()) != -1) {
            synchronized (inputLock) {
                // don't let anyone add a new reader while we are still processing the current actions
                asChar = (char) typedChar;

                if (logger2.isTraceEnabled()) {
                    logger2.trace("READ: {} ({})", asChar, typedChar);
                }

                // notify everyone waiting for a character.
                synchronized (inputLockSingle) {
                    // have to do readChar first (readLine has to deal with \b and \n
                    for (CharHolder holder : charInputBuffers) {
                        holder.character = asChar; // copy by value
                    }

                    inputLockSingle.notifyAll();
                }

                // now to handle readLine stuff

                // if we type a backspace key, swallow it + previous in READLINE. READCHAR will have it passed anyways.
                if (Console.ENABLE_BACKSPACE && asChar == '\b') {
                    int position = 0;
                    char[] overwrite = null;

                    // clear ourself + one extra.
                    for (ByteBuffer2 buffer : lineInputBuffers) {
                        // size of the buffer BEFORE our backspace was typed
                        int length = buffer.position();
                        int amtToOverwrite = 4; // 2*2 backspace is always 2 chars (^?) * 2 because it's bytes

                        if (length > 1) {
                            char charAt = buffer.readChar(length - 2);
                            amtToOverwrite += getPrintableCharacters(charAt);

                            // delete last item in our buffer
                            length -= 2;
                            buffer.setPosition(length);

                            // now figure out where the cursor is really at.
                            // this is more memory friendly than buf.toString.length
                            for (int i = 0; i < length; i += 2) {
                                charAt = buffer.readChar(i);
                                position += getPrintableCharacters(charAt);
                            }

                            position++;
                        }

                        overwrite = new char[amtToOverwrite];
                        for (int i = 0; i < amtToOverwrite; i++) {
                            overwrite[i] = overWriteChar;
                        }
                    }

                    if (Console.ENABLE_ANSI && overwrite != null) {
                        if (ansi == null) {
                            ansi = Ansi.ansi();
                        }

                        // move back however many, over write, then go back again
                        out.print(ansi.cursorToColumn(position));
                        out.print(overwrite);
                        out.print(ansi.cursorToColumn(position));
                        out.flush();
                    }
                }
                else if (asChar == '\n') {
                    // ignoring \r, because \n is ALWAYS the last character in a new line sequence. (even for windows, which we changed)
                    synchronized (inputLockLine) {
                        inputLockLine.notifyAll();
                    }
                }
                else {
                    // only append if we are not a new line.
                    // our windows console PREVENTS us from returning '\r' (it truncates '\r\n', and returns just '\n')
                    for (ByteBuffer2 buffer : lineInputBuffers) {
                        buffer.writeChar(asChar);
                    }
                }
            }
        }
    }

    private static final int PLUS_TWO_MAYBE = 128 + 32;
    private static final int PLUS_ONE = 128 + 127;

    /**
     * Return the number of characters that will be printed when the specified character is echoed to the screen
     * <p/>
     * Adapted from cat by Torbjorn Granlund, as repeated in stty by David MacKenzie.
     */
    private static
    int getPrintableCharacters(final int ch) {
        // StringBuilder sbuff = new StringBuilder();

        if (ch >= 32) {
            if (ch < 127) {
                // sbuff.append((char) ch);
                return 1;
            }
            else if (ch == 127) {
                // sbuff.append('^');
                // sbuff.append('?');
                return 2;
            }
            else {
                // sbuff.append('M');
                // sbuff.append('-');
                int count = 2;

                if (ch >= PLUS_TWO_MAYBE) {
                    if (ch < PLUS_ONE) {
                        // sbuff.append((char) (ch - 128));
                        count++;
                    }
                    else {
                        // sbuff.append('^');
                        // sbuff.append('?');
                        count += 2;
                    }
                }
                else {
                    // sbuff.append('^');
                    // sbuff.append((char) (ch - 128 + 64));
                    count += 2;
                }
                return count;
            }
        }
        else {
            // sbuff.append('^');
            // sbuff.append((char) (ch + 64));
            return 2;
        }

        // return sbuff;
    }
}

