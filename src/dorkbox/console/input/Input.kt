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
import dorkbox.os.OS.isWindows
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream

object Input {
    private val logger = LoggerFactory.getLogger(Console::class.java)
    val terminal: Terminal

    init {
        val type = Console.INPUT_CONSOLE_TYPE.uppercase()
        var didFallbackE: Throwable? = null
        var term: Terminal

        try {
            term = if (type == "UNIX") {
                PosixTerminal()
            }
            else if (type == "WINDOWS") {
                WindowsTerminal()
            }
            else if (type == "NONE") {
                UnsupportedTerminal()
            }
            else {
                // AUTO type.

                // if these cannot be created, because we are in an IDE, an error will be thrown

                val IS_CYGWIN = isWindows && System.getenv("PWD") != null && System.getenv("PWD").startsWith("/")
                val IS_MSYSTEM = isWindows && System.getenv("MSYSTEM") != null && (System.getenv("MSYSTEM")
                    .startsWith("MINGW") || System.getenv("MSYSTEM") == "MSYS")
                val IS_CONEMU = (isWindows && System.getenv("ConEmuPID") != null)


                if (isWindows && !IS_CYGWIN && !IS_MSYSTEM && !IS_CONEMU) {
                    WindowsTerminal()
                }
                else {
                    PosixTerminal()
                }
            }
        }
        catch (e: Exception) {
            didFallbackE = e
            term = UnsupportedTerminal()
        }

        terminal = term

        val debugEnabled = logger.isDebugEnabled

        if (didFallbackE != null && didFallbackE.message != Terminal.CONSOLE_ERROR_INIT) {
            logger.error("Failed to construct terminal, falling back to unsupported.", didFallbackE)
        }
        else if (debugEnabled && term is UnsupportedTerminal) {
            logger.debug("Terminal is UNSUPPORTED (best guess). Unable to support single key input. Only line input available.")
        }
        else if (debugEnabled) {
            logger.debug(
                "Created Terminal: {} ({}w x {}h)", terminal.javaClass.getSimpleName(), terminal.width, terminal.height
            )
        }

        if (term is SupportedTerminal) {
            // echo and backspace
            term.setEchoEnabled(Console.ENABLE_ECHO)
            term.setInterruptEnabled(Console.ENABLE_INTERRUPT)

            val consoleThread = Thread(term)
            consoleThread.setDaemon(true)
            consoleThread.setName("Console Input Reader")
            consoleThread.start()

            // has to be NOT DAEMON thread, since it must run before the app closes.
            // alternatively, shut everything down when the JVM closes.
            val shutdownThread: Thread = object : Thread() {
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
            Runtime.getRuntime().addShutdownHook(shutdownThread)
        }
    }

    val wrappedInputStream: InputStream = object : InputStream() {
        @Throws(IOException::class)
        override fun read(): Int {
            return terminal.read()
        }

        @Throws(IOException::class)
        override fun close() {
            terminal.close()
        }
    }
}
