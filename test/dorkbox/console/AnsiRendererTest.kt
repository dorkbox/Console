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
 * Copyright (C) 2009 the original author or authors.
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

import dorkbox.console.Console
import dorkbox.console.output.Ansi
import dorkbox.console.output.AnsiRenderer
import dorkbox.console.output.Attribute
import dorkbox.console.output.Color
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests for the [AnsiRenderer] class.
 *
 * @author [Jason Dillon](mailto:jason@planet57.com)
 */
class AnsiRendererTest {
    @Before
    fun setUp() {
        Console.ENABLE_ANSI = true
    }

    @Test
    @Throws(Exception::class)
    fun testTest() {
        Assert.assertFalse(test("foo"))
        Assert.assertTrue(test("@|foo|"))
        Assert.assertTrue(test("@|foo"))
    }

    @Test
    fun testRender() {
        val str: String = AnsiRenderer.render("@|bold foo|@")
        println(str)
        assertEquals(Ansi.ansi().a(Attribute.BOLD).a("foo").reset().toString(), str)
        assertEquals(Ansi.ansi().bold().a("foo").reset().toString(), str)
    }

    @Test
    fun testRender2() {
        val str: String = AnsiRenderer.render("@|bold,red foo|@")
        println(str)
        assertEquals(Ansi.ansi().a(Attribute.BOLD).fg(Color.RED).a("foo").reset().toString(), str)
    }

    @Test
    fun testRender3() {
        val str: String = AnsiRenderer.render("@|bold,red foo bar baz|@")
        println(str)
        assertEquals(Ansi.ansi().a(Attribute.BOLD).fg(Color.RED).a("foo bar baz").reset().toString(), str)
    }

    @Test
    fun testRender4() {
        val str: String = AnsiRenderer.render("@|bold,red foo bar baz|@ ick @|bold,red foo bar baz|@")
        println(str)
        assertEquals(
            Ansi.ansi().a(Attribute.BOLD).fg(Color.RED).a("foo bar baz").reset().a(" ick ").a(Attribute.BOLD).fg(Color.RED).a("foo bar baz")
                .reset().toString(), str
        )
    }

    @Test
    fun testRender5() {
        // Check the ansi() render method.
        val str = Ansi.ansi().render("@|bold Hello|@").toString()
        println(str)
        assertEquals(Ansi.ansi().a(Attribute.BOLD).a("Hello").reset().toString(), str)
    }

    @Test
    fun testRenderNothing() {
        assertEquals("foo", AnsiRenderer.render("foo"))
    }

    @Test
    fun testRenderInvalidMissingEnd() {
        val str: String = AnsiRenderer.render("@|bold foo")
        Assert.assertEquals("@|bold foo", str)
    }

    @Test
    fun testRenderInvalidMissingText() {
        val str: String = AnsiRenderer.render("@|bold|@")
        Assert.assertEquals("@|bold|@", str)
    }

    companion object {
        fun test(text: String): Boolean {
            return text.contains(AnsiRenderer.BEGIN_TOKEN)
        }
    }
}
