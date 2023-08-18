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

import dorkbox.console.Console
import org.slf4j.LoggerFactory
import java.io.IOException

@Suppress("unused")
abstract class Terminal internal constructor() {
    companion object {
        val EMPTY_LINE = CharArray(0)

        const val CONSOLE_ERROR_INIT = "Unable to initialize the input console."
        const val DEFAULT_WIDTH = 80
        const val DEFAULT_HEIGHT = 24
    }

    val logger = LoggerFactory.getLogger(javaClass)

    abstract fun doSetInterruptEnabled(enabled: Boolean)
    protected abstract fun doSetEchoEnabled(enabled: Boolean)

    @Throws(IOException::class)
    abstract fun restore()

    abstract val width: Int
    abstract val height: Int

    /**
     * Enables or disables CTRL-C behavior in the console
     */
    fun setInterruptEnabled(enabled: Boolean) {
        Console.ENABLE_INTERRUPT = enabled
        doSetInterruptEnabled(enabled)
    }

    /**
     * Enables or disables character echo to stdout
     */
    fun setEchoEnabled(enabled: Boolean) {
        Console.ENABLE_ECHO = enabled
        doSetEchoEnabled(enabled)
    }

    /**
     * Reads single character input from the console.
     *
     * @return -1 if no data or problems
     */
    abstract fun read(): Int

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    abstract fun readLineChars(): CharArray

    /**
     * Reads a single line of characters, defined as everything before the 'ENTER' key is pressed
     *
     * @return null if no data
     */
    fun readLine(): String? {
        val line = readLineChars() ?: return null
        return String(line)
    }

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    fun readLinePassword(): CharArray {
        // don't bother in an IDE. it won't work.
        val echoEnabled = Console.ENABLE_ECHO
        Console.ENABLE_ECHO = false

        val readLine0 = readLineChars()
        Console.ENABLE_ECHO = echoEnabled
        return readLine0
    }

    /**
     * releases any thread still waiting.
     */
    abstract fun close()
}
