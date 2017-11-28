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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import dorkbox.console.Console;
import dorkbox.console.output.Ansi;
import dorkbox.console.util.CharHolder;
import dorkbox.util.FastThreadLocal;
import dorkbox.util.bytes.ByteBuffer2;

public abstract
class SupportedTerminal extends Terminal implements Runnable {
    private final PrintStream out = System.out;

    protected final Object inputLockLine = new Object();
    protected final Object inputLockSingle = new Object();

    protected final List<CharHolder> charInputBuffers = new ArrayList<CharHolder>();
    protected final FastThreadLocal<CharHolder> charInput = new FastThreadLocal<CharHolder>() {
        @Override
        public
        CharHolder initialValue() {
            return new CharHolder();
        }
    };

    private final List<ByteBuffer2> lineInputBuffers = new ArrayList<ByteBuffer2>();
    private final FastThreadLocal<ByteBuffer2> lineInput = new FastThreadLocal<ByteBuffer2>() {
        @Override
        public
        ByteBuffer2 initialValue() {
            return new ByteBuffer2(8, -1);
        }
    };

    public
    SupportedTerminal() {
    }

    /**
     * Reads single character input from the console.
     *
     * @return -1 if no data or problems
     */
    @Override
    public final
    int read() {
        CharHolder holder = charInput.get();

        synchronized (inputLockSingle) {
            // don't want to register a read() WHILE we are still processing the current input.
            // also adds it to the global list of char inputs
            charInputBuffers.add(holder);

            try {
                inputLockSingle.wait();
            } catch (InterruptedException e) {
                return -1;
            }

            char c = holder.character;

            // also clears and removes from the global list of char inputs
            charInputBuffers.remove(holder);
            return c;
        }
    }

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    @Override
    public final
    char[] readLineChars() {
        ByteBuffer2 buffer = lineInput.get();

        synchronized (inputLockLine) {
            // don't want to register a readLine() WHILE we are still processing the current line info.
            // also adds it to the global list of line inputs
            lineInputBuffers.add(buffer);

            try {
                inputLockLine.wait();
            } catch (InterruptedException e) {
                return EMPTY_LINE;
            }

            int len = buffer.position();
            if (len == 0) {
                return EMPTY_LINE;
            }

            buffer.rewind();
            char[] readChars = buffer.readChars(len / 2); // java always stores chars in 2 bytes

            // dump the chars in the buffer (safer for passwords, etc)
            buffer.clearSecure();

            // also clears and removes from the global list of line inputs
            lineInputBuffers.remove(buffer);

            return readChars;
        }
    }

    /**
     * releases any thread still waiting.
     */
    @Override
    public final
    void close() {
        synchronized (inputLockSingle) {
            inputLockSingle.notifyAll();
        }

        synchronized (inputLockLine) {
            inputLockLine.notifyAll();
        }
    }

    /**
     * Reads a single character from whatever underlying stream is available.
     */
    protected abstract int doRead();

    @Override
    public
    void run() {
        final Logger logger2 = logger;
        final char overWriteChar = ' ';

        Ansi ansi = null;
        int typedChar;
        char asChar;

        while ((typedChar = doRead()) != -1) {
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
