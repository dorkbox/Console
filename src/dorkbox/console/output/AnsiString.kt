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

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset

/**
 * An ANSI string which reports the size of rendered text correctly (ignoring any ANSI escapes).
 *
 * @author [Jason Dillon](mailto:jason@planet57.com)
 */
class AnsiString(str: CharSequence) : CharSequence {
    companion object {
        private val CHARSET = Charset.forName("UTF-8")
    }

    val encoded: CharSequence
    val plain: CharSequence

    @Volatile
    private var toStringCalled = false

    init {
        encoded = str
        plain = chew(str)
    }

    private fun chew(str: CharSequence): CharSequence {
        val buff = ByteArrayOutputStream()
        val out = AnsiOutputStream(buff)

        try {
            out.write(
                str.toString().toByteArray(CHARSET)
            )
            out.flush()
            out.close()
        }
        catch (e: IOException) {
            throw RuntimeException(e)
        }

        return String(buff.toByteArray())
    }

    override val length: Int
        get() {
            return plain.length
        }

    override fun get(index: Int): Char {
        // toString() must be called first to get expected results
        if (!toStringCalled) {
            toStringCalled = true
            encoded.toString()
        }
        return encoded[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        // toString() must be called first to get expected results
        if (!toStringCalled) {
            toStringCalled = true
            encoded.toString()
        }
        return encoded.subSequence(startIndex, endIndex)
    }

    override fun hashCode(): Int {
        return encoded.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return encoded == other
    }

    override fun toString(): String {
        return encoded.toString()
    }
}
