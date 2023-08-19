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

import dorkbox.console.Console
import dorkbox.console.util.TerminalDetection
import dorkbox.os.OS
import org.slf4j.LoggerFactory
import java.io.IOException

@Suppress("unused")
abstract class Terminal internal constructor() {
    companion object {
        private val logger = LoggerFactory.getLogger(Console::class.java)
        val terminal: Terminal

        val EMPTY_LINE = CharArray(0)

        const val CONSOLE_ERROR_INIT = "Unable to initialize the input console."
        const val DEFAULT_WIDTH = 80
        const val DEFAULT_HEIGHT = 24


        init {
            var didFallbackE: Throwable? = null
            var term: Terminal

            try {
                term = when (Console.INPUT_CONSOLE_TYPE) {
                    TerminalDetection.MACOS   -> {
                        MacOsTerminal()
                    }
                    TerminalDetection.UNIX    -> {
                        PosixTerminal()
                    }
                    TerminalDetection.WINDOWS -> {
                        WindowsTerminal()
                    }
                    TerminalDetection.NONE    -> {
                        UnsupportedTerminal()
                    }
                    else                      -> {
                        // AUTO type.

                        // if these cannot be created, because we are in an IDE, an error will be thrown

                        if (OS.isMacOsX) {
                            MacOsTerminal()
                        } else {
                            val IS_CYGWIN = OS.isWindows && System.getenv("PWD") != null && System.getenv("PWD").startsWith("/")
                            val IS_MSYSTEM = OS.isWindows && System.getenv("MSYSTEM") != null && (System.getenv("MSYSTEM").startsWith("MINGW") || System.getenv("MSYSTEM") == "MSYS")
                            val IS_CONEMU = (OS.isWindows && System.getenv("ConEmuPID") != null)


                            if (OS.isWindows && !IS_CYGWIN && !IS_MSYSTEM && !IS_CONEMU) {
                                WindowsTerminal()
                            }
                            else {
                                PosixTerminal()
                            }
                        }
                    }
                }
            }
            catch (e: Exception) {
                didFallbackE = e
                term = UnsupportedTerminal()
            }

            terminal = term

            if (terminal is SupportedTerminal) {
                // enable echo and backspace
                terminal.setEchoEnabled(Console.ENABLE_ECHO)
                terminal.setInterruptEnabled(Console.ENABLE_INTERRUPT)

                val consoleThread = Thread(terminal)
                consoleThread.setDaemon(true)
                consoleThread.setName("Console Input Reader")
                consoleThread.start()

                // has to be NOT DAEMON thread, since it must run before the app closes.
                // alternatively, shut everything down when the JVM closes.
                val shutdownThread = object : Thread() {
                    override fun run() {
                        // called when the JVM is shutting down.
                        terminal.close()

                        try {
                            terminal.restore()
                            // this will 'hang' our shutdown, and honestly, who cares? We're shutting down anyways.
                            // inputConsole.reader.close(); // hangs on shutdown
                        }
                        catch (ignored: IOException) {
                            ignored.printStackTrace()
                        }
                    }
                }
                shutdownThread.setName("Console Input Shutdown")
                shutdownThread.setDaemon(true)
                Runtime.getRuntime().addShutdownHook(shutdownThread)
            }




            val debugEnabled = logger.isDebugEnabled

            if (didFallbackE != null && didFallbackE.message != CONSOLE_ERROR_INIT) {
                logger.error("Failed to construct terminal, falling back to unsupported.", didFallbackE)
            }
            else if (debugEnabled && term is UnsupportedTerminal) {
                logger.debug("Terminal is UNSUPPORTED (best guess). Unable to support single key input. Only line input available.")
            }
            else if (debugEnabled) {
                logger.debug("Created Terminal: ${terminal.javaClass.getSimpleName()} (${terminal.width}w x ${terminal.height}h)")
            }
        }
    }

    val logger = LoggerFactory.getLogger(javaClass)

    abstract fun doSetInterruptEnabled(enabled: Boolean)
    protected abstract fun doSetEchoEnabled(enabled: Boolean)

    @Throws(IOException::class)
    abstract fun restore()

    abstract val width: Int
    abstract val height: Int

    /**
     * Enables or disables CTRL-C behavior in the console
     */
    fun setInterruptEnabled(enabled: Boolean) {
        Console.ENABLE_INTERRUPT = enabled
        doSetInterruptEnabled(enabled)
    }

    /**
     * Enables or disables character echo to stdout
     */
    fun setEchoEnabled(enabled: Boolean) {
        Console.ENABLE_ECHO = enabled
        doSetEchoEnabled(enabled)
    }

    /**
     * Reads single character input from the console.
     *
     * @return -1 if no data or problems
     */
    abstract fun read(): Int

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    abstract fun readLineChars(): CharArray

    /**
     * Reads a single line of characters, defined as everything before the 'ENTER' key is pressed
     *
     * @return the string contents of a line (empty if there is no characters)
     */
    fun readLine(): String {
        val line = readLineChars()
        return String(line)
    }

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    fun readLinePassword(): CharArray {
        // don't bother in an IDE. it won't work.
        val echoEnabled = Console.ENABLE_ECHO
        Console.ENABLE_ECHO = false

        val readLine0 = readLineChars()
        Console.ENABLE_ECHO = echoEnabled
        return readLine0
    }

    /**
     * releases any thread still waiting.
     */
    abstract fun close()
}
