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

import dorkbox.console.output.Ansi
import dorkbox.console.output.AnsiString
import dorkbox.console.output.Attribute
import org.junit.Assert
import org.junit.Test

/**
 * Tests for [AnsiString].
 *
 * @author [Jason Dillon](mailto:jason@planet57.com)
 */
class AnsiStringTest {
    @Test
    @Throws(Exception::class)
    fun testNotEncoded() {
        val `as` = AnsiString("foo")
        Assert.assertEquals("foo", `as`.encoded)
        Assert.assertEquals("foo", `as`.plain)
        Assert.assertEquals(3, `as`.length.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testEncoded() {
        val `as` = AnsiString(Ansi.ansi().a(Attribute.BOLD).a("foo").reset().toString())

        Assert.assertEquals("foo", `as`.plain)
        Assert.assertEquals(3, `as`.length.toLong())
    }
}
