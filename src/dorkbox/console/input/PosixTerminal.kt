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
import dorkbox.jna.linux.structs.Termios
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Terminal that is used for unix platforms. Terminal initialization is handled via JNA and ioctl/tcgetattr/tcsetattr/cfmakeraw.
 *
 *
 * This implementation should work for a reasonable POSIX system.
 */
class PosixTerminal : SupportedTerminal() {
    private val original = Termios()
    private val termInfo = Termios()
    private val windowSizeBuffer = ByteBuffer.allocate(8)
    private val inputRef = IntByReference()

    init {
        // save off the defaults
        if (CLibraryPosix.tcgetattr(0, original) != 0) {
            throw IOException(CONSOLE_ERROR_INIT)
        }

        original.read()

        // CTRL-I (tab), CTRL-M (enter)  do not work
        if (CLibraryPosix.tcgetattr(0, termInfo) != 0) {
            throw IOException(CONSOLE_ERROR_INIT)
        }

        termInfo.read()

        termInfo.inputFlags = termInfo.inputFlags and Termios.Input.IXON.inv() // DISABLE - output flow control mediated by ^S and ^Q
        termInfo.inputFlags = termInfo.inputFlags and Termios.Input.IXOFF.inv() // DISABLE - input flow control mediated by ^S and ^Q
        termInfo.inputFlags = termInfo.inputFlags and Termios.Input.BRKINT.inv() // DISABLE - map BREAK to SIGINTR
        termInfo.inputFlags = termInfo.inputFlags and Termios.Input.INPCK.inv() // DISABLE - enable checking of parity errors
        termInfo.inputFlags = termInfo.inputFlags and Termios.Input.PARMRK.inv() // DISABLE - mark parity and framing errors
        termInfo.inputFlags = termInfo.inputFlags and Termios.Input.ISTRIP.inv() // DISABLE - strip 8th bit off chars
        termInfo.inputFlags = termInfo.inputFlags or Termios.Input.IGNBRK // ignore BREAK condition

        termInfo.localFlags = termInfo.localFlags and Termios.Local.ICANON.inv() // DISABLE - pass chars straight through to terminal instantly
        termInfo.localFlags = termInfo.localFlags or Termios.Local.ECHOCTL // echo control chars as ^(Char)

        termInfo.controlFlags = termInfo.controlFlags and Termios.Control.CSIZE.inv() // REMOVE character size mask
        termInfo.controlFlags = termInfo.controlFlags and Termios.Control.PARENB.inv() // DISABLE - parity enable
        termInfo.controlFlags = termInfo.controlFlags or Termios.Control.CS8 // set character size mask 8 bits
        termInfo.controlFlags = termInfo.controlFlags or Termios.Control.CREAD // enable receiver

        if (CLibraryPosix.tcsetattr(0, Termios.TCSANOW, termInfo) != 0) {
            throw IOException("Can not set terminal flags")
        }
    }

    /**
     * Restore the original terminal configuration, which can be used when shutting down the console reader. The ConsoleReader cannot be
     * used after calling this method.
     */
    @Throws(IOException::class)
    override fun restore() {
        if (CLibraryPosix.tcsetattr(0, Termios.TCSANOW, original) != 0) {
            throw IOException("Can not reset terminal to defaults")
        }
    }

    /**
     * Returns number of columns in the terminal.
     */
    override val width: Int
        get() {
            return if (CLibraryPosix.ioctl(0, CLibraryPosix.TIOCGWINSZ, windowSizeBuffer) != 0) {
                DEFAULT_WIDTH
            }
            else {
                (0x000000FF and windowSizeBuffer[2] + (0x000000FF and windowSizeBuffer[3].toInt()) * 256).toShort().toInt()
            }
        }

    /**
     * Returns number of rows in the terminal.
     */
    override val height: Int
        get() {
            return if (CLibraryPosix.ioctl(0, CLibraryPosix.TIOCGWINSZ, windowSizeBuffer) != 0) {
                DEFAULT_HEIGHT
            }
            else {
                (0x000000FF and windowSizeBuffer[0] + (0x000000FF and windowSizeBuffer[1].toInt()) * 256).toShort().toInt()
            }
        }

    override fun doSetEchoEnabled(enabled: Boolean) {
        // have to re-get them, since flags change everything
        if (CLibraryPosix.tcgetattr(0, termInfo) != 0) {
            logger.error("Failed to get terminal info")
        }

        if (enabled) {
            termInfo.localFlags = termInfo.localFlags or Termios.Local.ECHO // ENABLE Echo input characters.
        }
        else {
            termInfo.localFlags = termInfo.localFlags and Termios.Local.ECHO.inv() // DISABLE Echo input characters.
        }

        if (CLibraryPosix.tcsetattr(0, Termios.TCSANOW, termInfo) != 0) {
            logger.error("Can not set terminal flags")
        }
    }

    override fun doSetInterruptEnabled(enabled: Boolean) {

        // have to re-get them, since flags change everything
        if (CLibraryPosix.tcgetattr(0, termInfo) != 0) {
            logger.error("Failed to get terminal info")
        }

        if (enabled) {
            termInfo.localFlags = termInfo.localFlags or Termios.Local.ISIG // ENABLE ctrl-C
        }
        else {
            termInfo.localFlags = termInfo.localFlags and Termios.Local.ISIG.inv() // DISABLE ctrl-C
        }
        if (CLibraryPosix.tcsetattr(0, Termios.TCSANOW, termInfo) != 0) {
            logger.error("Can not set terminal flags")
        }
    }

    override fun doRead(): Int {
        CLibraryPosix.read(0, inputRef, 1)
        return inputRef.value
    }
}
