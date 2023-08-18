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

import dorkbox.console.output.Ansi
import dorkbox.console.output.Ansi.Companion.ansi
import dorkbox.console.output.AnsiRenderer.render
import dorkbox.console.output.Attribute
import dorkbox.console.output.Color
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the [Ansi] class.
 *
 * @author [Jason Dillon](mailto:jason@planet57.com)
 */
class AnsiTest {
    @Test
    @Throws(CloneNotSupportedException::class)
    fun testClone() {
        val ansi: Ansi = ansi().a("Some text").bg(Color.BLACK).fg(Color.WHITE)
        val clone: Ansi = ansi(ansi)

        assertEquals(ansi.a("test").reset().toString(), clone.a("test").reset().toString())
    }

    @Test
    @Throws(CloneNotSupportedException::class)
    fun testOutput() {

        // verify the output renderer
        var str: String = render("@|bold foo|@foo")
        assertEquals(ansi().a(Attribute.BOLD).a("foo").reset().a("foo").toString(), str)
        assertEquals(ansi().bold().a("foo").reset().a("foo").toString(), str)

        str = render("@|bold,red foo|@")
        assertEquals(ansi().a(Attribute.BOLD).fg(Color.RED).a("foo").reset().toString(), str)
        assertEquals(ansi().bold().fg(Color.RED).a("foo").reset().toString(), str)

        str = render("@|bold,red foo bar baz|@")
        assertEquals(ansi().a(Attribute.BOLD).fg(Color.RED).a("foo bar baz").reset().toString(), str)
        assertEquals(ansi().bold().fg(Color.RED).a("foo bar baz").reset().toString(), str)

        str = render("@|bold,red foo bar baz|@ ick @|bold,red foo bar baz|@")
        val expected: String =
            ansi().a(Attribute.BOLD).fg(Color.RED).a("foo bar baz").reset().a(" ick ").a(Attribute.BOLD).fg(Color.RED).a("foo bar baz")
                .reset().toString()

        assertEquals(expected, str)

        str = render("@|bold foo") // shouldn't work
        System.err.println("$str <- shouldn't work")

        str = render("@|bold|@") // shouldn't work
        System.err.println("$str <- shouldn't work")

        str = render("@|bold foo|@foo")
        println("$str <- shouldn't work")
    }
}
