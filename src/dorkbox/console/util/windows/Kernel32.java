/*
 * Copyright 2016 dorkbox, llc
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
package dorkbox.console.util.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class Kernel32 {
    static {
        Native.register("kernel32");
    }

    // see: http://msdn.microsoft.com/en-us/library/ms682013%28VS.85%29.aspx
    public static final short FOREGROUND_BLACK     = (short) 0x0000;
    public static final short FOREGROUND_BLUE      = (short) 0x0001;
    public static final short FOREGROUND_GREEN     = (short) 0x0002;
    public static final short FOREGROUND_CYAN      = (short) 0x0003;
    public static final short FOREGROUND_RED       = (short) 0x0004;
    public static final short FOREGROUND_MAGENTA   = (short) 0x0005;
    public static final short FOREGROUND_YELLOW    = (short) 0x0006;
    public static final short FOREGROUND_GREY      = (short) 0x0007;
    public static final short FOREGROUND_INTENSITY = (short) 0x0008; // foreground color is intensified.

    public static final short BACKGROUND_BLACK     = (short) 0x0000;
    public static final short BACKGROUND_BLUE      = (short) 0x0010;
    public static final short BACKGROUND_GREEN     = (short) 0x0020;
    public static final short BACKGROUND_CYAN      = (short) 0x0030;
    public static final short BACKGROUND_RED       = (short) 0x0040;
    public static final short BACKGROUND_MAGENTA   = (short) 0x0050;
    public static final short BACKGROUND_YELLOW    = (short) 0x0060;
    public static final short BACKGROUND_GREY      = (short) 0x0070;
    public static final short BACKGROUND_INTENSITY = (short) 0x0080; // background color is intensified.


    public static final short COMMON_LVB_LEADING_BYTE    = (short) 0x0100;
    public static final short COMMON_LVB_TRAILING_BYTE   = (short) 0x0200;
    public static final short COMMON_LVB_GRID_HORIZONTAL = (short) 0x0400;
    public static final short COMMON_LVB_GRID_LVERTICAL  = (short) 0x0800;
    public static final short COMMON_LVB_GRID_RVERTICAL  = (short) 0x1000;
    public static final short COMMON_LVB_REVERSE_VIDEO   = (short) 0x4000;
    public static final short COMMON_LVB_UNDERSCORE      = (short) 0x8000;


    private static final int FORMAT_MESSAGE_FROM_SYSTEM = 0x1000;

    public static final int STD_INPUT_HANDLE  = -10;
    public static final int STD_OUTPUT_HANDLE = -11;
    public static final int STD_ERROR_HANDLE  = -12;


    /**
     * https://msdn.microsoft.com/en-us/library/ms683231%28VS.85%29.aspx
     */
    public static native

    HANDLE GetStdHandle(int stdHandle);

    /**
     * https://msdn.microsoft.com/en-us/library/ms724211%28VS.85%29.aspx
     */
    public static native
    int CloseHandle(HANDLE handle);

    /**
     * https://msdn.microsoft.com/en-us/library/ms686047%28VS.85%29.aspx
     */
    public static native
    int SetConsoleTextAttribute(HANDLE consoleOutput, short attributes);

    /**
     * https://msdn.microsoft.com/en-us/library/windows/desktop/ms679351(v=vs.85).aspx
     */
    public static native
    int FormatMessageW(int flags, Pointer source, int messageId, int languageId, byte[] buffer, int size, long[] args);


    /**
     * https://msdn.microsoft.com/en-us/library/ms683171%28VS.85%29.aspx
     */
    public static native
    int GetConsoleScreenBufferInfo(HANDLE consoleOutput, CONSOLE_SCREEN_BUFFER_INFO consoleScreenBufferInfo);

    /**
     * https://msdn.microsoft.com/en-us/library/windows/desktop/ms686025(v=vs.85).aspx
     */
    public static native
    int SetConsoleCursorPosition(HANDLE consoleOutput, COORD.ByValue cursorPosition);

    /**
     * https://msdn.microsoft.com/en-us/library/windows/desktop/ms685107(v=vs.85).aspx
     */
    public static native
    int ScrollConsoleScreenBufferW(HANDLE consoleOutput, SMALL_RECT.ByReference scrollRect, SMALL_RECT.ByReference clipRect, COORD.ByValue destinationOrigin, IntByReference fillAttributes);




    /**
     * https://msdn.microsoft.com/en-us/library/ms682662%28VS.85%29.aspx
     */
    public static native
    int FillConsoleOutputAttribute(HANDLE consoleOutput, short attribute, int length, COORD.ByValue writeCoord, IntByReference numberOfAttrsWritten);

    /**
     * https://msdn.microsoft.com/en-us/library/ms682663%28VS.85%29.aspx
     */
    public static native
    int FillConsoleOutputCharacterW(HANDLE consoleOutput, char character, int length, COORD.ByValue writeCoord, IntByReference numberOfCharsWritten);



    /**
     * https://msdn.microsoft.com/en-us/library/ms683167%28VS.85%29.aspx
     */
    public static native
    int GetConsoleMode(HANDLE handle, IntByReference mode);

    /**
     * https://msdn.microsoft.com/en-us/library/ms686033%28VS.85%29.aspx
     */
    public static native
    int SetConsoleMode(HANDLE handle, int mode);


    /**
     * https://msdn.microsoft.com/en-us/library/ms684961(v=VS.85).aspx
     */
    public static native
    int ReadConsoleInputW(HANDLE handle, INPUT_RECORD.ByReference inputRecords, int length, IntByReference eventsCount);

    public static void ASSERT(final int returnValue, final String message) {
        // if returnValue == 0, throw assertion error
        assert returnValue != 0 : message + " : " + getLastErrorMessage();
    }


    private interface Win10 {
        boolean IsWindows10OrGreater();
    }

    private static
    String getLastErrorMessage() {
        int errorCode = Native.getLastError();
        int bufferSize = 160;
        byte data[] = new byte[bufferSize];
        FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM, Pointer.NULL, errorCode, 0, data, bufferSize, null);
        return new String(data);
    }

    /**
     * Windows 10+ supports ANSI according to microsoft
     */
    public static
    boolean isWindows10OrGreater() {
        try {
            final Object kernel32 = Native.loadLibrary("kernel32", Win10.class);
            if (kernel32 != null) {
                boolean isWin10Plus = ((Win10)kernel32).IsWindows10OrGreater();
                Native.unregister(Win10.class);
                return isWin10Plus;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
