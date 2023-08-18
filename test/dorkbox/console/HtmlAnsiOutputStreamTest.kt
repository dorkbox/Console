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
package dorkbox.console

import dorkbox.console.output.HtmlAnsiOutputStream
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset

/**
 * @author [Daniel Doubrovkine](http://code.dblock.org)
 */
class HtmlAnsiOutputStreamTest {
    @Test
    @Throws(IOException::class)
    fun testNoMarkup() {
        Assert.assertEquals("line", colorize("line"))
    }

    @Test
    @Throws(IOException::class)
    fun testClear() {
        Assert.assertEquals("", colorize("[0m[K"))
        Assert.assertEquals("hello world", colorize("[0mhello world"))
    }

    @Test
    @Throws(IOException::class)
    fun testBold() {
        Assert.assertEquals("<b>hello world</b>", colorize("[1mhello world"))
    }

    @Test
    @Throws(IOException::class)
    fun testGreen() {
        Assert.assertEquals(
            "<span style=\"color: green;\">hello world</span>", colorize("[32mhello world")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testGreenOnWhite() {
        Assert.assertEquals(
            "<span style=\"background-color: white;\"><span style=\"color: green;\">hello world</span></span>",
            colorize("[47;32mhello world")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testEscapeHtml() {
        Assert.assertEquals("&quot;", colorize("\""))
        Assert.assertEquals("&amp;", colorize("&"))
        Assert.assertEquals("&lt;", colorize("<"))
        Assert.assertEquals("&gt;", colorize(">"))
        Assert.assertEquals("&quot;&amp;&lt;&gt;", colorize("\"&<>"))
    }

    @Test
    @Throws(IOException::class)
    fun testResetOnOpen() {
        Assert.assertEquals(
            "<span style=\"color: red;\">red</span>", colorize("[0;31;49mred[0m")
        )
    }

    @Test
    @Throws(IOException::class)
    fun testUTF8Character() {
        Assert.assertEquals(
            "<b>\u3053\u3093\u306b\u3061\u306f</b>", colorize("[1m\u3053\u3093\u306b\u3061\u306f")
        )
    }

    @Throws(IOException::class)
    private fun colorize(text: String): String {
        val os = ByteArrayOutputStream()
        val hos = HtmlAnsiOutputStream(os)
        hos.write(text.toByteArray(charset))
        hos.close()
        return String(os.toByteArray(), charset)
    }

    companion object {
        private val charset = Charset.forName("UTF-8")
    }
}
