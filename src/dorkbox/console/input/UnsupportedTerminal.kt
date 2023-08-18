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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.locks.*
import kotlin.concurrent.withLock

class UnsupportedTerminal : Terminal() {
    private val buffer: ThreadLocal<ByteArrayBuffer> = object : ThreadLocal<ByteArrayBuffer>() {
        public override fun initialValue(): ByteArrayBuffer {
            return ByteArrayBuffer(8, -1)
        }
    }
    private val readCount: ThreadLocal<Int> = object : ThreadLocal<Int>() {
        public override fun initialValue(): Int {
            return 0
        }
    }

    override fun doSetInterruptEnabled(enabled: Boolean) {}
    override fun doSetEchoEnabled(enabled: Boolean) {}
    override fun restore() {}

    override val width: Int
        get() {
            return 0
        }

    override val height: Int
        get () {
            return 0
        }

    /**
     * Reads single character input from the console. This is "faked" by reading a line
     *
     * @return -1 if no data or problems
     */
    override fun read(): Int {
        val position: Int
        // so, 'readCount' is REALLY the index at which we return letters (until the whole string is returned)
        val buffer = buffer.get()
        buffer.clearSecure()

        // we have to wait for more data.
        if (readCount.get() == 0) {
            try {
                lock.withLock {
                    condition.await()
                }
            }
            catch (ignored: Exception) {
            }

            if (currentConsoleInput == null) {
                return -1
            }

            val chars = currentConsoleInput!!.toCharArray()
            buffer.writeChars(chars)
            position = buffer.position()
            buffer.rewind()

            readCount.set(position)
            if (position == 0) {
                // only send a NEW LINE if it was the ONLY thing pressed (this is to MOST ACCURATELY simulate single char input
                return '\n'.code
            }
            buffer.rewind()
        }

        readCount.set(readCount.get() - 2) // 2 bytes per char in the stream
        return buffer.readChar().code
    }

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    override fun readLineChars(): CharArray {
        // we have to wait for more data.
        try {
            lock.withLock {
                condition.await()
            }
        }
        catch (ignored: Exception) {
        }

        if (currentConsoleInput == null) {
            return EMPTY_LINE
        }

        val chars = currentConsoleInput!!.toCharArray()
        val length = chars.size
        return if (length == 0) {
            // only send a NEW LINE if it was the ONLY thing pressed (this is to MOST ACCURATELY simulate single char input
            NEW_LINE
        }
        else chars
    }

    override fun close() {
        lock.withLock {
            condition.signalAll()
        }
    }

    companion object {
        private val NEW_LINE: CharArray
        private val backgroundReaderThread: Thread

        private val lock = ReentrantLock()
        private val condition = lock.newCondition()

        private var currentConsoleInput: String? = null

        init {
            NEW_LINE = CharArray(1)
            NEW_LINE[0] = '\n'

            // this adopts a different thread + locking to enable reader threads to "unblock" after the blocking read.
            // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4514257
            // https://community.oracle.com/message/5318833#5318833
            backgroundReaderThread = Thread {
                val reader = BufferedReader(InputStreamReader(System.`in`))
                try {
                    while (!Thread.interrupted()) {
                        currentConsoleInput = null
                        val line = reader.readLine() ?: break

                        lock.withLock {
                            currentConsoleInput = line
                            condition.signalAll()
                        }
                    }
                }
                catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
            backgroundReaderThread.setDaemon(true)
            backgroundReaderThread.start()
        }
    }
}
