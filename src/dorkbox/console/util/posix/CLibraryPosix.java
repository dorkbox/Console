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

import java.nio.ByteBuffer;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

@SuppressWarnings("ALL")
public
class CLibraryPosix {
    static {
        Native.register("c");
    }

    // MAGIC!
    public static final int TIOCGWINSZ = System.getProperty("os.name").equalsIgnoreCase("linux") ? 0x5413 : 1074295912;


    public static native
    int isatty(int fd);

    public static native
    int read(int fd, IntByReference c, int count);

    /**
     * Original signature : <code>int ioctl(int, int, char*)</code><br>
     */
    public static native
    int ioctl(int d, int request, ByteBuffer data);

    /**
     * Put the state of FD into *TERMIOS_P.<br>
     * <p>
     * Original signature : <code>int tcgetattr(int, char*)</code><br>
     */
    public static native
    int tcgetattr(int fd, Termios termios_p);

    /**
     * Set the state of FD to *TERMIOS_P.<br>
     * <p>
     * Values for OPTIONAL_ACTIONS (TCSA*) are in <bits/termios.h>.<br>
     * <p>
     * Original signature : <code>int tcsetattr(int, int, char*)</code><br>
     */
    public static native
    int tcsetattr(int fd, int optional_actions, Termios termios_p);
}
