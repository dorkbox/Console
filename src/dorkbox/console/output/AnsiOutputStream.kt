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
package dorkbox.console.output

import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset

/**
 * A ANSI output stream extracts ANSI escape codes written to an output stream.
 *
 * For more information about ANSI escape codes, see:
 * http://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * This class just filters out the escape codes so that they are not sent out to the underlying OutputStream.  Subclasses should
 * actually perform the ANSI escape behaviors.
 *
 * @author dorkbox, llc
 * @author [Hiram Chirino](http://hiramchirino.com)
 * @author Joris Kuipers
 */
open class AnsiOutputStream(os: OutputStream?) : FilterOutputStream(os) {
    private var state = STATE.LOOKING_FOR_FIRST_ESC_CHAR
    private val buffer = ByteArray(MAX_ESCAPE_SEQUENCE_LENGTH)

    private var pos = 0
    private var startOfValue = 0

    private val options = ArrayList<Any?>()

    @Throws(IOException::class)
    override fun write(data: Int) {
        when (state) {
            STATE.LOOKING_FOR_FIRST_ESC_CHAR  -> {
                if (data == FIRST_ESC_CHAR) {
                    buffer[pos++] = data.toByte()
                    state = STATE.LOOKING_FOR_SECOND_ESC_CHAR
                }
                else {
                    out.write(data)
                }
            }

            STATE.LOOKING_FOR_SECOND_ESC_CHAR -> {
                buffer[pos++] = data.toByte()
                if (data == SECOND_ESC_CHAR) {
                    state = STATE.LOOKING_FOR_NEXT_ARG
                }
                else if (data == SECOND_OSC_CHAR) {
                    state = STATE.LOOKING_FOR_OSC_COMMAND
                }
                else {
                    reset(false)
                }
            }

            STATE.LOOKING_FOR_NEXT_ARG        -> {
                buffer[pos++] = data.toByte()

                if ('"'.code == data) {
                    startOfValue = pos - 1
                    state = STATE.LOOKING_FOR_STR_ARG_END
                }
                else if ('0'.code <= data && data <= '9'.code) {
                    startOfValue = pos - 1
                    state = STATE.LOOKING_FOR_INT_ARG_END
                }
                else if (';'.code == data) {
                    options.add(null)
                }
                else if ('?'.code == data) {
                    options.add('?')
                }
                else if ('='.code == data) {
                    options.add('=')
                }
                else {
                    reset(processEscapeCommand(options, data.toChar()))
                }
            }

            STATE.LOOKING_FOR_INT_ARG_END     -> {
                buffer[pos++] = data.toByte()

                if (!('0'.code <= data && data <= '9'.code)) {
                    val strValue = String(buffer, startOfValue, pos - 1 - startOfValue, CHARSET)
                    val value = strValue.toInt()
                    options.add(value)

                    if (data == ';'.code) {
                        state = STATE.LOOKING_FOR_NEXT_ARG
                    }
                    else {
                        reset(processEscapeCommand(options, data.toChar()))
                    }
                }
            }

            STATE.LOOKING_FOR_STR_ARG_END     -> {
                buffer[pos++] = data.toByte()

                if ('"'.code != data) {
                    val value = String(buffer, startOfValue, pos - 1 - startOfValue, CHARSET)
                    options.add(value)

                    if (data == ';'.code) {
                        state = STATE.LOOKING_FOR_NEXT_ARG
                    }
                    else {
                        reset(processEscapeCommand(options, data.toChar()))
                    }
                }
            }

            STATE.LOOKING_FOR_OSC_COMMAND     -> {
                buffer[pos++] = data.toByte()

                if ('0'.code <= data && data <= '9'.code) {
                    startOfValue = pos - 1
                    state = STATE.LOOKING_FOR_OSC_COMMAND_END
                }
                else {
                    reset(false)
                }
            }

            STATE.LOOKING_FOR_OSC_COMMAND_END -> {
                buffer[pos++] = data.toByte()

                if (';'.code == data) {
                    val strValue = String(buffer, startOfValue, pos - 1 - startOfValue, CHARSET)
                    val value = strValue.toInt()
                    options.add(value)
                    startOfValue = pos
                    state = STATE.LOOKING_FOR_OSC_PARAM
                }
                else if ('0'.code <= data && data <= '9'.code) {
                    // already pushed digit to buffer, just keep looking
                }
                else {
                    // oops, did not expect this
                    reset(false)
                }
            }

            STATE.LOOKING_FOR_OSC_PARAM       -> {
                buffer[pos++] = data.toByte()

                if (BEL == data) {
                    val value = String(buffer, startOfValue, pos - 1 - startOfValue, CHARSET)
                    options.add(value)
                    reset(processOperatingSystemCommand(options))
                }
                else if (FIRST_ESC_CHAR == data) {
                    state = STATE.LOOKING_FOR_ST
                }
                else {
                    // just keep looking while adding text
                }
            }

            STATE.LOOKING_FOR_ST              -> {
                buffer[pos++] = data.toByte()

                if (SECOND_ST_CHAR == data) {
                    val value = String(buffer, startOfValue, pos - 2 - startOfValue, CHARSET)
                    options.add(value)
                    reset(processOperatingSystemCommand(options))
                }
                else {
                    state = STATE.LOOKING_FOR_OSC_PARAM
                }
            }
        }

        // Is it just too long?
        if (pos >= buffer.size) {
            reset(false)
        }
    }

