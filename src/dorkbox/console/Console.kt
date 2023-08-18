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
package dorkbox.console

import dorkbox.console.input.Input
import dorkbox.console.input.Terminal
import dorkbox.console.output.Ansi
import dorkbox.console.output.AnsiOutputStream
import dorkbox.propertyLoader.Property
import dorkbox.updates.Updates.add
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream

/**
 * Provides access to single character input streams and ANSI capable output streams.
 *
 * @author dorkbox, llc
 */
@Suppress("unused")
object Console {
    /**
     * If true, allows an ANSI output stream to be created on System.out/err, If true, allows an ANSI output stream to be created on
     * System.out/err, otherwise it will provide an ANSI aware PrintStream which strips out the ANSI escape sequences.
     */
    @Property
    var ENABLE_ANSI = true

    /**
     * If true, then we always force the raw ANSI output stream to be enabled (even if the output stream is not aware of ANSI commands).
     * This can be used to obtain the raw ANSI escape codes for other color aware programs (ie: less -r)
     */
    @Property
    var FORCE_ENABLE_ANSI = false

    /**
     * Enables or disables character echo to stdout in the console, should call [Terminal.setEchoEnabled] after
     * initialization
     */
    @Property
    @Volatile
    var ENABLE_ECHO = true

    /**
     * Enables or disables CTRL-C behavior in the console, should call [Terminal.setInterruptEnabled] after initialization
     */
    @Property
    @Volatile
    var ENABLE_INTERRUPT = false

    /**
     * Enables the backspace key to delete characters in the line buffer and (if ANSI is enabled) from the screen.
     */
    @Property
    val ENABLE_BACKSPACE = true

    /**
     * Used to determine what console to use/hook when AUTO is not correctly working.
     * Valid options are:
     * AUTO - automatically determine which OS/console type to use
     * UNIX - try to control a UNIX console
     * WINDOWS - try to control a WINDOWS console
     * NONE - do not try to control anything, only line input is supported
     */
    @Property
    val INPUT_CONSOLE_TYPE = "AUTO"

    /**
     * Gets the version number.
     */
    const val version = "3.10"


    init {
        // Add this project to the updates system, which verifies this class + UUID + version information
        add(Console::class.java, "030fa739af4e4698ba99cf275a69d230", version)
    }

    /**
     * If the standard in supports single character input, then a terminal will be returned that supports it, otherwise a buffered (aka
     * 'normal') input will be returned
     *
     * @return a terminal that supports single character input or the default buffered input
     */
    fun `in`(): Terminal {
        return Input.terminal
    }

    /**
     * If the standard in supports single character input, then an InputStream will be returned that supports it, otherwise a buffered (aka
     * 'normal') InputStream will be returned
     *
     * @return an InputStream that supports single character input or the default buffered input
     */
    fun inputStream(): InputStream {
        return Input.wrappedInputStream
    }

    /**
     * Initializes and hooks output streams, necessary when using ANSI for the first time inside of an output stream (as it initializes
     * after assignment).
     *
     *
     * This is not needed for input streams, since they do not hook System.err/out.
     */
    fun hookSystemOutputStreams() {
        out()
        err()
    }

    /**
     * If the standard out natively supports ANSI escape codes, then this just returns System.out (wrapped to reset ANSI stream on close),
     * otherwise it will provide an ANSI aware PrintStream which strips out the ANSI escape sequences.
     *
     * @return a PrintStream which is ANSI aware.
     */
    fun out(): PrintStream {
        return Ansi.out
    }

    /**
     * If the standard out natively supports ANSI escape codes, then this just returns System.err (wrapped to reset ANSI stream on close),
     * otherwise it will provide an ANSI aware PrintStream which strips out the ANSI escape sequences.
     *
     * @return a PrintStream which is ANSI aware.
     */
    fun err(): PrintStream {
        return Ansi.err
    }

    /**
     * If we are installed to the system (IE: System.err/out, then reset those streams, otherwise there is nothing to do from a static
     * perspective (since creating a NEW ANSI stream will automatically reset the output
     */
    fun reset() {
        try {
            Ansi.out.write(AnsiOutputStream.RESET_CODE)
            Ansi.err.write(AnsiOutputStream.RESET_CODE)
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
