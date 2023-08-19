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

import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.Wincon
import com.sun.jna.ptr.IntByReference
import dorkbox.jna.windows.Kernel32
import dorkbox.jna.windows.structs.CONSOLE_SCREEN_BUFFER_INFO
import dorkbox.jna.windows.structs.COORD
import dorkbox.jna.windows.structs.SMALL_RECT
import java.io.IOException
import java.io.OutputStream
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.math.max
import kotlin.math.min

/**
 * A Windows ANSI escape processor, uses JNA direct-mapping to access native platform API's to change the console attributes.
 *
 * See: https://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * @author dorkbox, llc
 * @author [Hiram Chirino](http://hiramchirino.com)
 * @author Joris Kuipers
 */
class WindowsAnsiOutputStream internal constructor(os: OutputStream?, fHandle: Int) : AnsiOutputStream(os) {
    private val console: WinNT.HANDLE?
    private val originalInfo = CONSOLE_SCREEN_BUFFER_INFO()
    private val fileHandle: Int

    private val info = CONSOLE_SCREEN_BUFFER_INFO()

    @Volatile
    private var negative = false

    @Volatile
    private var savedX = (-1).toShort()

    @Volatile
    private var savedY = (-1).toShort()

    // reused vars
    private val written = IntByReference()

    init {
        fileHandle = if (fHandle == 1) { // STDOUT_FILENO
            Wincon.STD_OUTPUT_HANDLE
        }
        else if (fHandle == 2) { // STDERR_FILENO
            Wincon.STD_ERROR_HANDLE
        }
        else {
            throw IllegalArgumentException("Invalid file handle $fHandle")
        }


        console = Kernel32.GetStdHandle(fileHandle)
        if (console === WinBase.INVALID_HANDLE_VALUE) {
            throw IOException("Unable to get input console handle.")
        }
        out.flush()
        if (Kernel32.GetConsoleScreenBufferInfo(console, originalInfo) == 0) {
            throw IOException("Could not get the screen info")
        }
    }

    @get:Throws(IOException::class)
    private val consoleInfo: Unit
        get() {
            out.flush()
            Kernel32.ASSERT(Kernel32.GetConsoleScreenBufferInfo(console, info), "Could not get the screen info:")
        }

    @Throws(IOException::class)
    private fun applyAttributes() {
        out.flush()

        var attributes = info.attributes
        if (negative) {
            // Swap the the Foreground and Background bits.
            var fg = 0x000F and attributes.toInt()
            fg = fg shl 8
            var bg = 0X00F0 * attributes
            bg = bg shr 8
            attributes = (attributes.toInt() and 0xFF00 or fg or bg).toShort()
        }
        Kernel32.ASSERT(Kernel32.SetConsoleTextAttribute(console, attributes), "Could not set text attributes")
    }

    @Throws(IOException::class)
    private fun applyCursorPosition() {
        Kernel32.ASSERT(Kernel32.SetConsoleCursorPosition(console, info.cursorPosition.asValue()), "Could not set cursor position")
    }

    @Throws(IOException::class)
    override fun processRestoreCursorPosition() {
        // restore only if there was a save operation first
        if (savedX.toInt() != -1 && savedY.toInt() != -1) {
            out.flush()
            info.cursorPosition.x = savedX
            info.cursorPosition.y = savedY
            applyCursorPosition()
        }
    }

    @Throws(IOException::class)
    override fun processSaveCursorPosition() {
        consoleInfo
        savedX = info.cursorPosition.x
        savedY = info.cursorPosition.y
    }

    @Throws(IOException::class)
    override fun processEraseScreen(eraseOption: Int) {
        consoleInfo
        when (eraseOption) {
            ERASE_ALL          -> {
                val topLeft = COORD()
                topLeft.x = 0.toShort()
                topLeft.y = info.window.top

                val screenLength = info.window.height() * info.size.x
                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputAttribute(
                        console,
                        originalInfo.attributes,
                        screenLength,
                        topLeft.asValue(),
                        written
                    ), "Could not fill console"
                )
                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputCharacter(console, ' ', screenLength, topLeft.asValue(), written),
                    "Could not fill console"
                )
            }

