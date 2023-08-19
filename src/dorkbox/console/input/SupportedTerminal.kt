/*
 * Copyright 2023 dorkbox, llc
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
package dorkbox.console.input

import dorkbox.bytes.ByteArrayBuffer
import dorkbox.console.Console
import dorkbox.console.output.Ansi
import dorkbox.console.util.CharHolder
import java.util.concurrent.locks.*
import kotlin.concurrent.withLock

abstract class SupportedTerminal : Terminal(), Runnable {
    private val out = System.out

    private val inputLockLine = ReentrantLock()
    private val inputLockLineCondition = inputLockLine.newCondition()

    private val inputLockSingle = ReentrantLock()
    private val inputLockSingleCondition = inputLockSingle.newCondition()


    private val charInputBuffers: MutableList<CharHolder> = ArrayList()
    private val charInput: ThreadLocal<CharHolder> = object : ThreadLocal<CharHolder>() {
        public override fun initialValue(): CharHolder {
            return CharHolder()
        }
    }

    private val lineInputBuffers: MutableList<ByteArrayBuffer> = ArrayList()
    private val lineInput: ThreadLocal<ByteArrayBuffer> = object : ThreadLocal<ByteArrayBuffer>() {
        public override fun initialValue(): ByteArrayBuffer {
            return ByteArrayBuffer(8, -1)
        }
    }

    /**
     * Reads single character input from the console.
     *
     * @return -1 if no data or problems
     */
    override fun read(): Int {
        val holder = charInput.get()

        inputLockSingle.withLock {
            // don't want to register a read() WHILE we are still processing the current input.
            // also adds it to the global list of char inputs
            charInputBuffers.add(holder)

            try {
                inputLockSingleCondition.await()
            }
            catch (e: InterruptedException) {
                return -1
            }
            val c = holder.character

            // also clears and removes from the global list of char inputs
            charInputBuffers.remove(holder)
            return c.code
        }
    }

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    override fun readLineChars(): CharArray {
        val buffer = lineInput.get()

        inputLockLine.withLock {
            // don't want to register a readLine() WHILE we are still processing the current line info.
            // also adds it to the global list of line inputs
            lineInputBuffers.add(buffer)

            try {
                inputLockLineCondition.await()
            }
            catch (e: InterruptedException) {
                return EMPTY_LINE
            }

            // removes from the global list of line inputs
            lineInputBuffers.remove(buffer)

            val len = buffer.position()
            if (len == 0) {
                return EMPTY_LINE
            }

            buffer.rewind()
            val readChars = buffer.readChars(len / 2) // java always stores chars in 2 bytes

            // dump the chars in the buffer (safer for passwords, etc)
            buffer.clearSecure()


            return readChars
        }
    }

    /**
     * releases any thread still waiting.
     */
    override fun close() {
        inputLockSingle.withLock {
            inputLockSingleCondition.signalAll()
        }
        inputLockLine.withLock {
            inputLockLineCondition.signalAll()
        }
    }

    /**
     * Reads a single character from whatever underlying stream is available.
     */
    protected abstract fun doRead(): Int

    override fun run() {
        val logger2 = logger
        val overWriteChar = ' '
        var ansi: Ansi? = null
        var typedChar: Int
        var asChar: Char

        while (doRead().also { typedChar = it } != -1) {
            // don't let anyone add a new reader while we are still processing the current actions
            asChar = typedChar.toChar()
            if (logger2.isTraceEnabled) {
                logger2.trace("READ: {} ({})", asChar, typedChar)
            }

            // notify everyone waiting for a character.
            inputLockSingle.withLock {
                // have to do readChar first (readLine has to deal with \b and \n
                for (holder in charInputBuffers) {
                    holder.character = asChar // copy by value
                }

                inputLockSingleCondition.signalAll()
            }

            // now to handle readLine stuff

            // if we type a backspace key, swallow it + previous in READLINE. READCHAR will have it passed anyways.
            if (Console.ENABLE_BACKSPACE && (asChar == '\b' || asChar == '\u007F')) {
                var position = 0
                var overwrite: CharArray? = null

                // clear ourself + one extra.
                inputLockLine.withLock {
                    for (buffer in lineInputBuffers) {
                        // size of the buffer BEFORE our backspace was typed
                        var length = buffer.position()
                        var amtToOverwrite = 4 // 2*2 backspace is always 2 chars (^?) * 2 because it's bytes
                        if (length > 1) {
                            var charAt = buffer.readChar(length - 2)
                            amtToOverwrite += getPrintableCharacters(charAt.code)

                            // delete last item in our buffer
                            length -= 2
                            buffer.setPosition(length)

                            // now figure out where the cursor is really at.
                            // this is more memory friendly than buf.toString.length
                            var i = 0
                            while (i < length) {
                                charAt = buffer.readChar(i)
                                position += getPrintableCharacters(charAt.code)
                                i += 2
                            }
                            position++
                        }
                        overwrite = CharArray(amtToOverwrite)

                        for (i in 0 until amtToOverwrite) {
                            overwrite!![i] = overWriteChar
                        }
                    }
                }


                if (Console.ENABLE_ANSI && overwrite != null) {
                    if (ansi == null) {
                        ansi = Ansi.ansi()
                    }

                    // move back however many, overwrite, then go back again
                    out.print(ansi.cursorToColumn(position))
                    out.print(overwrite!!)
                    out.print(ansi.cursorToColumn(position))
                    out.flush()
                }
            }
            else if (asChar == '\n') {
                // ignoring \r, because \n is ALWAYS the last character in a new line sequence. (even for windows, which we changed)
                inputLockLine.withLock {
                    inputLockLineCondition.signalAll()
                }
            }
            else {
                // only append if we are not a new line.
                // our windows console PREVENTS us from returning '\r' (it truncates '\r\n', and returns just '\n')
                inputLockLine.withLock {
                    for (buffer in lineInputBuffers) {
                        buffer.writeChar(asChar)
                    }
                }
            }
        }
    }

    companion object {
        private const val PLUS_TWO_MAYBE = 128 + 32
        private const val PLUS_ONE = 128 + 127

        /**
         * Return the number of characters that will be printed when the specified character is echoed to the screen
         *
         *
         * Adapted from cat by Torbjorn Granlund, as repeated in stty by David MacKenzie.
         */
        private fun getPrintableCharacters(ch: Int): Int {
            // StringBuilder sbuff = new StringBuilder();
            return if (ch >= 32) {
                if (ch < 127) {
                    // sbuff.append((char) ch);
                    1
                }
                else if (ch == 127) {
                    // sbuff.append('^');
                    // sbuff.append('?');
                    2
                }
                else {
                    // sbuff.append('M');
                    // sbuff.append('-');
                    var count = 2
                    if (ch >= PLUS_TWO_MAYBE) {
                        if (ch < PLUS_ONE) {
                            // sbuff.append((char) (ch - 128));
                            count++
                        }
                        else {
                            // sbuff.append('^');
                            // sbuff.append('?');
                            count += 2
                        }
                    }
                    else {
                        // sbuff.append('^');
                        // sbuff.append((char) (ch - 128 + 64));
                        count += 2
                    }
                    count
                }
            }
            else {
                // sbuff.append('^');
                // sbuff.append((char) (ch + 64));
                2
            }

            // return sbuff;
        }
    }
}
