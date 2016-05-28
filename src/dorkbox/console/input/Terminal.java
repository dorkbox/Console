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

public abstract
class Terminal {

    public static final String CONSOLE_ERROR_INIT = "Unable to get input console mode.";
    protected static final int DEFAULT_WIDTH = 80;
    protected static final int DEFAULT_HEIGHT = 24;
    protected final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    protected
    Terminal() {
    }

    public abstract
    void restore() throws IOException;

    public abstract
    int getWidth();

    public abstract
    int getHeight();

    public abstract
    void setEchoEnabled(final boolean enabled);

    public abstract
    void setInterruptEnabled(final boolean enabled);

    /**
     * @return a character from whatever underlying input method the terminal has available.
     */
    public abstract
    int read();
}
