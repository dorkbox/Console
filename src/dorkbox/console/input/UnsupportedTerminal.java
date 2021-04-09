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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import dorkbox.util.FastThreadLocal;
import dorkbox.bytes.ByteBuffer2;

@SuppressWarnings("Duplicates")
public
class UnsupportedTerminal extends Terminal {

    private static final char[] NEW_LINE;
    private static final Thread backgroundReaderThread;

    private static final Object lock = new Object[0];
    private static String currentConsoleInput = null;



    static {
        NEW_LINE = new char[1];
        NEW_LINE[0] = '\n';

        // this adopts a different thread + locking to enable reader threads to "unblock" after the blocking read.
        // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4514257
        // https://community.oracle.com/message/5318833#5318833
        backgroundReaderThread = new Thread(new Runnable() {
            @Override
            public
            void run() {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

                try {
                    while (!Thread.interrupted()) {
                        currentConsoleInput = null;

                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }

                        synchronized (lock) {
                            currentConsoleInput = line;
                            lock.notifyAll();
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        backgroundReaderThread.setDaemon(true);
        backgroundReaderThread.start();
    }

    private final FastThreadLocal<ByteBuffer2> buffer = new FastThreadLocal<ByteBuffer2>() {
        @Override
        public
        ByteBuffer2 initialValue() {
            return new ByteBuffer2(8, -1);
        }
    };

    private final FastThreadLocal<Integer> readCount = new FastThreadLocal<Integer>() {
        @Override
        public
        Integer initialValue() {
            return 0;
        }
    };

    public
    UnsupportedTerminal() {
    }

    @Override
    protected
    void doSetInterruptEnabled(final boolean enabled) {
    }

    @Override
    protected
    void doSetEchoEnabled(final boolean enabled) {
    }

    @Override
    public final
    void restore() {
    }

    @Override
    public final
    int getWidth() {
        return 0;
    }

    @Override
    public final
    int getHeight() {
        return 0;
    }

    /**
     * Reads single character input from the console. This is "faked" by reading a line
     *
     * @return -1 if no data or problems
     */
    @Override
    public
    int read() {
        int position;
        // so, 'readCount' is REALLY the index at which we return letters (until the whole string is returned)
        ByteBuffer2 buffer = this.buffer.get();
        buffer.clearSecure();

        // we have to wait for more data.
        if (this.readCount.get() == 0) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                }
            }

            if (currentConsoleInput == null) {
                return -1;
            }


            char[] chars = currentConsoleInput.toCharArray();
            buffer.writeChars(chars);
            position = buffer.position();
            buffer.rewind();

            this.readCount.set(position);
            if (position == 0) {
                // only send a NEW LINE if it was the ONLY thing pressed (this is to MOST ACCURATELY simulate single char input
                return '\n';
            }

            buffer.rewind();
        }

        readCount.set(this.readCount.get() - 2); // 2 bytes per char in the stream
        return buffer.readChar();
    }

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    @Override
    public
    char[] readLineChars() {
        // we have to wait for more data.
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException ignored) {
            }
        }

        if (currentConsoleInput == null) {
            return EMPTY_LINE;
        }


        char[] chars = currentConsoleInput.toCharArray();
        int length = chars.length;

        if (length == 0) {
            // only send a NEW LINE if it was the ONLY thing pressed (this is to MOST ACCURATELY simulate single char input
            return NEW_LINE;
        }

        return chars;
    }

    @Override
    public
    void close() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
