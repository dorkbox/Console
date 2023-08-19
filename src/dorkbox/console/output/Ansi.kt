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

import dorkbox.console.Console
import dorkbox.jna.linux.CLibraryPosix
import dorkbox.os.OS.Windows.isWindows10_plus
import dorkbox.os.OS.isWindows
import org.slf4j.LoggerFactory
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream

/**
 * Provides a fluent API for generating ANSI escape sequences and providing access to streams that support it.
 *
 *
 * See: https://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * @author dorkbox, llc
 * @author [Hiram Chirino](http://hiramchirino.com)
 */
@Suppress("unused")
class Ansi(private val builder: StringBuilder = StringBuilder()) {

    /**
     * Creates a new Ansi object from the parent.
    */
    constructor(parent: Ansi) : this(StringBuilder(parent.builder)) {
        attributeOptions.addAll(parent.attributeOptions)
    }

    /**
     * Creates a new Ansi object of the specified length
     */
    constructor(size: Int) : this(StringBuilder(size)) {
        reset() // always reset a NEW Ansi object (with no parent)
    }

    /**
     * Creates a new Ansi object
     */
    private val attributeOptions = mutableListOf<Int>()

    /**
     * Sets the foreground color of the ANSI output. BRIGHT_* colors are a brighter version of that color. DEFAULT is the color from
     * the beginning before any other color was applied.
     *
     * @param color foreground color to set
     */
    fun fg(color: Color): Ansi {
        if (color.isNormal) {
            if (color !== Color.DEFAULT) {
                attributeOptions.add(color.fg())
            }
            else {
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_FG)
            }
        }
        else {
            if (color !== Color.BRIGHT_DEFAULT) {
                attributeOptions.add(color.fgBright())
            }
            else {
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_FG)
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_BOLD)
            }
        }
        return this
    }

    /**
     * Sets the background color of the ANSI output. BRIGHT_* colors are a brighter version of that color. DEFAULT is the color from
     * the beginning before any other color was applied.
     *
     * @param color background color to set
     */
    fun bg(color: Color): Ansi {
        if (color.isNormal) {
            if (color !== Color.DEFAULT) {
                attributeOptions.add(color.bg())
            }
            else {
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_BG)
            }
        }
        else {
            if (color !== Color.BRIGHT_DEFAULT) {
                attributeOptions.add(color.bgBright())
            }
            else {
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_DEFAULT_BG)
                attributeOptions.add(AnsiOutputStream.ATTRIBUTE_BOLD)
            }
        }
        return this
    }

    /**
     * Moves the cursor y (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    fun cursorUp(y: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP, y)
    }

    /**
     * Moves the cursor y (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    fun cursorDown(y: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN, y)
    }

    /**
     * Moves the cursor x (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    fun cursorRight(x: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_FORWARD, x)
    }

    /**
     * Moves the cursor x (default 1) cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    fun cursorLeft(x: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_BACK, x)
    }

    /**
     * Moves the cursor 1 cell cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    fun cursorUp(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP)
    }

    /**
     * Moves the cursor 1 cell cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    fun cursorDown(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN)
    }

    /**
     * Moves the cursor 1 cell cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    fun cursorRight(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_FORWARD)
    }

    /**
     * Moves the cursor 1 cell cells in the given direction. If the cursor is already at the edge of the screen, this has no effect.
     */
    fun cursorLeft(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_BACK)
    }

    /**
     * Moves cursor to beginning of the line n (default 1) lines down.
     */
    fun cursorDownLine(n: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN_LINE, n)
    }

    /**
     * Moves cursor to beginning of the line n (default 1) lines up.
     */
    fun cursorUpLine(n: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP_LINE, n)
    }

    /**
     * Moves cursor to beginning of the line 1 line down.
     */
    fun cursorDownLine(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_DOWN_LINE)
    }

    /**
     * Moves cursor to beginning of the line 1 lines up.
     */
    fun cursorUpLine(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_UP_LINE)
    }

    /**
     * Moves the cursor to column n (default 1).
     * @param n is 1 indexed (the very first value is 1, not 0)
     */
    fun cursorToColumn(n: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_TO_COL, n)
    }

    /**
     * Moves the cursor to column 1. The very first value is 1, not 0.
     */
    fun cursorToColumn(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_TO_COL)
    }

    /**
     * Moves the cursor to row x, column y.
     *
     * The values are 1-based, the top left corner.
     *
     * @param x is 1 indexed (the very first value is 1, not 0)
     * @param y is 1 indexed (the very first value is 1, not 0)
     */
    fun cursor(x: Int, y: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_POS, x, y)
    }

    /**
     * Moves the cursor to row 1, column 1 (top left corner).
     */
    fun cursor(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_POS)
    }

    /**
     * Clears part of the screen, by default clear everything forwards of the cursor
     *
     * @param kind
     * - [Erase.FORWARD] (or missing), clear from cursor to end of screen.
     * - [Erase.BACKWARD], clear from cursor to beginning of the screen.
     * - [Erase.ALL], clear entire screen (and moves cursor to upper left on DOS ANSI.SYS).
     */
    fun eraseScreen(kind: Erase?): Ansi {
        return if (kind == null) {
            eraseScreen()
        }
        else appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_SCREEN, kind.value())
    }

    /**
     * Clears everything forwards of the cursor
     */
    fun eraseScreen(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_SCREEN, Erase.ALL.value())
    }

    /**
     * Erases part of the line.
     *
     * @param kind
     * - [Erase.FORWARD] (or missing), clear from cursor to the end of the line.
     * - [Erase.BACKWARD], clear from cursor to beginning of the line.
     * - [Erase.ALL], clear entire line. Cursor position does not change.
     */
    fun eraseLine(kind: Erase): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_LINE, kind.value())
    }

    /**
     * Erases from cursor to the end of the line.
     */
    fun eraseLine(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.CURSOR_ERASE_LINE)
    }

    /**
     * Scroll whole page up by n (default 1) lines. New lines are added at the bottom.
     */
    fun scrollUp(n: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.SCROLL_UP, n)
    }

    /**
     * Scroll whole page up by 1 line. New lines are added at the bottom.
     */
    fun scrollUp(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.SCROLL_UP)
    }

    /**
     * Scroll whole page down by n (default 1) lines. New lines are added at the top.
     */
    fun scrollDown(n: Int): Ansi {
        return appendEscapeSequence(AnsiOutputStream.SCROLL_DOWN, n)
    }

    /**
     * Scroll whole page down by 1 line. New lines are added at the top.
     */
    fun scrollDown(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.SCROLL_DOWN)
    }

    /**
     * Saves the cursor position.
     */
    fun saveCursorPosition(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.SAVE_CURSOR_POS)
    }

    /**
     * Restores the cursor position.
     */
    fun restoreCursorPosition(): Ansi {
        return appendEscapeSequence(AnsiOutputStream.RESTORE_CURSOR_POS)
    }

    /**
     * Resets all of the attributes on the ANSI stream
     */
    fun reset(): Ansi {
        return a(Attribute.RESET)
    }

    /**
     * Bold enabled
     */
    fun bold(): Ansi {
        return a(Attribute.BOLD)
    }

    /**
     * Bold disabled
     */
    fun boldOff(): Ansi {
        return a(Attribute.BOLD_OFF)
    }

    /**
     * Faint enabled (not widely supported)
     */
    fun faint(): Ansi {
        return a(Attribute.FAINT)
    }

    /**
     * Faint disabled (not widely supported)
     */
    fun faintOff(): Ansi {
        return a(Attribute.FAINT_OFF)
    }

    /**
     * Italic enabled (not widely supported. Sometimes treated as inverse)
     */
    fun italic(): Ansi {
        return a(Attribute.ITALIC)
    }

    /**
     * Italic disabled (not widely supported. Sometimes treated as inverse)
     */
    fun italicOff(): Ansi {
        return a(Attribute.ITALIC_OFF)
    }

    /**
     * Underline; Single
     */
    fun underline(): Ansi {
        return a(Attribute.UNDERLINE)
    }

    /**
     * Underline; Double
     */
    fun underlineDouble(): Ansi {
        return a(Attribute.UNDERLINE_DOUBLE)
    }

    /**
     * Underline disabled
     */
    fun underlineOff(): Ansi {
        return a(Attribute.UNDERLINE_OFF)
    }

    /**
     * Blink; Slow  less than 150 per minute
     */
    fun blinkSlow(): Ansi {
        return a(Attribute.BLINK_SLOW)
    }

    /**
     * Blink; Rapid 150 per minute or more
     */
    fun blinkFast(): Ansi {
        return a(Attribute.BLINK_FAST)
    }

    /**
     * Blink disabled
     */
    fun blinkOff(): Ansi {
        return a(Attribute.BLINK_OFF)
    }

    /**
     * Negative inverse or reverse; swap foreground and background
     */
    fun negative(): Ansi {
        return a(Attribute.NEGATIVE)
    }

    /**
     * Negative disabled (back to normal)
     */
    fun negativeOff(): Ansi {
        return a(Attribute.NEGATIVE_OFF)
    }

    /**
     * Conceal on
     */
    fun conceal(): Ansi {
        return a(Attribute.CONCEAL)
    }

    /**
     * Conceal off
     */
    fun concealOff(): Ansi {
        return a(Attribute.CONCEAL_OFF)
    }

    /**
     * Strikethrough enabled
     */
    fun strikethrough(): Ansi {
        return a(Attribute.STRIKETHROUGH)
    }

    /**
     * Strikethrough disabled
     */
    fun strikethroughOff(): Ansi {
        return a(Attribute.STRIKETHROUGH_OFF)
    }

    /**
     * Appends an attribute (color/etc)
     *
     * @param attribute the Attribute (color/etc) to be appended to the ANSI stream
     * @return this
     */
    fun a(attribute: Attribute): Ansi {
        attributeOptions.add(attribute.value)
        return this
    }

    /**
     * Appends a String
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: String): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a boolean
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: Boolean): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a char
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: Char): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a char array + offset + length
     *
     * @param valueArray value to be appended to the ANSI stream
     * @return this
     */
    fun a(valueArray: CharArray, offset: Int, length: Int): Ansi {
        flushAttributes()
        builder.appendRange(valueArray, offset, offset + length)
        return this
    }

    /**
     * Appends a char array
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: CharArray): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a CharSequence + start + end
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: CharSequence, start: Int, end: Int): Ansi {
        flushAttributes()
        builder.append(value, start, end)
        return this
    }

    /**
     * Appends a CharSequence
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: CharSequence): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a double
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: Double): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a float
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: Float): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a int
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: Int): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a long
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: Long): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends an Object
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: Any): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a StringBuilder
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    fun a(value: StringBuilder): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a StringBuffer.
     *
     * You should be using a StringBuilder instead.
     *
     * @param value value to be appended to the ANSI stream
     * @return this
     */
    @Deprecated("You should be using a StringBuilder instead.")
    fun a(value: StringBuffer): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    /**
     * Appends a new line
     *
     * @return this
     */
    fun newline(): Ansi {
        flushAttributes()
        builder.append(NEW_LINE)
        return this
    }

    /**
     * Appends a formatted string
     *
     * @param pattern String.format pattern to use
     * @param args arguments to use in the formatted string
     * @return this
     */
    fun format(pattern: String, vararg args: Any): Ansi {
        flushAttributes()
        builder.append(String.format(pattern, *args))
        return this
    }

    /**
     * Uses the [AnsiRenderer] to generate the ANSI escape sequences for the supplied text.
     */
    fun render(text: String): Ansi {
        a(AnsiRenderer.render(text))
        return this
    }

    /**
     * String formats and renders the supplied arguments.
     * Uses the [AnsiRenderer] to generate the ANSI escape sequences.
     */
    fun render(text: String, vararg args: Any): Ansi {
        a(String.format(AnsiRenderer.render(text), *args))
        return this
    }

    override fun toString(): String {
        flushAttributes()
        return builder.toString()
    }

    /**
     * Creates a new Ansi object from the specified StringBuilder
     */
    private fun appendEscapeSequence(command: Char): Ansi {
        flushAttributes()
        builder.append(FIRST_ESC_CHAR)
        builder.append(SECOND_ESC_CHAR)
        builder.append(command)
        return this
    }

    private fun appendEscapeSequence(command: Char, option: Int): Ansi {
        flushAttributes()
        builder.append(FIRST_ESC_CHAR)
        builder.append(SECOND_ESC_CHAR)
        builder.append(option)
        builder.append(command)
        return this
    }

    private fun appendEscapeSequence(command: Char, vararg options: Any): Ansi {
        flushAttributes()
        return _appendEscapeSequence(command, *options)
    }

    private fun flushAttributes() {
        if (attributeOptions.isEmpty()) {
            return
        }
        if (attributeOptions.size == 1 && attributeOptions[0] == AnsiOutputStream.ATTRIBUTE_RESET) {
            builder.append(FIRST_ESC_CHAR)
            builder.append(SECOND_ESC_CHAR)
            builder.append(AnsiOutputStream.TEXT_ATTRIBUTE)
        }
        else {
            _appendEscapeSequence(AnsiOutputStream.TEXT_ATTRIBUTE, *attributeOptions.toTypedArray())
        }
        attributeOptions.clear()
    }

    private fun _appendEscapeSequence(command: Char, vararg options: Any): Ansi {
        builder.append(FIRST_ESC_CHAR)
        builder.append(SECOND_ESC_CHAR)
        val size = options.size
        for (i in 0 until size) {
            if (i != 0) {
                builder.append(';')
            }
            builder.append(options[i])
        }
        builder.append(command)
        return this
    }

    companion object {
        /**
         * Gets the version number.
         */
        const val version = Console.version

        private val logger = LoggerFactory.getLogger(Console::class.java)
        private val original_out = System.out
        private val original_err = System.err

        val out = createPrintStream(original_out, 1, Console.AUTO_FLUSH) // STDOUT_FILENO
        val err = createPrintStream(original_err, 2, Console.AUTO_FLUSH) // STDERR_FILENO

        init {
            // make SURE that our console in/out/err are correctly setup BEFORE accessing methods in this class
            System.setOut(out)
            System.setErr(err)

            // don't forget we have to shut down the ansi console as well
            val shutdownThread: Thread = object : Thread() {
                override fun run() {
                    // called when the JVM is shutting down.
                    restoreSystemStreams()
                }
            }

            shutdownThread.setName("Console ANSI Shutdown Hook")
            Runtime.getRuntime().addShutdownHook(shutdownThread)
        }

        private val NEW_LINE = System.getProperty("line.separator")

        /**
         * Restores System.err/out PrintStreams to their ORIGINAL configuration. Useful when using ANSI functionality but do not want to
         * hook into the system.
         */
        fun restoreSystemStreams() {
            System.setOut(original_out)
            System.setErr(original_err)
        }

        /**
         * Creates a new Ansi object
         */
        fun ansi(): Ansi {
            return Ansi()
        }

        /**
         * Creates a new Ansi object from the specified StringBuilder
         */
        fun ansi(builder: StringBuilder): Ansi {
            return Ansi(builder)
        }

        /**
         * Creates a new Ansi object of the specified length
         */
        fun ansi(size: Int): Ansi {
            return Ansi(size)
        }

        /**
         * Creates a new Ansi object from the specified parent
         */
        fun ansi(ansi: Ansi): Ansi {
            return Ansi(ansi)
        }

        ///////////////////////////////////////////////////////////////////
        // Private Helper Methods
        ///////////////////////////////////////////////////////////////////
        private const val FIRST_ESC_CHAR = 27.toChar()
        private const val SECOND_ESC_CHAR = '['

        private val isXterm: Boolean
            get() {
                val term = System.getenv("TERM")
                return "xterm".equals(term, ignoreCase = true)
            }

        private fun createPrintStream(stream: OutputStream, fileno: Int, autoFlush: Boolean): PrintStream {
            val type = if (fileno == 1) "OUT" else "ERR"

            if (!Console.ENABLE_ANSI) {
                // Use the ANSIOutputStream to strip out the ANSI escape sequences.
                return getStripPrintStream(stream, type, autoFlush)
            }

            // intellij idea console supports ANSI colors... but NOT REALLY! (they are off)
            if (System.getProperty("idea.launcher.bin.path") != null // "run"
                || System.getProperty("java.class.path").contains("idea_rt.jar") // "debug"
            ) {

                // Use the ANSIOutputStream to strip out the ANSI escape sequences.
                return getStripPrintStream(stream, type, autoFlush)
            }
            if (!isXterm) {
                if (isWindows) {
                    // check if windows10+ (which natively supports ANSI)
                    if (isWindows10_plus) {
                        // Just wrap it up so that when we get closed, we reset the attributes.
                        return defaultPrintStream(stream, type, autoFlush)
                    }

                    // On windows we know the console does not interpret ANSI codes...
                    try {
                        val printStream = PrintStream(WindowsAnsiOutputStream(stream, fileno), autoFlush)
                        if (logger.isDebugEnabled) {
                            logger.debug("Created a Windows ANSI PrintStream for {}", type)
                        }
                        return printStream
                    }
                    catch (ignore: Throwable) {
                        // this happens when JNA is not in the path... or
                        // this happens when the stdout is being redirected to a file.
                        // this happens when the stdout is being redirected to different console.
                    }

                    // Use the ANSIOutputStream to strip out the ANSI escape sequences.
                    if (!Console.FORCE_ENABLE_ANSI) {
                        return getStripPrintStream(stream, type, autoFlush)
                    }
                }
                else {
                    // We must be on some unix variant..
                    try {
                        // If we can detect that stdout is not a tty.. then setup to strip the ANSI sequences..
                        if (!Console.FORCE_ENABLE_ANSI && CLibraryPosix.isatty(fileno) == 0) {
                            return getStripPrintStream(stream, type, autoFlush)
                        }
                    }
                    catch (ignore: Throwable) {
                        // These errors happen if the JNI lib is not available for your platform.
                    }
                }
            }

            // By default we assume the terminal can handle ANSI codes.
            // Just wrap it up so that when we get closed, we reset the attributes.
            return defaultPrintStream(stream, type, autoFlush)
        }

        private fun getStripPrintStream(stream: OutputStream, type: String, autoFlush: Boolean): PrintStream {
            if (logger.isDebugEnabled) {
                logger.debug("Created a strip-ANSI PrintStream for {}", type)
            }
            return PrintStream(AnsiOutputStream(stream), autoFlush)
        }

        private fun defaultPrintStream(stream: OutputStream, type: String, autoFlush: Boolean): PrintStream {
            if (logger.isDebugEnabled) {
                logger.debug("Created ANSI PrintStream for {}", type)
            }

            return PrintStream(object : FilterOutputStream(stream) {
                @Throws(IOException::class)
                override fun close() {
                    write(AnsiOutputStream.RESET_CODE)
                    flush()
                    super.close()
                }
            }, autoFlush)
        }
    }
}
