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

import java.nio.ByteBuffer;

import com.sun.jna.Library;

@SuppressWarnings("ALL")
interface PosixTerminalControl extends Library {

    public static final int TCSANOW = 0;
    public static final int TBUFLEN = 124;

    // Definitions at: http://linux.die.net/man/3/termios
    // also: http://code.metager.de/source/xref/DragonFly-BSD/sys/sys/termios.h
    public static final int IGNBRK = 0x00000001; /* ignore BREAK condition */
    public static final int BRKINT = 0x00000002; /* map BREAK to SIGINTR */


    public static final int ISIG = 0000001;
    public static final int ICANON = 0000002;
    public static final int ECHO = 0000010;
    public static final int IXON = 0002000;


    public static final int VINTR = 0;
    public static final int VQUIT = 1;
    public static final int VERASE = 2;
    public static final int VKILL = 3;
    public static final int VEOF = 4;
    public static final int VTIME = 5;
    public static final int VMIN = 6;
    public static final int VSWTC = 7;
    public static final int VSTART = 8;
    public static final int VSTOP = 9;
    public static final int VSUSP = 10;
    public static final int VEOL = 11;
    public static final int VREPRINT = 12;
    public static final int VDISCARD = 13;
    public static final int VWERASE = 14;
    public static final int VLNEXT = 15;
    public static final int VEOL2 = 16;

    // MAGIC!
    public static final int TIOCGWINSZ = System.getProperty("os.name").equalsIgnoreCase("linux") ? 0x5413 : 1074295912;

    public int open(String path, int flags);

    public int close(int fd);

    /**
     * Original signature : <code>int ioctl(int, int, char*)</code><br>
     */
    public int ioctl(int d, int request, ByteBuffer data);

    /**
     * Put the state of FD into *TERMIOS_P.<br>
     *
     * Original signature : <code>int tcgetattr(int, char*)</code><br>
     */
    public int tcgetattr(int fd, TermiosStruct termios_p);

    /**
     * Set the state of FD to *TERMIOS_P.<br>
     *
     * Values for OPTIONAL_ACTIONS (TCSA*) are in <bits/termios.h>.<br>
     *
     * Original signature : <code>int tcsetattr(int, int, char*)</code><br>
     */
    public int tcsetattr(int fd, int optional_actions, TermiosStruct termios_p);
}
