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
package dorkbox.util.input.posix;

import com.sun.jna.Native;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;

import dorkbox.util.input.Encoding;
import dorkbox.util.input.Terminal;

/**
 * Terminal that is used for unix platforms. Terminal initialization is handled via JNA and
 * ioctl/tcgetattr/tcsetattr/cfmakeraw.
 *
 * This implementation should work for an reasonable POSIX system.
 */
public class UnixTerminal extends Terminal {

  private volatile TermiosStruct termInfoDefault = new TermiosStruct();
  private volatile TermiosStruct termInfo = new TermiosStruct();

  private final Reader reader;

  private final PosixTerminalControl term;
  private ByteBuffer windowSizeBuffer = ByteBuffer.allocate(8);


  public UnixTerminal() throws Exception {
    String encoding = Encoding.get();
    this.reader = new InputStreamReader(System.in, encoding);

    this.term = (PosixTerminalControl) Native.loadLibrary("c", PosixTerminalControl.class);

    // save off the defaults
    if (this.term.tcgetattr(0, this.termInfoDefault) != 0) {
      throw new IOException("Failed to get terminal info");
    }
  }

  @Override
  public void init() throws IOException {

    // COMPARABLE TO (from upstream)
    //settings.set("-icanon min 1 -ixon");
    //settings.set("dsusp undef");

        /*
         * NOT done in constructor, since our unit test DOES NOT use this!
         *
         * Set the console to be character-buffered instead of line-buffered.
         * Allow ctrl-s and ctrl-q keypress to be used (as forward search)
         */

// raw mode
//        t->c_iflag &= ~(IMAXBEL|IXOFF|INPCK|BRKINT|PARMRK|ISTRIP|INLCR|IGNCR|ICRNL|IXON|IGNPAR);
//        t->c_iflag |= IGNBRK;
//        t->c_oflag &= ~OPOST;
//        t->c_lflag &= ~(ECHO|ECHOE|ECHOK|ECHONL|ICANON|ISIG|IEXTEN|NOFLSH|TOSTOP|PENDIN);
//        t->c_cflag &= ~(CSIZE|PARENB);
//        t->c_cflag |= CS8|CREAD;
//        t->c_cc[VMIN] = 1;
//        t->c_cc[VTIME] = 0;

    if (this.term.tcgetattr(0, this.termInfo) != 0) {
      throw new IOException("Failed to get terminal info");
    }

    this.termInfo.c_iflag &= ~PosixTerminalControl.IXON; // DISABLE - flow control mediated by ^S and ^Q
//        struct.c_iflag |= PosixTerminalControl.IUTF8; // DISABLE - flow control mediated by ^S and ^Q

    this.termInfo.c_lflag &=
        ~PosixTerminalControl.ICANON; // DISABLE - canonical mode (pass chars straight through to terminal)
//        struct.c_lflag &= ~PosixTerminalControl.ISIG; // DISABLE - When any of the characters INTR, QUIT, SUSP, or DSUSP are received, generate the corresponding signal.

    // If MIN > 0 and TIME = 0, MIN sets the number of characters to receive before the read is satisfied. As TIME is zero, the timer is not used.
    this.termInfo.c_cc[PosixTerminalControl.VMIN] = 1;  // Minimum number of characters for noncanonical read (MIN).
    this.termInfo.c_cc[PosixTerminalControl.VTIME] = 0;  // Timeout in deciseconds for noncanonical read (TIME).

    this.termInfo.c_cc[PosixTerminalControl.VSUSP] = 0; // suspend disabled
    this.termInfo.c_cc[PosixTerminalControl.VEOF] = 0; // eof disabled
    this.termInfo.c_cc[PosixTerminalControl.VEOL] = 0; // eol disabled

    if (this.term.tcsetattr(0, PosixTerminalControl.TCSANOW, this.termInfo) != 0) {
      throw new IOException("Can not set terminal flags");
    }
  }

  /**
   * Restore the original terminal configuration, which can be used when shutting down the console reader. The
   * ConsoleReader cannot be used after calling this method.
   */
  @Override
  public final void restore() throws IOException {
    if (this.term.tcsetattr(0, PosixTerminalControl.TCSANOW, this.termInfoDefault) != 0) {
      throw new IOException("Can not reset terminal to defaults");
    }
  }

  /**
   * Returns number of columns in the terminal.
   */
  @Override
  public final int getWidth() {
    if (this.term.ioctl(0, PosixTerminalControl.TIOCGWINSZ, this.windowSizeBuffer) != 0) {
      return DEFAULT_WIDTH;
    }

    return (short) (0x000000FF & this.windowSizeBuffer.get(2) + (0x000000FF & this.windowSizeBuffer.get(3)) * 256);
  }

  /**
   * Returns number of rows in the terminal.
   */
  @Override
  public final int getHeight() {
    if (this.term.ioctl(0, PosixTerminalControl.TIOCGWINSZ, this.windowSizeBuffer) != 0) {
      return DEFAULT_HEIGHT;
    }

    return
        (short) (0x000000FF & this.windowSizeBuffer.get(0) + (0x000000FF & this.windowSizeBuffer.get(1)) * 256);
  }

  @Override
  public final synchronized void setEchoEnabled(final boolean enabled) {
    // have to reget them, since flags change everything
    if (this.term.tcgetattr(0, this.termInfo) != 0) {
      this.logger.error("Failed to get terminal info");
    }

    if (enabled) {
      this.termInfo.c_lflag |= PosixTerminalControl.ECHO; // ENABLE Echo input characters.
    } else {
      this.termInfo.c_lflag &= ~PosixTerminalControl.ECHO; // DISABLE Echo input characters.
    }

    if (this.term.tcsetattr(0, PosixTerminalControl.TCSANOW, this.termInfo) != 0) {
      this.logger.error("Can not set terminal flags");
    }

    super.setEchoEnabled(enabled);
  }

//    public final void disableInterruptCharacter() {
//        // have to re-get them, since flags change everything
//        if (this.term.tcgetattr(0, this.termInfo) !=0) {
//            this.logger.error("Failed to get terminal info");
//        }
//
//        this.termInfo.c_cc[PosixTerminalControl.VINTR] = 0; // interrupt disabled
//
//        if (this.term.tcsetattr(0, PosixTerminalControl.TCSANOW, this.termInfo) != 0) {
//            this.logger.error("Can not set terminal flags");
//        }
//    }
//
//    public final void enableInterruptCharacter() {
//        // have to re-get them, since flags change everything
//        if (this.term.tcgetattr(0, this.termInfo) !=0) {
//            this.logger.error("Failed to get terminal info");
//        }
//
//        this.termInfo.c_cc[PosixTerminalControl.VINTR] = 3; // interrupt is ctrl-c
//
//        if (this.term.tcsetattr(0, PosixTerminalControl.TCSANOW, this.termInfo) != 0) {
//            this.logger.error("Can not set terminal flags");
//        }
//    }

  @Override
  public final int read() {
    try {
      return this.reader.read();
    } catch (IOException ignored) {
      return -1;
    }
  }
}