            ERASE_TO_BEGINNING -> {
                val topLeft2 = COORD()
                topLeft2.x = 0.toShort()
                topLeft2.y = info.window.top

                val lengthToCursor = (info.cursorPosition.y - info.window.top) * info.size.x + info.cursorPosition.x
                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputAttribute(
                        console,
                        originalInfo.attributes,
                        lengthToCursor,
                        topLeft2.asValue(),
                        written
                    ), "Could not fill console"
                )
                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputCharacter(console, ' ', lengthToCursor, topLeft2.asValue(), written),
                    "Could not fill console"
                )
            }

            ERASE_TO_END       -> {
                val lengthToEnd = (info.window.bottom - info.cursorPosition.y) * info.size.x + info.size.x - info.cursorPosition.x
                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputAttribute(
                        console,
                        originalInfo.attributes,
                        lengthToEnd,
                        info.cursorPosition.asValue(),
                        written
                    ), "Could not fill console"
                )
                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputCharacter(console, ' ', lengthToEnd, info.cursorPosition.asValue(), written),
                    "Could not fill console"
                )
            }
        }
    }

    @Throws(IOException::class)
    override fun processEraseLine(eraseOption: Int) {
        consoleInfo
        when (eraseOption) {
            ERASE_ALL          -> {
                val currentRow: COORD = info.cursorPosition.asValue()
                currentRow.x = 0.toShort()

                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputAttribute(
                        console,
                        originalInfo.attributes,
                        info.size.x.toInt(),
                        currentRow.asValue(),
                        written
                    ), "Could not fill console"
                )
                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputCharacter(console, ' ', info.size.x.toInt(), currentRow.asValue(), written),
                    "Could not fill console"
                )
            }

            ERASE_TO_BEGINNING -> {
                val leftColCurrRow2: COORD = info.cursorPosition.asValue()
                leftColCurrRow2.x = 0.toShort()

                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputAttribute(
                        console,
                        originalInfo.attributes,
                        info.cursorPosition.x.toInt(),
                        leftColCurrRow2.asValue(),
                        written
                    ), "Could not fill console"
                )
                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputCharacter(
                        console,
                        ' ',
                        info.cursorPosition.x.toInt(),
                        leftColCurrRow2.asValue(),
                        written
                    ), "Could not fill console"
                )
            }

            ERASE_TO_END       -> {
                val lengthToLastCol = info.size.x - info.cursorPosition.x

                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputAttribute(
                        console,
                        originalInfo.attributes,
                        lengthToLastCol,
                        info.cursorPosition.asValue(),
                        written
                    ), "Could not fill console"
                )
                Kernel32.ASSERT(
                    Kernel32.FillConsoleOutputCharacter(console, ' ', lengthToLastCol, info.cursorPosition.asValue(), written),
                    "Could not fill console"
                )
            }
        }
    }

    @Throws(IOException::class)
    override fun processSetAttribute(attribute: Int) {
        if (90 <= attribute && attribute <= 97) {
            // foreground bright
            info.attributes = (info.attributes.toInt() and 0x000F.inv() or ANSI_FOREGROUND_COLOR_MAP[attribute - 90].toInt()).toShort()
            info.attributes = (info.attributes or Kernel32.FOREGROUND_INTENSITY)
            applyAttributes()
            return
        }
        else if (100 <= attribute && attribute <= 107) {
            // background bright
            info.attributes = (info.attributes.toInt() and 0x00F0.inv() or ANSI_BACKGROUND_COLOR_MAP[attribute - 100].toInt()).toShort()
            info.attributes = (info.attributes or Kernel32.BACKGROUND_INTENSITY)
            applyAttributes()
            return
        }

        when (attribute) {
            ATTRIBUTE_BOLD          -> {
                info.attributes = (info.attributes or Kernel32.FOREGROUND_INTENSITY)
                applyAttributes()
            }

            ATTRIBUTE_NORMAL        -> {
                info.attributes = (info.attributes and Kernel32.FOREGROUND_INTENSITY.inv())
                applyAttributes()
            }

            ATTRIBUTE_UNDERLINE     -> {
                info.attributes = (info.attributes or Kernel32.BACKGROUND_INTENSITY)
                applyAttributes()
            }

            ATTRIBUTE_UNDERLINE_OFF -> {
                info.attributes = (info.attributes and Kernel32.BACKGROUND_INTENSITY.inv())
                applyAttributes()
            }

            ATTRIBUTE_NEGATIVE_ON   -> {
                negative = true
                applyAttributes()
            }

            ATTRIBUTE_NEGATIVE_OFF  -> {
                negative = false
                applyAttributes()
            }
        }
    }

    @Throws(IOException::class)
    override fun processSetForegroundColor(color: Int) {
        info.attributes = (info.attributes.toInt() and 0x000F.inv() or ANSI_FOREGROUND_COLOR_MAP[color].toInt()).toShort()
        applyAttributes()
    }

    @Throws(IOException::class)
    override fun processSetBackgroundColor(color: Int) {
        info.attributes = (info.attributes.toInt() and 0x00F0.inv() or ANSI_BACKGROUND_COLOR_MAP[color].toInt()).toShort()
        applyAttributes()
    }

    @Throws(IOException::class)
    override fun processDefaultTextColor() {
        info.attributes = (info.attributes.toInt() and 0x000F.inv() or (originalInfo.attributes.toInt() and 0x000F)).toShort()
        applyAttributes()
    }

    @Throws(IOException::class)
    override fun processDefaultBackgroundColor() {
        info.attributes = (info.attributes.toInt() and 0x00F0.inv() or (originalInfo.attributes.toInt() and 0x00F0)).toShort()
        applyAttributes()
    }

    @Throws(IOException::class)
    override fun processAttributeReset() {
        //info.attributes = originalInfo.attributes;
        info.attributes = (info.attributes.toInt() and 0x00FF.inv() or originalInfo.attributes.toInt()).toShort()
        negative = false
        applyAttributes()
    }

    @Throws(IOException::class)
    override fun processScrollDown(count: Int) {
        scroll((-count).toShort())
    }

    @Throws(IOException::class)
    override fun processScrollUp(count: Int) {
        scroll(count.toShort())
    }

    @Throws(IOException::class)
    override fun processCursorUpLine(count: Int) {
        consoleInfo
        info.cursorPosition.y = max(info.window.top.toDouble(), (info.cursorPosition.y - count).toDouble()).toInt().toShort()
        info.cursorPosition.x = 0.toShort()
        applyCursorPosition()
    }

    @Throws(IOException::class)
    override fun processCursorDownLine(count: Int) {
        consoleInfo
        info.cursorPosition.y = max(info.window.top.toDouble(), (info.cursorPosition.y + count).toDouble()).toInt().toShort()
        info.cursorPosition.x = 0.toShort()
        applyCursorPosition()
    }

    @Throws(IOException::class)
    override fun processCursorTo(row: Int, col: Int) {
        consoleInfo
        info.cursorPosition.y =
            max(info.window.top.toDouble(), min(info.size.y.toDouble(), (info.window.top + row - 1).toDouble())).toInt().toShort()
        info.cursorPosition.x = max(0.0, min(info.window.width().toDouble(), (col - 1).toDouble())).toInt().toShort()
        applyCursorPosition()
    }

    @Throws(IOException::class)
    override fun processCursorToColumn(x: Int) {
        consoleInfo
        info.cursorPosition.x = max(0.0, min(info.window.width().toDouble(), (x - 1).toDouble())).toInt().toShort()
        applyCursorPosition()
    }

    @Throws(IOException::class)
    override fun processCursorLeft(count: Int) {
        consoleInfo
        info.cursorPosition.x = max(0.0, (info.cursorPosition.x - count).toDouble()).toInt().toShort()
        applyCursorPosition()
    }

    @Throws(IOException::class)
    override fun processCursorRight(count: Int) {
        consoleInfo
        info.cursorPosition.x = min(info.window.width().toDouble(), (info.cursorPosition.x + count).toDouble()).toInt().toShort()
        applyCursorPosition()
    }

    @Throws(IOException::class)
    override fun processCursorDown(count: Int) {
        consoleInfo
        info.cursorPosition.y = min(info.size.y.toDouble(), (info.cursorPosition.y + count).toDouble()).toInt().toShort()
        applyCursorPosition()
    }

    @Throws(IOException::class)
    override fun processCursorUp(count: Int) {
        consoleInfo
        info.cursorPosition.y = max(info.window.top.toDouble(), (info.cursorPosition.y - count).toDouble()).toInt().toShort()
        applyCursorPosition()
    }

    /**
     * Scrolls the contents of the buffer either UP or DOWN
     *
     * @param rowsToScroll negative to go down, positive to go up.
     *
     * Scroll up and new lines are added at the bottom, scroll down and new lines are added at the
     * top (per the definition).
     *
     * Windows doesn't EXACTLY do this, since it will use whatever content is still on the buffer
     * and show THAT instead of blank lines. If the content is moved enough so that it runs OFF the
     * buffer, blank lines will be shown.
     */
    @Throws(IOException::class)
    private fun scroll(rowsToScroll: Short) {
        if (rowsToScroll.toInt() == 0) {
            return
        }

        // Get the current screen buffer window position.
        consoleInfo
        val scrollRect = SMALL_RECT.ByReference()
        val coordDest = COORD.ByValue()

        // the content that will be scrolled (just what is visible in the window)
        scrollRect.top = (info.cursorPosition.y - info.window.height()).toShort()
        scrollRect.bottom = info.cursorPosition.y
        scrollRect.left = 0.toShort()
        scrollRect.right = (info.size.x - 1).toShort()

        // The destination for the scroll rectangle is xxx row up/down.
        coordDest.x = 0.toShort()
        coordDest.y = (scrollRect.top - rowsToScroll).toShort()

        // fill the space with whatever color was already there with spaces
        val attribs = written
        attribs.value = info.attributes.toInt()

        // The clipping rectangle is the same as the scrolling rectangle, so we pass NULL
        Kernel32.ASSERT(Kernel32.ScrollConsoleScreenBuffer(console, scrollRect, null, coordDest, attribs), "Could not scroll console")
    }

    @Throws(IOException::class)
    override fun close() {
        super.close()
        if (console != null) {
            Kernel32.CloseHandle(console)
        }
    }

    companion object {
        private val ANSI_FOREGROUND_COLOR_MAP: ShortArray
        private val ANSI_BACKGROUND_COLOR_MAP: ShortArray

        init {
            ANSI_FOREGROUND_COLOR_MAP = ShortArray(8)
            ANSI_FOREGROUND_COLOR_MAP[BLACK] = Kernel32.FOREGROUND_BLACK
            ANSI_FOREGROUND_COLOR_MAP[RED] = Kernel32.FOREGROUND_RED
            ANSI_FOREGROUND_COLOR_MAP[GREEN] = Kernel32.FOREGROUND_GREEN
            ANSI_FOREGROUND_COLOR_MAP[YELLOW] = Kernel32.FOREGROUND_YELLOW
            ANSI_FOREGROUND_COLOR_MAP[BLUE] = Kernel32.FOREGROUND_BLUE
            ANSI_FOREGROUND_COLOR_MAP[MAGENTA] = Kernel32.FOREGROUND_MAGENTA
            ANSI_FOREGROUND_COLOR_MAP[CYAN] = Kernel32.FOREGROUND_CYAN
            ANSI_FOREGROUND_COLOR_MAP[WHITE] = Kernel32.FOREGROUND_GREY
            ANSI_BACKGROUND_COLOR_MAP = ShortArray(8)
            ANSI_BACKGROUND_COLOR_MAP[BLACK] = Kernel32.BACKGROUND_BLACK
            ANSI_BACKGROUND_COLOR_MAP[RED] = Kernel32.BACKGROUND_RED
            ANSI_BACKGROUND_COLOR_MAP[GREEN] = Kernel32.BACKGROUND_GREEN
            ANSI_BACKGROUND_COLOR_MAP[YELLOW] = Kernel32.BACKGROUND_YELLOW
            ANSI_BACKGROUND_COLOR_MAP[BLUE] = Kernel32.BACKGROUND_BLUE
            ANSI_BACKGROUND_COLOR_MAP[MAGENTA] = Kernel32.BACKGROUND_MAGENTA
            ANSI_BACKGROUND_COLOR_MAP[CYAN] = Kernel32.BACKGROUND_CYAN
            ANSI_BACKGROUND_COLOR_MAP[WHITE] = Kernel32.BACKGROUND_GREY
        }
    }
}
