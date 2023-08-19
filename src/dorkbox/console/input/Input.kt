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
import dorkbox.console.input.Terminal.Companion.terminal
import dorkbox.console.util.TerminalDetection
import dorkbox.os.OS
import dorkbox.os.OS.isWindows
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream

object Input {
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
