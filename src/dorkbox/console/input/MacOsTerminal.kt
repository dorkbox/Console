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

import com.sun.jna.ptr.IntByReference
import dorkbox.jna.linux.CLibraryPosix
import dorkbox.jna.macos.CLibraryApple
import dorkbox.jna.macos.CLibraryApple.TIOCGWINSZ
import dorkbox.jna.macos.Termios
import dorkbox.jna.macos.Termios.*
import dorkbox.jna.macos.Termios.Input
import dorkbox.jna.macos.WindowSize
import java.io.IOException



/**
 * Terminal that is used for unix platforms. Terminal initialization is handled via JNA and ioctl/tcgetattr/tcsetattr/cfmakeraw.
 *
 * This implementation should work for Apple osx.
 */
class MacOsTerminal : SupportedTerminal() {
    private val original = Termios()
    private val termInfo = Termios()
    private val inputRef = IntByReference()

    init {
        // save off the defaults
        if (CLibraryApple.tcgetattr(0, original) != 0) {
            throw IOException(CONSOLE_ERROR_INIT)
        }

        original.read()

        // CTRL-I (tab), CTRL-M (enter)  do not work
        if (CLibraryApple.tcgetattr(0, termInfo) != 0) {
            throw IOException(CONSOLE_ERROR_INIT)
        }

        termInfo.read()

        and(termInfo.inputFlags, Input.IXON.inv()) // DISABLE - output flow control mediated by ^S and ^Q
        and(termInfo.inputFlags, Input.IXOFF.inv()) // DISABLE - input flow control mediated by ^S and ^Q
        and(termInfo.inputFlags, Input.BRKINT.inv()) // DISABLE - map BREAK to SIGINTR
        and(termInfo.inputFlags, Input.INPCK.inv()) // DISABLE - enable checking of parity errors
        and(termInfo.inputFlags, Input.PARMRK.inv()) // DISABLE - mark parity and framing errors
        and(termInfo.inputFlags, Input.ISTRIP.inv()) // DISABLE - strip 8th bit off chars
        or(termInfo.inputFlags, Input.IGNBRK) // ignore BREAK condition

        and(termInfo.localFlags, Local.ICANON.inv()) // DISABLE - pass chars straight through to terminal instantly
        or(termInfo.localFlags, Local.ECHOCTL) // echo control chars as ^(Char)

        and(termInfo.controlFlags, Control.CSIZE.inv()) // REMOVE character size mask
        and(termInfo.controlFlags, Control.PARENB.inv()) // DISABLE - parity enable
        or(termInfo.controlFlags, Control.CS8) // set character size mask 8 bits
        or(termInfo.controlFlags, Control.CREAD) // enable receiver

        if (CLibraryApple.tcsetattr(0, TCSANOW, termInfo) != 0) {
            throw IOException("Can not set terminal flags")
        }
    }

    /**
     * Restore the original terminal configuration, which can be used when shutting down the console reader. The ConsoleReader cannot be
     * used after calling this method.
     */
    @Throws(IOException::class)
    override fun restore() {
        if (CLibraryApple.tcsetattr(0, TCSANOW, original) != 0) {
            throw IOException("Can not reset terminal to defaults")
        }
    }

    /**
     * Returns number of columns in the terminal.
     */
    override val width: Int
        get() {
            val size = WindowSize()
            return if (CLibraryApple.ioctl(0, TIOCGWINSZ, size) != 0) {
                DEFAULT_WIDTH
            }
            else {
                size.read()
                size.rows.toInt()
            }
        }

    /**
     * Returns number of rows in the terminal.
     */
    override val height: Int
        get() {
            val size = WindowSize()
            return if (CLibraryApple.ioctl(0, TIOCGWINSZ, size) != 0) {
                DEFAULT_HEIGHT
            }
            else {
                size.read()
                size.columns.toInt()
            }
        }

    override fun doSetEchoEnabled(enabled: Boolean) {
        // have to re-get them, since flags change everything
        if (CLibraryApple.tcgetattr(0, termInfo) != 0) {
            logger.error("Failed to get terminal info")
        }

        if (enabled) {
            or(termInfo.localFlags, Local.ECHO) // ENABLE Echo input characters.
        }
        else {
            and(termInfo.localFlags, Local.ECHO.inv()) // DISABLE Echo input characters.
        }

        if (CLibraryApple.tcsetattr(0, TCSANOW, termInfo) != 0) {
            logger.error("Can not set terminal flags")
        }
    }

    override fun doSetInterruptEnabled(enabled: Boolean) {
        // have to re-get them, since flags change everything
        if (CLibraryApple.tcgetattr(0, termInfo) != 0) {
            logger.error("Failed to get terminal info")
        }

        if (enabled) {
            or(termInfo.localFlags, Local.ISIG) // ENABLE ctrl-C
        }
        else {
            and(termInfo.localFlags, Local.ISIG.inv()) // DISABLE ctrl-C
        }
        if (CLibraryApple.tcsetattr(0, TCSANOW, termInfo) != 0) {
            logger.error("Can not set terminal flags")
        }
    }

    override fun doRead(): Int {
        CLibraryPosix.read(0, inputRef, 1)
        return inputRef.value
    }
}
