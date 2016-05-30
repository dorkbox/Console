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
package dorkbox.console.input;

import java.io.IOException;

import dorkbox.console.Console;

@SuppressWarnings("unused")
public abstract
class Terminal {

    static final String CONSOLE_ERROR_INIT = "Unable to initialize the input console.";

    static final int DEFAULT_WIDTH = 80;
    static final int DEFAULT_HEIGHT = 24;
    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    Terminal() {
    }

    abstract
    void doSetInterruptEnabled(final boolean enabled);

    protected abstract
    void doSetEchoEnabled(final boolean enabled);

    public abstract
    void restore() throws IOException;

    public abstract
    int getWidth();

    public abstract
    int getHeight();

    /**
     * Enables or disables CTRL-C behavior in the console
     */
    public final
    void setInterruptEnabled(final boolean enabled) {
        Console.ENABLE_INTERRUPT = enabled;
        doSetInterruptEnabled(enabled);
    }

    /**
     * Enables or disables character echo to stdout
     */
    public final
    void setEchoEnabled(final boolean enabled) {
        Console.ENABLE_ECHO = enabled;
        doSetEchoEnabled(enabled);
    }

    /**
     * Reads single character input from the console.
     *
     * @return -1 if no data or problems
     */
    public abstract
    int read();

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    public abstract
    char[] readLineChars();

    /**
     * Reads a single line of characters, defined as everything before the 'ENTER' key is pressed
     * @return null if no data
     */
    public
    String readLine() {
        char[] line = readLineChars();
        if (line == null) {
            return null;
        }
        return new String(line);
    }

    /**
     * Reads a line of characters from the console as a character array, defined as everything before the 'ENTER' key is pressed
     *
     * @return empty char[] if no data
     */
    public
    char[] readLinePassword() {
        // don't bother in an IDE. it won't work.
        boolean echoEnabled = Console.ENABLE_ECHO;
        Console.ENABLE_ECHO = false;
        char[] readLine0 = readLineChars();
        Console.ENABLE_ECHO = echoEnabled;

        return readLine0;
    }

    /**
     * releases any thread still waiting.
     */
    public abstract
    void close();
}
