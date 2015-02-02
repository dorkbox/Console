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
package dorkbox.util.input;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;

import dorkbox.util.OS;
import dorkbox.util.bytes.ByteBuffer2;
import dorkbox.util.bytes.ByteBuffer2Poolable;
import dorkbox.util.input.posix.UnixTerminal;
import dorkbox.util.input.unsupported.UnsupportedTerminal;
import dorkbox.util.input.windows.WindowsTerminal;
import dorkbox.objectPool.ObjectPool;
import dorkbox.objectPool.ObjectPoolFactory;
import dorkbox.objectPool.ObjectPoolHolder;

public class InputConsole {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InputConsole.class);
    private static final InputConsole consoleProxyReader = new InputConsole();
    private static final char[] emptyLine = new char[0];

    // this is run by our init...
    static {
        AnsiConsole.systemInstall();

        Thread consoleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                consoleProxyReader.run();
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
            public void run() {
                AnsiConsole.systemUninstall();

                consoleProxyReader.shutdown0();
            }
        };
        shutdownThread.setName("Console Input Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    /**
     * Permit our InputConsole to be initialized
     */
    public static void init() {
        if (logger.isDebugEnabled()) {
            logger.debug("Created Terminal: {} ({}w x {}h)", consoleProxyReader.terminal.getClass().getSimpleName(),
                                                             consoleProxyReader.terminal.getWidth(),
                                                             consoleProxyReader.terminal.getHeight());
        }
    }

    /**
     * return null if no data
     */
    public static String readLine() {
        char[] line = consoleProxyReader.readLine0();
        return new String(line);
    }

    private static InputStream wrappedInputStream = new InputStream() {
        @Override
        public int read() throws IOException {
            return consoleProxyReader.read0();
        }

        @Override
        public void close() throws IOException {
            consoleProxyReader.release0();
        }
    };


    /**
     * return -1 if no data
     */
    public static int read() {
        return consoleProxyReader.read0();
    }

    /**
     * return null if no data
     */
    public static char[] readLinePassword() {
        return consoleProxyReader.readLinePassword0();
    }

    public static InputStream getInputStream() {
        return wrappedInputStream;
    }

    public static void echo(boolean enableEcho) {
        consoleProxyReader.echo0(enableEcho);
    }

    public static boolean echo() {
        return consoleProxyReader.echo0();
    }

    private final Object inputLock = new Object();
    private final Object inputLockSingle = new Object();
    private final Object inputLockLine = new Object();

    private final ObjectPool<ByteBuffer2> pool;

    private ThreadLocal<ObjectPoolHolder<ByteBuffer2>> readBuff = new ThreadLocal<ObjectPoolHolder<ByteBuffer2>>();
    private List<ObjectPoolHolder<ByteBuffer2>> readBuffers = new CopyOnWriteArrayList<ObjectPoolHolder<ByteBuffer2>>();
    private ThreadLocal<Integer> threadBufferCounter = new ThreadLocal<Integer>();

    private ThreadLocal<ObjectPoolHolder<ByteBuffer2>> readLineBuff = new ThreadLocal<ObjectPoolHolder<ByteBuffer2>>();
    private List<ObjectPoolHolder<ByteBuffer2>> readLineBuffers = new CopyOnWriteArrayList<ObjectPoolHolder<ByteBuffer2>>();

    private final Terminal terminal;
    private final Boolean enableBackspace;

    private InputConsole() {
        Logger logger = InputConsole.logger;

        String readers = System.getProperty(TerminalType.READERS);
        int readers2 = 32;
        if (readers != null) {
            try {
                readers2 = Integer.parseInt(readers);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.pool = ObjectPoolFactory.create(new ByteBuffer2Poolable(), readers2);

        String type = System.getProperty(TerminalType.TYPE, TerminalType.AUTO).toLowerCase();
        if ("dumb".equals(System.getenv("TERM"))) {
            type = TerminalType.NONE;
            if (logger.isTraceEnabled()) {
                logger.trace("System environment 'TERM'=dumb, creating type=" + type);
            }
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Creating terminal, type=" + type);
            }
        }

        Class<? extends Terminal> t;
        try {
            if (type.equals(TerminalType.UNIX)) {
                t = UnixTerminal.class;
            } else if (type.equals(TerminalType.WIN) || type.equals(TerminalType.WINDOWS)) {
                t = WindowsTerminal.class;
            } else if (type.equals(TerminalType.NONE) || type.equals(TerminalType.OFF) || type.equals(TerminalType.FALSE)) {
                t = UnsupportedTerminal.class;
            } else {
                if (isIDEAutoDetect()) {
                    logger.debug("Terminal is in UNSUPPORTED (best guess). Unable to support single key input. Only line input available.");
                    t = UnsupportedTerminal.class;
                } else {
                    if (OS.isWindows()) {
                        t = WindowsTerminal.class;
                    } else {
                        t = UnixTerminal.class;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to construct terminal, falling back to unsupported.");
            t = UnsupportedTerminal.class;
        }

        Terminal terminal = null;
        try {
            terminal = t.newInstance();
            terminal.init();
        } catch (Throwable e) {
            logger.error("Terminal initialization failed for {}, falling back to unsupported.", t.getSimpleName());
            t = UnsupportedTerminal.class;

            try {
                terminal = t.newInstance();
                terminal.init();
            } catch (Exception e1) {
                // UnsupportedTerminal can't do this
            }
        }

        if (terminal != null) {
            terminal.setEchoEnabled(true);
        }

        this.terminal = terminal;
        this.enableBackspace = Boolean.parseBoolean(System.getProperty(TerminalType.ENABLE_BACKSPACE, "true"));
    }

    // called when the JVM is shutting down.
    private void shutdown0() {
        synchronized (this.inputLockSingle) {
            this.inputLockSingle.notifyAll();
        }

        synchronized (this.inputLockLine) {
            this.inputLockLine.notifyAll();
        }

        try {
            InputConsole inputConsole = InputConsole.this;

            inputConsole.terminal.restore();
            // this will 'hang' our shutdown, and honestly, who cares? We're shutting down anyways.
            // inputConsole.reader.close(); // hangs on shutdown
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
    }

    private void echo0(boolean enableEcho) {
        this.terminal.setEchoEnabled(enableEcho);
    }

    private boolean echo0() {
        return this.terminal.isEchoEnabled();
    }

    /**
     * return -1 if no data or bunged-up
     */
    private int read0() {
        Integer bufferCounter = this.threadBufferCounter.get();
        ObjectPoolHolder<ByteBuffer2> objectPoolHolder = this.readBuff.get();
        ByteBuffer2 buffer;

        if (objectPoolHolder == null) {
            bufferCounter = 0;
            this.threadBufferCounter.set(bufferCounter);

            ObjectPoolHolder<ByteBuffer2> holder = this.pool.take();
            buffer = holder.getValue();
            buffer.clear();
            this.readBuff.set(holder);
            this.readBuffers.add(holder);
        } else {
            buffer = objectPoolHolder.getValue();
        }

        if (bufferCounter == buffer.position()) {
            synchronized (this.inputLockSingle) {
                buffer.setPosition(0);
                this.threadBufferCounter.set(0);

                try {
                    this.inputLockSingle.wait();
                } catch (InterruptedException e) {
                    return -1;
                }
            }
        }

        bufferCounter = this.threadBufferCounter.get();
        char c = buffer.readChar(bufferCounter);
        bufferCounter += 2;

        this.threadBufferCounter.set(bufferCounter);
        return c;
    }

    /**
     * return empty char[] if no data
     */
    private char[] readLinePassword0() {
        // don't bother in an IDE. it won't work.
        boolean echoEnabled = this.terminal.isEchoEnabled();
        this.terminal.setEchoEnabled(false);
        char[] readLine0 = readLine0();
        this.terminal.setEchoEnabled(echoEnabled);

        return readLine0;
    }

    /**
     * return empty char[] if no data
     */
    private char[] readLine0() {
        synchronized (this.inputLock) {
            // empty here, because we don't want to register a readLine WHILE we are still processing
            // the current line info.

            // the threadBufferForRead getting added is the part that is important
            if (this.readLineBuff.get() == null) {
                ObjectPoolHolder<ByteBuffer2> holder = this.pool.take();
                this.readLineBuff.set(holder);
                this.readLineBuffers.add(holder);
            } else {
                this.readLineBuff.get().getValue().clear();
            }
        }

        synchronized (this.inputLockLine) {
            try {
                this.inputLockLine.wait();
            } catch (InterruptedException e) {
                return emptyLine;
            }
        }

        ObjectPoolHolder<ByteBuffer2> objectPoolHolder = this.readLineBuff.get();
        ByteBuffer2 buffer = objectPoolHolder.getValue();
        int len = buffer.position();
        if (len == 0) {
            return emptyLine;
        }

        buffer.rewind();
        char[] readChars = buffer.readChars(len / 2); // java always stores chars in 2 bytes

        // dump the chars in the buffer (safer for passwords, etc)
        buffer.clearSecure();

        this.readLineBuffers.remove(objectPoolHolder);
        this.pool.release(objectPoolHolder);
        this.readLineBuff.set(null);

        return readChars;
    }

    /**
     * releases any thread still waiting.
     */
    private void release0() {
        synchronized (this.inputLockSingle) {
            this.inputLockSingle.notifyAll();
        }

        synchronized (this.inputLockLine) {
            this.inputLockLine.notifyAll();
        }
    }

    private void run() {
        Logger logger2 = logger;

        final boolean ansiEnabled = Ansi.isEnabled();
        Ansi ansi = Ansi.ansi();
        PrintStream out = AnsiConsole.out;

        int typedChar;
        char asChar;

        // don't type ; in a bash shell, it quits everything
        // \n is replaced by \r in unix terminal?
        while ((typedChar = this.terminal.read()) != -1) {
            synchronized (this.inputLock) {
                // don't let anyone add a new reader while we are still processing the current actions
                asChar = (char) typedChar;

                if (logger2.isTraceEnabled()) {
                    logger2.trace("READ: {} ({})", asChar, typedChar);
                }

                // notify everyone waiting for a character.
                synchronized (this.inputLockSingle) {
                    // have to do readChar first (readLine has to deal with \b and \n
                    for (ObjectPoolHolder<ByteBuffer2> objectPoolHolder : this.readBuffers) {
                        ByteBuffer2 buffer = objectPoolHolder.getValue();
                        buffer.writeChar(asChar);
                    }

                    this.inputLockSingle.notifyAll();
                }

                // now to handle readLine stuff

                // if we type a backspace key, swallow it + previous in READLINE. READCHAR will have it passed.
                if (this.enableBackspace && asChar == '\b') {
                    int position = 0;

                    // clear ourself + one extra.
                    if (ansiEnabled) {
                        for (ObjectPoolHolder<ByteBuffer2> objectPoolHolder : this.readLineBuffers) {
                            ByteBuffer2 buffer = objectPoolHolder.getValue();
                            // size of the buffer BEFORE our backspace was typed
                            int length = buffer.position();
                            int amtToOverwrite = 2 * 2; // backspace is always 2 chars (^?) * 2 because it's bytes

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

                            char[] overwrite = new char[amtToOverwrite];
                            char c = ' ';
                            for (int i = 0; i < amtToOverwrite; i++) {
                                overwrite[i] = c;
                            }

                            // move back however many, over write, then go back again
                            out.print(ansi.cursorToColumn(position));
                            out.print(overwrite);
                            out.print(ansi.cursorToColumn(position));
                            out.flush();
                        }
                    }
                } else if (asChar == '\n') {
                    // ignoring \r, because \n is ALWAYS the last character in a new line sequence. (even for windows)
                    synchronized (this.inputLockLine) {
                        this.inputLockLine.notifyAll();
                    }
                } else {
                    // only append if we are not a new line.
                    // our windows console PREVENTS us from returning '\r' (it truncates '\r\n', and returns just '\n')
                    for (ObjectPoolHolder<ByteBuffer2> objectPoolHolder : this.readLineBuffers) {
                        ByteBuffer2 buffer = objectPoolHolder.getValue();
                        buffer.writeChar(asChar);
                    }
                }
            }
        }
    }

    /**
     * try to guess if we are running inside an IDE
     */
    private boolean isIDEAutoDetect() {
        try {
            // Get the location of this class
            ProtectionDomain pDomain = getClass().getProtectionDomain();
            CodeSource cSource = pDomain.getCodeSource();
            URL loc = cSource.getLocation(); // file:/X:/workspace/xxxx/classes/ when it's in eclipse

            // if we are in eclipse, this won't be a jar -- it will be a class directory.
            File locFile = new File(loc.getFile());
            return locFile.isDirectory();

        } catch (Exception ignored) {
        }

        // fall-back to unsupported
        return true;
    }


    /**
     * Return the number of characters that will be printed when the specified character is echoed to the screen
     *
     * Adapted from cat by Torbjorn Granlund, as repeated in stty by David MacKenzie.
     */
    private static int getPrintableCharacters(final int ch) {
        // StringBuilder sbuff = new StringBuilder();

        if (ch >= 32) {
            if (ch < 127) {
                // sbuff.append((char) ch);
                return 1;
            } else if (ch == 127) {
                // sbuff.append('^');
                // sbuff.append('?');
                return 2;
            } else {
                // sbuff.append('M');
                // sbuff.append('-');
                int count = 2;

                if (ch >= 128 + 32) {
                    if (ch < 128 + 127) {
                        // sbuff.append((char) (ch - 128));
                        count++;
                    } else {
                        // sbuff.append('^');
                        // sbuff.append('?');
                        count += 2;
                    }
                } else {
                    // sbuff.append('^');
                    // sbuff.append((char) (ch - 128 + 64));
                    count += 2;
                }
                return count;
            }
        } else {
            // sbuff.append('^');
            // sbuff.append((char) (ch + 64));
            return 2;
        }

        // return sbuff;
    }
}
