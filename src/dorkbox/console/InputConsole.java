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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;

import dorkbox.console.input.PosixTerminal;
import dorkbox.console.input.Terminal;
import dorkbox.console.input.UnsupportedTerminal;
import dorkbox.console.input.WindowsTerminal;
import dorkbox.console.output.Ansi;
import dorkbox.objectPool.ObjectPool;
import dorkbox.util.FastThreadLocal;
import dorkbox.util.OS;
import dorkbox.util.bytes.ByteBuffer2;
import dorkbox.util.bytes.ByteBuffer2Poolable;

public
class InputConsole {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InputConsole.class);
    private static final char[] emptyLine = new char[0];


    private final static ObjectPool<ByteBuffer2> pool;
    private final static Terminal terminal;

    private static final Object inputLock = new Object();
    private static final Object inputLockSingle = new Object();
    private static final Object inputLockLine = new Object();

    private static final FastThreadLocal<ByteBuffer2> readBuff = new FastThreadLocal<ByteBuffer2>();
    private static final List<ByteBuffer2> readBuffers = new CopyOnWriteArrayList<ByteBuffer2>();
    private static final FastThreadLocal<Integer> threadBufferCounter = new FastThreadLocal<Integer>();

    private static final FastThreadLocal<ByteBuffer2> readLineBuff = new FastThreadLocal<ByteBuffer2>();
    private static final List<ByteBuffer2> readLineBuffers = new CopyOnWriteArrayList<ByteBuffer2>();



    static {
        pool = ObjectPool.Blocking(new ByteBuffer2Poolable(), Console.NUMBER_OF_READERS);

        String type = Console.INPUT_CONSOLE_OSTYPE.toUpperCase(Locale.ENGLISH);

        Throwable didFallbackE = null;
        Class<? extends Terminal> t;
        try {
            if (type.equals(Console.UNIX)) {
                t = PosixTerminal.class;
            }
            else if (type.equals(Console.WIN) || type.equals(Console.WINDOWS)) {
                t = WindowsTerminal.class;
            }
            else if (type.equals(Console.NONE) || type.equals(Console.OFF) || type.equals(Console.FALSE)) {
                t = UnsupportedTerminal.class;
            }
            else {
                // if these cannot be created, because we are in an IDE, an error will be thrown
                if (OS.isWindows()) {
                    t = WindowsTerminal.class;
                }
                else {
                    t = PosixTerminal.class;
                }
            }
        } catch (Exception e) {
            didFallbackE = e;
            t = UnsupportedTerminal.class;
        }

        Terminal term = null;
        try {
            term = t.newInstance();
        } catch (Throwable e) {
            didFallbackE = e;
            t = UnsupportedTerminal.class;

            try {
                term = t.newInstance();
            } catch (Exception e1) {
                // UnsupportedTerminal can't do this
            }
        }
        terminal = term;


        if (logger.isTraceEnabled()) {
            logger.trace("Creating terminal based on type: " + type);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Created Terminal: {} ({}w x {}h)", InputConsole.terminal.getClass().getSimpleName(),
                         InputConsole.terminal.getWidth(), InputConsole.terminal.getHeight());
        }

        if (didFallbackE != null && !didFallbackE.getMessage().equals(Terminal.CONSOLE_ERROR_INIT)) {
            logger.error("Failed to construct terminal, falling back to unsupported.", didFallbackE);
        } else if (term instanceof UnsupportedTerminal) {
            logger.debug("Terminal is in UNSUPPORTED (best guess). Unable to support single key input. Only line input available.");
        }

        // echo and backspace
        term.setEchoEnabled(Console.ENABLE_ECHO);
        term.setInterruptEnabled(Console.ENABLE_BACKSPACE);

        Thread consoleThread = new Thread(new Runnable() {
            @Override
            public
            void run() {
                InputConsole.run();
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

    private static InputStream wrappedInputStream = new InputStream() {
        @Override
        public
        int read() throws IOException {
            return InputConsole.read();
        }

        @Override
        public
        void close() throws IOException {
            InputConsole.release0();
        }
    };

    public static
    InputStream getInputStream() {
        return wrappedInputStream;
    }

    private
    InputConsole() {
    }

    /**
     * return -1 if no data or bunged-up
     */
    public static
    int read() {
        Integer bufferCounter = threadBufferCounter.get();
        ByteBuffer2 buffer = readBuff.get();

        if (buffer == null) {
            bufferCounter = 0;
            threadBufferCounter.set(bufferCounter);

            try {
                buffer = pool.takeInterruptibly();
                buffer.clear();
            } catch (InterruptedException e) {
                logger.error("Interrupted while receiving buffer from pool.");
                buffer = pool.newInstance();
            }

           readBuff.set(buffer);
           readBuffers.add(buffer);
        }

        if (bufferCounter == buffer.position()) {
            synchronized (inputLockSingle) {
                buffer.setPosition(0);
                threadBufferCounter.set(0);

                try {
                    inputLockSingle.wait();
                } catch (InterruptedException e) {
                    return -1;
                }
            }
        }

        bufferCounter = threadBufferCounter.get();
        char c = buffer.readChar(bufferCounter);
        bufferCounter += 2;

        threadBufferCounter.set(bufferCounter);
        return c;
    }

    /**
     * return empty char[] if no data
     */
    public static
    char[] readLinePassword() {
        // don't bother in an IDE. it won't work.
        boolean echoEnabled = Console.ENABLE_ECHO;
        Console.ENABLE_ECHO = false;
        char[] readLine0 = readLineChars();
        Console.ENABLE_ECHO = echoEnabled;

        return readLine0;
    }

    /**
     * return null if no data
     */
    public static
    String readLine() {
        char[] line = InputConsole.readLineChars();
        if (line == null) {
            return null;
        }
        return new String(line);
    }

    /**
     * return empty char[] if no data
     */
    public static
    char[] readLineChars() {
        synchronized (inputLock) {
            // empty here, because we don't want to register a readLine WHILE we are still processing
            // the current line info.

            // the threadBufferForRead getting added is the part that is important
            if (readLineBuff.get() == null) {
                ByteBuffer2 buffer;
                try {
                    buffer = pool.takeInterruptibly();
                } catch (InterruptedException e) {
                    logger.error("Interrupted while receiving buffer from pool.");
                    buffer = pool.newInstance();
                }

                readLineBuff.set(buffer);
                readLineBuffers.add(buffer);
            }
            else {
                readLineBuff.get().clear();
            }
        }

        synchronized (inputLockLine) {
            try {
                inputLockLine.wait();
            } catch (InterruptedException e) {
                return emptyLine;
            }
        }

        ByteBuffer2 buffer = readLineBuff.get();
        int len = buffer.position();
        if (len == 0) {
            return emptyLine;
        }

        buffer.rewind();
        char[] readChars = buffer.readChars(len / 2); // java always stores chars in 2 bytes

        // dump the chars in the buffer (safer for passwords, etc)
        buffer.clearSecure();

        readLineBuffers.remove(buffer);
        pool.put(buffer);
        readLineBuff.set(null);

        return readChars;
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
        Logger logger2 = logger;

        final boolean ansiEnabled = Ansi.isEnabled();
        Ansi ansi = Ansi.ansi();
//        PrintStream out = AnsiConsole.out;
        PrintStream out = System.out;

        int typedChar;
        char asChar;
        final char overWriteChar = ' ';
        boolean enableBackspace = Console.ENABLE_BACKSPACE;


        // don't type ; in a bash shell, it quits everything
        // \n is replaced by \r in unix terminal?
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
                    for (ByteBuffer2 buffer : readBuffers) {
                        buffer.writeChar(asChar);
                    }

                    inputLockSingle.notifyAll();
                }

                // now to handle readLine stuff

                // if we type a backspace key, swallow it + previous in READLINE. READCHAR will have it passed anyways.
                if (enableBackspace && asChar == '\b') {
                    int position = 0;
                    char[] overwrite = null;

                    // clear ourself + one extra.
                    for (ByteBuffer2 buffer : readLineBuffers) {
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

                    if (ansiEnabled && overwrite != null) {
                        // move back however many, over write, then go back again
                        out.print(ansi.cursorToColumn(position));
                        out.print(overwrite);
                        out.print(ansi.cursorToColumn(position));
                        out.flush();
                    }
                }
                else if (asChar == '\n') {
                    // ignoring \r, because \n is ALWAYS the last character in a new line sequence. (even for windows)
                    synchronized (inputLockLine) {
                        inputLockLine.notifyAll();
                    }
                }
                else {
                    // only append if we are not a new line.
                    // our windows console PREVENTS us from returning '\r' (it truncates '\r\n', and returns just '\n')
                    for (ByteBuffer2 buffer : readLineBuffers) {
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

