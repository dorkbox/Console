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

/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
package dorkbox.console.input

import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.Wincon
import com.sun.jna.ptr.IntByReference
import dorkbox.jna.windows.Kernel32
import dorkbox.jna.windows.structs.CONSOLE_SCREEN_BUFFER_INFO
import dorkbox.jna.windows.structs.INPUT_RECORD
import java.io.IOException

/**
 * Terminal implementation for Microsoft Windows.
 */
class WindowsTerminal : SupportedTerminal() {
    companion object {
        // Console mode constants copied `wincon.h`
        // There are OTHER options, however they DO NOT work with unbuffered input or we just don't care about them.
        /**
         * CTRL+C is processed by the system and is not placed in the input buffer. If the input buffer is being read by ReadFile or
         * ReadConsole, other control keys are processed by the system and are not returned in the ReadFile or ReadConsole buffer. If the
         * ENABLE_LINE_INPUT mode is also enabled, backspace, carriage return, and linefeed characters are handled by the system.
         */
        private const val PROCESSED_INPUT = 1

        // output stream for "echo" to goto
        private val OUT = System.out
    }

    private val console: WinNT.HANDLE
    private val outputConsole: WinNT.HANDLE
    private val info = CONSOLE_SCREEN_BUFFER_INFO()
    private val inputRecords = INPUT_RECORD.ByReference()
    private val reference = IntByReference()

    private val originalMode: Int
    private var echoEnabled = false

    init {
        console = Kernel32.GetStdHandle(Wincon.STD_INPUT_HANDLE)
        if (console === WinBase.INVALID_HANDLE_VALUE) {
            throw IOException("Unable to get input console handle.")
        }

        outputConsole = Kernel32.GetStdHandle(Wincon.STD_OUTPUT_HANDLE)
        if (outputConsole === WinBase.INVALID_HANDLE_VALUE) {
            throw IOException("Unable to get output console handle.")
        }

        val mode = IntByReference()
        if (Kernel32.GetConsoleMode(console, mode) == 0) {
            throw IOException(CONSOLE_ERROR_INIT)
        }

        originalMode = mode.value
        val newMode = 0 // this is raw everything, not ignoring ctrl-c
        Kernel32.ASSERT(Kernel32.SetConsoleMode(console, newMode), CONSOLE_ERROR_INIT)
    }

    /**
     * Restore the original terminal configuration, which can be used when shutting down the console reader.
     * The ConsoleReader cannot be used after calling this method.
     */
    @Throws(IOException::class)
    override fun restore() {
        Kernel32.ASSERT(Kernel32.SetConsoleMode(console, originalMode), CONSOLE_ERROR_INIT)
        Kernel32.CloseHandle(console)
        Kernel32.CloseHandle(outputConsole)
    }

    override val width: Int
        get() {
            Kernel32.GetConsoleScreenBufferInfo(outputConsole, info)
            val w = info.window.width() + 1
            return if (w < 1) DEFAULT_WIDTH else w
        }

    override val height: Int
        get() {
            Kernel32.GetConsoleScreenBufferInfo(outputConsole, info)
            val h = info.window.height() + 1
            return if (h < 1) DEFAULT_HEIGHT else h
        }

    override fun doSetEchoEnabled(enabled: Boolean) {
        // only way to do this, console modes DO NOT work
        echoEnabled = enabled
    }

    override fun doSetInterruptEnabled(enabled: Boolean) {
        val mode = IntByReference()
        Kernel32.GetConsoleMode(console, mode)

        val newMode: Int = if (enabled) {
            // Enable  Ctrl+C
            mode.value or PROCESSED_INPUT
        }
        else {
            // Disable Ctrl+C
            mode.value and PROCESSED_INPUT.inv()
        }
        Kernel32.ASSERT(Kernel32.SetConsoleMode(console, newMode), CONSOLE_ERROR_INIT)
    }

    override fun doRead(): Int {
        val input = readInput()
        if (echoEnabled) {
            val asChar = input.toChar()
            if (asChar == '\n') {
                OUT.println()
            }
            else {
                OUT.write(asChar.code)
            }
            // have to flush, otherwise we'll never see the chars on screen
            OUT.flush()
        }
        return input
    }

    private fun readInput(): Int {
        // keep reading input events until we find one that we are interested in (ie: keyboard input)
        while (true) {
            // blocks until there is (at least) 1 event on the buffer
            Kernel32.ReadConsoleInput(console, inputRecords, 1, reference)
            for (i in 0 until reference.value) {
                if (inputRecords.EventType == INPUT_RECORD.KEY_EVENT) {
                    val keyEvent = inputRecords.Event.KeyEvent

                    //logger.trace(keyEvent.bKeyDown ? "KEY_DOWN" : "KEY_UP", "key code:", keyEvent.wVirtualKeyCode, "char:", (long)keyEvent.uChar.unicodeChar);
                    if (keyEvent.keyDown) {
                        val uChar = keyEvent.uChar.unicodeChar
                        if (uChar.code > 0) {
                            if (uChar == '\r') {
                                // we purposefully swallow input after \r, and substitute it with \n
                                return '\n'.code
                            }
                            else if (uChar == '\n') {
                                continue
                            }
                            return uChar.code
                        }
                    }
                }
            }
        }
    }
}