    /**
     * Resets all state to continue with regular parsing
     *
     * @param skipBuffer if current buffer should be skipped or written to out
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun reset(skipBuffer: Boolean) {
        if (!skipBuffer) {
            out.write(buffer, 0, pos)
        }
        pos = 0
        startOfValue = 0
        options.clear()
        state = STATE.LOOKING_FOR_FIRST_ESC_CHAR
    }

    /**
     * @return true if the escape command was processed.
     */
    @Throws(IOException::class)
    private fun processEscapeCommand(options: ArrayList<Any?>, command: Char): Boolean {
        try {
            return when (command) {
                CURSOR_UP                  -> {
                    processCursorUp(optionInt(options, 0, 1))
                    true
                }

                CURSOR_DOWN                -> {
                    processCursorDown(optionInt(options, 0, 1))
                    true
                }

                CURSOR_FORWARD             -> {
                    processCursorRight(optionInt(options, 0, 1))
                    true
                }

                CURSOR_BACK                -> {
                    processCursorLeft(optionInt(options, 0, 1))
                    true
                }

                CURSOR_DOWN_LINE           -> {
                    processCursorDownLine(optionInt(options, 0, 1))
                    true
                }

                CURSOR_UP_LINE             -> {
                    processCursorUpLine(optionInt(options, 0, 1))
                    true
                }

                CURSOR_TO_COL              -> {
                    processCursorToColumn(optionInt(options, 0))
                    true
                }

                CURSOR_POS, CURSOR_POS_ALT -> {
                    processCursorTo(optionInt(options, 0, 1), optionInt(options, 1, 1))
                    true
                }

                CURSOR_ERASE_SCREEN        -> {
                    processEraseScreen(optionInt(options, 0, 0))
                    true
                }

                CURSOR_ERASE_LINE          -> {
                    processEraseLine(optionInt(options, 0, 0))
                    true
                }

                SCROLL_UP                  -> {
                    processScrollUp(optionInt(options, 0, 1))
                    true
                }

                SCROLL_DOWN                -> {
                    processScrollDown(optionInt(options, 0, 1))
                    true
                }

                TEXT_ATTRIBUTE             -> {
                    var count = 0
                    for (next in options) {
                        if (next != null) {
                            count++

                            // will throw a ClassCast exception IF NOT an int.
                            val value = next as Int
                            if (30 <= value && value <= 37) {
                                // foreground
                                processSetForegroundColor(value - 30)
                            }
                            else if (40 <= value && value <= 47) {
                                // background
                                processSetBackgroundColor(value - 40)
                            }
                            else {
                                when (value) {
                                    ATTRIBUTE_DEFAULT_FG -> processDefaultTextColor()
                                    ATTRIBUTE_DEFAULT_BG -> processDefaultBackgroundColor()
                                    ATTRIBUTE_RESET      -> processAttributeReset()
                                    else                 -> processSetAttribute(value)
                                }
                            }
                        }
                    }
                    if (count == 0) {
                        processAttributeReset()
                    }
                    true
                }

                SAVE_CURSOR_POS            -> {
                    processSaveCursorPosition()
                    true
                }

                RESTORE_CURSOR_POS         -> {
                    processRestoreCursorPosition()
                    true
                }

                else                       -> {
                    if (command in 'a'..'z') {
                        processUnknownExtension(options, command)
                        return true
                    }
                    if (command in 'A'..'Z') {
                        processUnknownExtension(options, command)
                        return true
                    }
                    false
                }
            }
        }
        catch (ignore: IllegalArgumentException) {
        }
        return false
    }

