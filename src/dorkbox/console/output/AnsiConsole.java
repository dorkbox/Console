/**
 * Copyright (C) 2009, Progress Software Corporation and/or its
 * subsidiaries or affiliates.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a asValue of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dorkbox.console.output;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import dorkbox.console.util.posix.CLibraryPosix;
import dorkbox.console.util.windows.Kernel32;

/**
 * Provides consistent access to an ANSI aware console PrintStream.
 *
 * See: https://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * @author Dorkbox, LLC
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@SuppressWarnings("SpellCheckingInspection")
class AnsiConsole {

    static final int STDOUT_FILENO = 1;
    static final int STDERR_FILENO = 2;

    public static final PrintStream system_out = System.out;
    public static final PrintStream out = new PrintStream(wrapOutputStream(system_out, STDOUT_FILENO));

    public static final PrintStream system_err = System.err;
    public static final PrintStream err = new PrintStream(wrapOutputStream(system_err, STDERR_FILENO));

    private static boolean isXterm() {
        String term = System.getenv("TERM");
        return term != null && term.equals("xterm");
    }

    private static
    OutputStream wrapOutputStream(final OutputStream stream, int fileno) {

        // If the jansi.passthrough property is set, then don't interpret
        // any of the ansi sequences.
        if (Boolean.getBoolean("jansi.passthrough")) {
            return stream;
        }

        // If the jansi.strip property is set, then we just strip the
        // the ansi escapes.
        if (Boolean.getBoolean("jansi.strip")) {
            return new AnsiOutputStream(stream);
        }

        String os = System.getProperty("os.name");
        if (os.startsWith("Windows") && !isXterm()) {

            // check if windows10+ (which natively supports ANSI)
            if (Kernel32.isWindows10OrGreater()) {
                // Just wrap it up so that when we get closed, we reset the attributes.
                return new FilterOutputStream(stream) {
                    @Override
                    public
                    void close() throws IOException {
                        write(AnsiOutputStream.RESET_CODE);
                        flush();
                        super.close();
                    }
                };
            }

            // On windows we know the console does not interpret ANSI codes..
            try {
                return new WindowsAnsiOutputStream(stream, fileno);
            } catch (Throwable ignore) {
                ignore.printStackTrace();
                // this happens when JNA is not in the path.. or
                // this happens when the stdout is being redirected to a file.
                // this happens when the stdout is being redirected to different console.
            }

            // Use the ANSIOutputStream to strip out the ANSI escape sequences.
            return new AnsiOutputStream(stream);
        }

        // We must be on some unix variant..
        try {
            // If the jansi.force property is set, then we force to output
            // the ansi escapes for piping it into ansi color aware commands (e.g. less -r)
            boolean forceColored = Boolean.getBoolean("jansi.force");

            // If we can detect that stdout is not a tty.. then setup to strip the ANSI sequences..
            int rc = CLibraryPosix.isatty(fileno);
            if (!isXterm() && !forceColored && rc == 0) {
                return new AnsiOutputStream(stream);
            }

            // These errors happen if the JNI lib is not available for your platform.
        } catch (NoClassDefFoundError ignore) {
        } catch (UnsatisfiedLinkError ignore) {
        }

        // By default we assume your Unix tty can handle ANSI codes.
        // Just wrap it up so that when we get closed, we reset the attributes.
        return new FilterOutputStream(stream) {
            @Override
            public
            void close() throws IOException {
                write(AnsiOutputStream.RESET_CODE);
                flush();
                super.close();
            }
        };
    }

    /**
     * If the standard out natively supports ANSI escape codes, then this just
     * returns System.out, otherwise it will provide an ANSI aware PrintStream
     * which strips out the ANSI escape sequences or which implement the escape
     * sequences.
     *
     * @return a PrintStream which is ANSI aware.
     */
    public static
    PrintStream out() {
        return out;
    }

    /**
     * If the standard out natively supports ANSI escape codes, then this just
     * returns System.err, otherwise it will provide an ANSI aware PrintStream
     * which strips out the ANSI escape sequences or which implement the escape
     * sequences.
     *
     * @return a PrintStream which is ANSI aware.
     */
    public static
    PrintStream err() {
        return err;
    }
}
