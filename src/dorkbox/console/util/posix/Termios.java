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
package dorkbox.console.util.posix;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

@SuppressWarnings("ALL")
public
class Termios extends Structure {
    // NOTE: MUST BE BITS!! from: /usr/include/x86_64-linux-gnu/bits/termios.h
    // the one in octal WILL NOT WORK!! (you have been warned)

    // Definitions at: http://linux.die.net/man/3/termios

    // Input flags - software input processing
    public static class Input {
        public static final int 	IGNBRK = 0000001; // ignore BREAK condition
        public static final int 	BRKINT = 0000002; // map BREAK to SIGINTR
        public static final int 	IGNPAR = 0000004; // ignore (discard) parity errors
        public static final int 	PARMRK = 0000010; // mark parity and framing errors
        public static final int 	 INPCK = 0000020; // enable checking of parity errors
        public static final int 	ISTRIP = 0000040; // strip 8th bit off chars
        public static final int      INLCR = 0000100; // map NL into CR
        public static final int      IGNCR = 0000200; // ignore CR
        public static final int      ICRNL = 0000400; // map CR to NL (ala CRMOD)
     // public static final int      IUCLC = 0001000; // (not in POSIX) Map uppercase characters to lowercase on input.
        public static final int       IXON = 0002000; // enable output flow control
        public static final int      IXANY = 0004000; // any char will restart after stop
        public static final int      IXOFF = 0010000; // enable input flow control
        public static final int    IMAXBEL = 0020000; // ring bell on input queue full
     // public static final int      IUTF8 = 0040000; // (since Linux 2.6.4) (not in POSIX) Input is UTF8; this allows character-erase to be correctly performed in cooked mode.
    }

    public static class Output {
        // Output flags - software output processing
        public static final int  OPOST = 0000001; // enable following output processing (not set = raw output)
     // public static final int  OLCUC = 0000002; // (not in POSIX) Map lowercase characters to uppercase on output.
        public static final int  ONLCR = 0000004; // map NL to CR-NL (ala CRMOD)
        public static final int  OCRNL = 0000010; // map CR to NL on output
        public static final int  ONOCR = 0000020; // no CR output at column 0
        public static final int ONLRET = 0000040; // NL performs CR function
        public static final int  OFILL = 0000100; // Send fill characters for a delay, rather than using a timed delay.
        public static final int  OFDEL = 0000200; // Fill character is ASCII DEL (0177).  If unset, fill character is ASCII NUL ('\0').  (Not implemented on Linux.)
    }


    public static class Control {
        // Control flags - hardware control of terminal
        public static final int      CSIZE = 0000060; // character size mask
        public static final int        CS5 = 0000000; //   5 bits (pseudo)
        public static final int        CS6 = 0000020; //   6 bits
        public static final int        CS7 = 0000040; //   7 bits
        public static final int        CS8 = 0000060; //   8 bits
        public static final int     CSTOPB = 0000100; // send 2 stop bits
        public static final int      CREAD = 0000200; // enable receiver
        public static final int     PARENB = 0000400; // parity enable
        public static final int     PARODD = 0001000; // odd parity, else even
        public static final int      HUPCL = 0002000; // hang up on last close
        public static final int     CLOCAL = 0004000; // ignore modem status lines
    }


    public static class Local {
        // "Local" flags - dumping ground for other state
        // Warning: some flags in this structure begin with the letter "I" and look like they belong in the input flag.
        public static final int       ISIG = 0000001; // enable signals INTR, QUIT, [D]SUSP
        public static final int     ICANON = 0000002; // canonicalize input lines
      //public static final int      XCASE = 0000004; // (not in POSIX; not supported under Linux)
        public static final int       ECHO = 0000010; // enable echoing
        public static final int      ECHOE = 0000020; // visually erase chars
        public static final int      ECHOK = 0000040; // echo NL after line kill
        public static final int     ECHONL = 0000100; // echo NL even if ECHO is off
        public static final int     NOFLSH = 0000200; // don't flush after interrupt
        public static final int     TOSTOP = 0000400; // stop background jobs from output
        public static final int    ECHOCTL = 0001000; // echo control chars as ^(Char)
        public static final int    ECHOPRT = 0002000; // visual erase mode for hardcopy
        public static final int     ECHOKE = 0004000; // visual erase for line kill
        public static final int     FLUSHO = 0001000; // output being flushed (state)
        public static final int     PENDIN = 0004000; // XXX retype pending input (state)
        public static final int     IEXTEN = 0100000; // enable DISCARD and LNEXT
        public static final int    EXTPROC = 0200000; // external processing
    }


    public static class ControlChars {
        // Special Control Characters
        //
        // the value is the index into c_cc[] character array.
        public static final int    VINTR = 0; //  ISIG
        public static final int    VQUIT = 1; //  ISIG
        public static final int   VERASE = 2; //  ICANON
        public static final int    VKILL = 3; //  ICANON
        public static final int     VEOF = 4; //  ICANON
        public static final int    VTIME = 5; //   !ICANON
        public static final int     VMIN = 6; //   !ICANON
        public static final int    VSWTC = 7;
        public static final int   VSTART = 8; //  IXON, IXOFF
        public static final int    VSTOP = 9; //  IXON, IXOFF
        public static final int    VSUSP = 10;//  ISIG
        public static final int     VEOL = 11;//  ICANON
        public static final int VREPRINT = 12;//  ICANON together with IEXTEN
        public static final int VDISCARD = 13;
        public static final int  VWERASE = 14;//  ICANON together with IEXTEN
        public static final int   VLNEXT = 15;//  IEXTEN
        public static final int    VEOL2 = 16;//  ICANON together with IEXTEN
    }




    public static final int TCSANOW = 0;

    /**
     * input mode flags
     */
    public int inputFlags;

    /**
     * output mode flags
     */
    public int outputFlags;

    /**
     * control mode flags
     */
    public int controlFlags;

    /**
     * local mode flags
     */
    public int localFlags;

   	/**
     *  line discipline
     */
    public char lineDiscipline;

    /**
     * control characters
     */
    public byte[] controlChars = new byte[32];

    /**
     * input speed
     */
    public int inputSpeed;

    /**
     * output speed
     */
    public int outputSpeed;

    public
    Termios() {}

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("inputFlags",
                             "outputFlags",
                             "controlFlags",
                             "localFlags",
                             "lineDiscipline",
                             "controlChars",
                             "inputSpeed",
                             "outputSpeed");
    }
}
