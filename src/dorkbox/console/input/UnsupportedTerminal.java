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

import dorkbox.util.FastThreadLocal;
import dorkbox.util.bytes.ByteBuffer2;

@SuppressWarnings("Duplicates")
public
class UnsupportedTerminal extends Terminal {

    private static final char[] newLine;
    static {
        newLine = new char[1];
        newLine[0] = '\n';
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
     * Reads single character input from the console.
     *
     * @return -1 if no data or problems
     */
    public
    int read() {
        // so, 'readCount' is REALLY the index at which we return letters (until the whole string is returned)
        ByteBuffer2 buffer = this.buffer.get();
        if (this.readCount.get() == 0) {
            // we have to wait for more data.
            try {
                InputStream sysIn = System.in;
                int read;
                char asChar;
                buffer.clearSecure();

                while ((read = sysIn.read()) != -1) {
                    asChar = (char) read;
                    if (asChar == '\n') {
                        int position = buffer.position();
                        this.readCount.set(position);
                        if (position == 0) {
                            // only send a NEW LINE if it was the ONLY thing pressed (this is to MOST ACCURATELY simulate single char input
                            return '\n';
                        }

                        buffer.rewind();
                        break;
                    }
                    else {
                        buffer.writeChar(asChar);
                    }
                }
            } catch (IOException ignored) {
            }
        }

//        // EACH thread will have it's own count!
//        if (this.readCount == buffer.position()) {
//            this.readCount = -1;
//            return '\n';
//        }
//        else {
//            return buffer.readChar();
//        }
        readCount.set(this.readCount.get() - 2); // 2 bytes per char in the stream
        return buffer.readChar();
    }

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    public
    char[] readLineChars() {
        ByteBuffer2 buffer = this.buffer.get();
        buffer.clearSecure();

        int position = 0;
        // we have to wait for more data.
        try {
            final InputStream sysIn = System.in;
            int read;
            char asChar;

            while ((read = sysIn.read()) != -1) {
                asChar = (char) read;

                if (asChar == '\n') {
                    buffer.rewind();
                    break;
                }
                buffer.writeChar(asChar);
                position = buffer.position();
            }
        } catch (IOException ignored) {
        }

        if (position == 0) {
            // only send a NEW LINE if it was the ONLY thing pressed (this is to MOST ACCURATELY simulate single char input
            return newLine;
        }

        char[] chars = buffer.readChars(position/2); // 2 bytes per char
        buffer.clearSecure();

        return chars;
    }

    @Override
    public
    void close() {
    }
}
