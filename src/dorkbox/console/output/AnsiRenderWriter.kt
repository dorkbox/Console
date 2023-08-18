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

/*
 * Copyright (C) 2009 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.console.output

import java.io.OutputStream
import java.io.PrintWriter
import java.io.Writer
import java.util.*

/**
 * Print writer which supports automatic ANSI color rendering via [AnsiRenderer].
 *
 * @author [Jason Dillon](mailto:jason@planet57.com)
 * @author [Hiram Chirino](http://hiramchirino.com)
 */
class AnsiRenderWriter : PrintWriter {
    constructor(out: OutputStream) : super(out)
    constructor(out: OutputStream, autoFlush: Boolean) : super(out, autoFlush)
    constructor(out: Writer) : super(out)
    constructor(out: Writer, autoFlush: Boolean) : super(out, autoFlush)

    override fun write(s: String) {
        if (s.contains(AnsiRenderer.BEGIN_TOKEN)) {
            super.write(AnsiRenderer.render(s))
        }
        else {
            super.write(s)
        }
    }

    override fun format(format: String, vararg args: Any): PrintWriter {
        flush() // prevents partial output from being written while formatting or we will get rendering exceptions
        print(String.format(format, *args))
        return this
    }

    override fun format(l: Locale, format: String, vararg args: Any): PrintWriter {
        flush() // prevents partial output from being written while formatting or we will get rendering exceptions
        print(String.format(l, format, *args))
        return this
    }
}
