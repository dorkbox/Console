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
package dorkbox.console;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import dorkbox.console.output.AnsiOutputStream;
import dorkbox.console.output.WindowsAnsiOutputStream;
import dorkbox.console.util.posix.CLibraryPosix;
import dorkbox.console.util.windows.Kernel32;
import dorkbox.util.Property;

/**
 * Provides a fluent API for generating ANSI escape sequences and providing access to streams that support it.
 * <p>
 * See: https://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * @author dorkbox, llc
 */
public
class Console {


    /**
     * If true, allows an ANSI output stream to be created, otherwise a NO-OP stream is created instead
     */
    @Property
    public static boolean ENABLE_ANSI = true;

    /**
     * If true, then we always force the raw ANSI output stream to be enabled (even if the output stream is not aware of ANSI commands).
     * This can be used to obtain the raw ANSI escape codes for other color aware programs (ie: less -r)
     */
    @Property
    public static boolean FORCE_ENABLE_ANSI = false;

    /**
     * Enables or disables character echo to stdout in the console, should call {@link #setEchoEnabled(boolean)} after initialization
     */
    @Property
    public static volatile boolean ENABLE_ECHO = true;

    /**
     * Enables or disables CTRL-C behavior in the console, should call {@link #setInterruptEnabled(boolean)} after initialization
     */
    @Property
    public static volatile boolean ENABLE_INTERRUPT = false;

    @Property
    public static char PASSWORD_ECHO_CHAR = '*';

    /**
     * Enables the backspace key to delete characters in the line buffer and (if ANSI is enabled) from the screen.
     */
    @Property
    public final static boolean ENABLE_BACKSPACE = true;



    // OS types supported by the input console. Default is AUTO
    public static final String AUTO = "auto";
    public static final String UNIX = "unix";
    public static final String WINDOWS = "windows";

    public static final String NONE = "none";  // this is the same as unsupported

    // valid are what's above
    @Property
    public static final String INPUT_CONSOLE_TYPE = "AUTO";



    private static final PrintStream original_out = System.out;
    private static final PrintStream original_err = System.err;

    // protected by synchronize
    private static int installed = 0;
    private static PrintStream out;
    private static PrintStream err;



    /**
     * Gets the version number.
     */
    public static
    String getVersion() {
        return "2.9";
    }

    /**
     * Reads single character input from the console.
     *
     * @return -1 if no data or problems
     */
    public static
    int read() {
        return Input.read();
    }

    /**
     * Reads a line of characters from the console, defined as everything before the 'ENTER' key is pressed
     *
     * @return null if no data
     */
    public static
    String readLine() {
        return Input.readLine();
    }


    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    public static
    char[] readLineChars() {
        return Input.readLineChars();
    }

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    public static
    char[] readPassword() {
        return Input.readLinePassword();
    }

    /**
     * Enables or disables CTRL-C behavior in the console
     */
    public static
    void setInterruptEnabled(final boolean enabled) {
        Console.ENABLE_INTERRUPT = enabled;
        Input.setInterruptEnabled(enabled);
    }

    /**
     * Enables or disables character echo to stdout
     */
    public static
    void setEchoEnabled(final boolean enabled) {
        Console.ENABLE_ECHO = enabled;
        Input.setEchoEnabled(enabled);
    }

    /**
     * Override System.err and System.out with an ANSI capable {@link java.io.PrintStream}.
     */
    public static synchronized
    void systemInstall() {
        installed++;
        if (installed == 1) {
            out = out();
            err = err();

            System.setOut(out);
            System.setErr(err);
        }
    }

    /**
     * un-does a previous {@link #systemInstall()}.
     * <p>
     * If {@link #systemInstall()} was called multiple times, then {@link #systemUninstall()} must be called the same number of
     * times before it is uninstalled.
     */
    public static synchronized
    void systemUninstall() {
        installed--;
        if (installed == 0) {
            if (out != null && out != original_out) {
                out.close();
                System.setOut(original_out);
            }

            if (err != null && err != original_err) {
                err.close();
                System.setErr(original_err);
            }
        }
    }

    /**
     * If we are installed to the system (IE: System.err/out, then reset those streams, otherwise there is nothing to do from a static
     * perspective (since creating a NEW ANSI stream will automatically reset the output
     */
    public static synchronized
    void reset() {
        if (installed >= 1) {
            // TODO: make this reset readLine, etc as well?
            try {
                System.out.write(AnsiOutputStream.RESET_CODE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * If the standard out natively supports ANSI escape codes, then this just returns System.out, otherwise it will provide an ANSI
     * aware PrintStream which strips out the ANSI escape sequences or which implement the escape sequences.
     *
     * @return a PrintStream which is ANSI aware.
     */
    public static
    PrintStream out() {
        if (out == null) {
            out = createPrintStream(original_out, 1); // STDOUT_FILENO
        }
        return out;
    }

    /**
     * If the standard out natively supports ANSI escape codes, then this just returns System.err, otherwise it will provide an ANSI aware
     * PrintStream which strips out the ANSI escape sequences or which implement the escape sequences.
     *
     * @return a PrintStream which is ANSI aware.
     */
    public static
    PrintStream err() {
        if (err == null) {
            err = createPrintStream(original_err, 2); // STDERR_FILENO
        }
        return err;
    }









    private static boolean isXterm() {
        String term = System.getenv("TERM");
        return "xterm".equalsIgnoreCase(term);
    }

    private static
    PrintStream createPrintStream(final OutputStream stream, int fileno) {
        if (!ENABLE_ANSI) {
            // Use the ANSIOutputStream to strip out the ANSI escape sequences.
            return new PrintStream(new AnsiOutputStream(stream));
        }

        if (!isXterm()) {
            String os = System.getProperty("os.name");
            if (os.startsWith("Windows")) {
                // check if windows10+ (which natively supports ANSI)
                if (Kernel32.isWindows10OrGreater()) {
                    // Just wrap it up so that when we get closed, we reset the attributes.
                    return deafultPrintStream(stream);
                }

                // On windows we know the console does not interpret ANSI codes..
                try {
                    return new PrintStream(new WindowsAnsiOutputStream(stream, fileno));
                } catch (Throwable ignore) {
                    // this happens when JNA is not in the path.. or
                    // this happens when the stdout is being redirected to a file.
                    // this happens when the stdout is being redirected to different console.
                }

                // Use the ANSIOutputStream to strip out the ANSI escape sequences.
                if (!FORCE_ENABLE_ANSI) {
                    return new PrintStream(new AnsiOutputStream(stream));
                }
            } else {
                // We must be on some unix variant..
                try {
                    // If we can detect that stdout is not a tty.. then setup to strip the ANSI sequences..
                    if (!FORCE_ENABLE_ANSI && CLibraryPosix.isatty(fileno) == 0) {
                        return new PrintStream(new AnsiOutputStream(stream));
                    }
                } catch (Throwable ignore) {
                    // These errors happen if the JNI lib is not available for your platform.
                }
            }
        }

        // By default we assume the terminal can handle ANSI codes.
        // Just wrap it up so that when we get closed, we reset the attributes.
        return deafultPrintStream(stream);
    }

    private static
    PrintStream deafultPrintStream(final OutputStream stream) {
        return new PrintStream(new FilterOutputStream(stream) {
            @Override
            public
            void close() throws IOException {
                write(AnsiOutputStream.RESET_CODE);
                flush();
                super.close();
            }
        });
    }
}