    /**
     * @return true if the operating system command was processed.
     */
    @Throws(IOException::class)
    private fun processOperatingSystemCommand(options: ArrayList<Any?>): Boolean {
        val command = optionInt(options, 0)
        val label = options[1] as String?

        // for command > 2 label could be composed (i.e. contain ';'), but we'll leave
        // it to processUnknownOperatingSystemCommand implementations to handle that
        try {
            return when (command) {
                else -> {
                    // not exactly unknown, but not supported through dedicated process methods
                    processUnknownOperatingSystemCommand(command, label)
                    true
                }
            }
        }
        catch (ignore: IllegalArgumentException) {
        }
        return false
    }

    @Throws(IOException::class)
    protected open fun processRestoreCursorPosition() {
    }

    @Throws(IOException::class)
    protected open fun processSaveCursorPosition() {
    }

    @Throws(IOException::class)
    protected open fun processScrollDown(count: Int) {
    }

    @Throws(IOException::class)
    protected open fun processScrollUp(count: Int) {
    }

    @Throws(IOException::class)
    protected open fun processEraseScreen(eraseOption: Int) {
    }

    @Throws(IOException::class)
    protected open fun processEraseLine(eraseOption: Int) {
    }

    @Throws(IOException::class)
    protected open fun processSetAttribute(attribute: Int) {
    }

    @Throws(IOException::class)
    protected open fun processSetForegroundColor(color: Int) {
    }

    @Throws(IOException::class)
    protected open fun processSetBackgroundColor(color: Int) {
    }

    @Throws(IOException::class)
    protected open fun processDefaultTextColor() {
    }

    @Throws(IOException::class)
    protected open fun processDefaultBackgroundColor() {
    }

    @Throws(IOException::class)
    protected open fun processAttributeReset() {
    }

    @Throws(IOException::class)
    protected open fun processCursorTo(row: Int, col: Int) {
    }

    @Throws(IOException::class)
    protected open fun processCursorToColumn(x: Int) {
    }

    @Throws(IOException::class)
    protected open fun processCursorUpLine(count: Int) {
    }

    @Throws(IOException::class)
    protected open fun processCursorDownLine(count: Int) {
    }

    @Throws(IOException::class)
    protected open fun processCursorLeft(count: Int) {
    }

    @Throws(IOException::class)
    protected open fun processCursorRight(count: Int) {
    }

    @Throws(IOException::class)
    protected open fun processCursorDown(count: Int) {
    }

    @Throws(IOException::class)
    protected open fun processCursorUp(count: Int) {
    }

    protected fun processUnknownExtension(options: ArrayList<Any?>, command: Char) {}
    protected fun processUnknownOperatingSystemCommand(command: Int, param: String?) {}

    private fun optionInt(options: ArrayList<Any?>, index: Int): Int {
        require(options.size > index)
        val value = options[index] ?: throw IllegalArgumentException()

        require(value.javaClass == Int::class.java)
        return value as Int
    }

    private fun optionInt(options: ArrayList<Any?>, index: Int, defaultValue: Int): Int {
        if (options.size > index) {
            val value = options[index] ?: return defaultValue
            return value as Int
        }
        return defaultValue
    }

    @Throws(IOException::class)
    override fun close() {
        flush()
        super.close()
    }

