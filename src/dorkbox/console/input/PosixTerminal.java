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
import java.nio.ByteBuffer;

import com.sun.jna.ptr.IntByReference;

import dorkbox.jna.linux.CLibraryPosix;
import dorkbox.jna.linux.structs.Termios;

/**
 * Terminal that is used for unix platforms. Terminal initialization is handled via JNA and ioctl/tcgetattr/tcsetattr/cfmakeraw.
 * <p>
 * This implementation should work for an reasonable POSIX system.
 */
public
class PosixTerminal extends SupportedTerminal {

    private final Termios original = new Termios();
    private Termios termInfo = new Termios();
    private ByteBuffer windowSizeBuffer = ByteBuffer.allocate(8);
    private final IntByReference inputRef = new IntByReference();

    public
    PosixTerminal() throws IOException {
        // save off the defaults
        if (CLibraryPosix.tcgetattr(0, this.original) != 0) {
            throw new IOException(CONSOLE_ERROR_INIT);
        }
        this.original.read();

        // CTRL-I (tab), CTRL-M (enter)  do not work

        if (CLibraryPosix.tcgetattr(0, this.termInfo) != 0) {
            throw new IOException(CONSOLE_ERROR_INIT);
        }
        this.termInfo.read();

        this.termInfo.inputFlags &= ~Termios.Input.IXON;   // DISABLE - output flow control mediated by ^S and ^Q
        this.termInfo.inputFlags &= ~Termios.Input.IXOFF;  // DISABLE - input flow control mediated by ^S and ^Q
        this.termInfo.inputFlags &= ~Termios.Input.BRKINT; // DISABLE - map BREAK to SIGINTR
        this.termInfo.inputFlags &= ~Termios.Input.INPCK;  // DISABLE - enable checking of parity errors
        this.termInfo.inputFlags &= ~Termios.Input.PARMRK; // DISABLE - mark parity and framing errors
        this.termInfo.inputFlags &= ~Termios.Input.ISTRIP; // DISABLE - strip 8th bit off chars
        this.termInfo.inputFlags |= Termios.Input.IGNBRK;  // ignore BREAK condition

        this.termInfo.localFlags &= ~Termios.Local.ICANON; // DISABLE - pass chars straight through to terminal instantly
        this.termInfo.localFlags |= Termios.Local.ECHOCTL;   // echo control chars as ^(Char)

        this.termInfo.controlFlags &= ~Termios.Control.CSIZE;  // REMOVE character size mask
        this.termInfo.controlFlags &= ~Termios.Control.PARENB; // DISABLE - parity enable

        this.termInfo.controlFlags |= Termios.Control.CS8;   // set character size mask 8 bits
        this.termInfo.controlFlags |= Termios.Control.CREAD; // enable receiver

        if (CLibraryPosix.tcsetattr(0, Termios.TCSANOW, this.termInfo) != 0) {
            throw new IOException("Can not set terminal flags");
        }
    }

    /**
     * Restore the original terminal configuration, which can be used when shutting down the console reader. The ConsoleReader cannot be
     * used after calling this method.
     */
    @Override
    public final
    void restore() throws IOException {
        if (CLibraryPosix.tcsetattr(0, Termios.TCSANOW, this.original) != 0) {
            throw new IOException("Can not reset terminal to defaults");
        }
    }

    /**
     * Returns number of columns in the terminal.
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public final
    int getWidth() {
        if (CLibraryPosix.ioctl(0, CLibraryPosix.TIOCGWINSZ, this.windowSizeBuffer) != 0) {
            return DEFAULT_WIDTH;
        }

        return (short) (0x000000FF & this.windowSizeBuffer.get(2) + (0x000000FF & this.windowSizeBuffer.get(3)) * 256);
    }

    /**
     * Returns number of rows in the terminal.
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public final
    int getHeight() {
        if (CLibraryPosix.ioctl(0, CLibraryPosix.TIOCGWINSZ, this.windowSizeBuffer) != 0) {
            return DEFAULT_HEIGHT;
        }

        return (short) (0x000000FF & this.windowSizeBuffer.get(0) + (0x000000FF & this.windowSizeBuffer.get(1)) * 256);
    }

    @Override
    protected
    void doSetEchoEnabled(final boolean enabled) {
        // have to re-get them, since flags change everything
        if (CLibraryPosix.tcgetattr(0, this.termInfo) != 0) {
            this.logger.error("Failed to get terminal info");
        }

        if (enabled) {
            this.termInfo.localFlags |= Termios.Local.ECHO; // ENABLE Echo input characters.
        }
        else {
            this.termInfo.localFlags &= ~Termios.Local.ECHO; // DISABLE Echo input characters.
        }

        if (CLibraryPosix.tcsetattr(0, Termios.TCSANOW, this.termInfo) != 0) {
            this.logger.error("Can not set terminal flags");
        }
    }

    @Override
    protected
    void doSetInterruptEnabled(final boolean enabled) {
        // have to re-get them, since flags change everything
        if (CLibraryPosix.tcgetattr(0, this.termInfo) != 0) {
            this.logger.error("Failed to get terminal info");
        }

        if (enabled) {
            this.termInfo.localFlags |= Termios.Local.ISIG;  // ENABLE ctrl-C
        }
        else {
            this.termInfo.localFlags &= ~Termios.Local.ISIG;  // DISABLE ctrl-C
        }

        if (CLibraryPosix.tcsetattr(0, Termios.TCSANOW, this.termInfo) != 0) {
            this.logger.error("Can not set terminal flags");
        }
    }

    @Override
    protected final
    int doRead() {
        CLibraryPosix.read(0, inputRef, 1);
        return inputRef.getValue();
    }
}