    companion object {
        private val CHARSET = Charset.forName("UTF-8")

        const val BLACK = 0
        const val RED = 1
        const val GREEN = 2
        const val YELLOW = 3
        const val BLUE = 4
        const val MAGENTA = 5
        const val CYAN = 6
        const val WHITE = 7

        // Moves the cursor n (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
        const val CURSOR_UP = 'A'
        const val CURSOR_DOWN = 'B'
        const val CURSOR_FORWARD = 'C'
        const val CURSOR_BACK = 'D'
        const val CURSOR_DOWN_LINE = 'E' // Moves cursor to beginning of the line n (default 1) lines down.
        const val CURSOR_UP_LINE = 'F' // Moves cursor to beginning of the line n (default 1) lines up.
        const val CURSOR_TO_COL = 'G' // Moves the cursor to column n (default 1).

        // Moves the cursor to row n, column m. The values are 1-based, and default to 1 (top left corner) if omitted.
        const val CURSOR_POS = 'H'

        // Moves the cursor to row n, column m. Both default to 1 if omitted. Same as CUP
        const val CURSOR_POS_ALT = 'f'

        // Clears part of the screen. If n is 0 (or missing), clear from cursor to end of screen. If n is 1, clear from cursor to beginning of the screen. If n is 2, clear entire screen (and moves cursor to upper left on DOS ANSI.SYS).
        const val CURSOR_ERASE_SCREEN = 'J'

        // Erases part of the line. If n is zero (or missing), clear from cursor to the end of the line. If n is one, clear from cursor to beginning of the line. If n is two, clear entire line. Cursor position does not change.
        const val CURSOR_ERASE_LINE = 'K'

        const val SCROLL_UP = 'S' // Scroll whole page up by n (default 1) lines. New lines are added at the bottom. (not ANSI.SYS)
        const val SCROLL_DOWN = 'T' // Scroll whole page down by n (default 1) lines. New lines are added at the top. (not ANSI.SYS)
        const val SAVE_CURSOR_POS = 's' // Saves the cursor position.
        const val RESTORE_CURSOR_POS = 'u' // Restores the cursor position.

        // Sets SGR parameters, including text color. After CSI can be zero or more parameters separated with ;. With no parameters, CSI m is treated as CSI 0 m (reset / normal), which is typical of most of the ANSI escape sequences.
        const val TEXT_ATTRIBUTE = 'm'

        const val ATTRIBUTE_RESET = 0 // Reset / Normal - all attributes off
        const val ATTRIBUTE_BOLD = 1 // Intensity: Bold
        const val ATTRIBUTE_FAINT = 2 // Intensity; Faint (not widely supported)
        const val ATTRIBUTE_ITALIC = 3 // Italic; (on not widely supported. Sometimes treated as inverse)
        const val ATTRIBUTE_UNDERLINE = 4 // Underline; Single
        const val ATTRIBUTE_BLINK_SLOW = 5 // Blink; Slow  less than 150 per minute
        const val ATTRIBUTE_BLINK_FAST = 6 // Blink; Rapid 150 per minute or more
        const val ATTRIBUTE_NEGATIVE_ON = 7 // Negative inverse or reverse; swap foreground and background
        const val ATTRIBUTE_CONCEAL_ON = 8 // Conceal on
        const val ATTRIBUTE_STRIKETHROUGH_ON = 9 // Crossed-out
        const val ATTRIBUTE_UNDERLINE_DOUBLE = 21 // Underline; Double not widely supported
        const val ATTRIBUTE_NORMAL = 22 // Intensity; Normal not bold and not faint
        const val ATTRIBUTE_ITALIC_OFF = 23 // Not italic
        const val ATTRIBUTE_UNDERLINE_OFF = 24 // Underline; None
        const val ATTRIBUTE_BLINK_OFF = 25 // Blink; off
        const val ATTRIBUTE_NEGATIVE_OFF = 27 // Image; Positive
        const val ATTRIBUTE_CONCEAL_OFF = 28 // Reveal conceal off
        const val ATTRIBUTE_STRIKETHROUGH_OFF = 29 // Not crossed out
        const val ATTRIBUTE_DEFAULT_FG = 39 //  Default text color (foreground)
        const val ATTRIBUTE_DEFAULT_BG = 49 //  Default background color

        // for Erase Screen/Line
        const val ERASE_TO_END = 0
        const val ERASE_TO_BEGINNING = 1
        const val ERASE_ALL = 2

        private const val MAX_ESCAPE_SEQUENCE_LENGTH = 100

        internal enum class STATE {
            LOOKING_FOR_FIRST_ESC_CHAR,
            LOOKING_FOR_SECOND_ESC_CHAR,
            LOOKING_FOR_NEXT_ARG,
            LOOKING_FOR_STR_ARG_END,
            LOOKING_FOR_INT_ARG_END,
            LOOKING_FOR_OSC_COMMAND,
            LOOKING_FOR_OSC_COMMAND_END,
            LOOKING_FOR_OSC_PARAM,
            LOOKING_FOR_ST
        }


        private const val FIRST_ESC_CHAR = 27
        private const val SECOND_ESC_CHAR = '['.code
        private const val SECOND_OSC_CHAR = ']'.code
        private const val BEL = 7
        private const val SECOND_ST_CHAR = '\\'.code

        val RESET_CODE = StringBuilder(3).append(FIRST_ESC_CHAR.toChar()).append(SECOND_ESC_CHAR.toChar()).append(TEXT_ATTRIBUTE).toString()
            .toByteArray(CHARSET)
    }
}
