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

import dorkbox.console.output.AnsiRenderWriter
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Tests for the [AnsiRenderWriter] class.
 *
 * @author [Jason Dillon](mailto:jason@planet57.com)
 */
class AnsiRenderWriterTest {
    private var baos: ByteArrayOutputStream? = null
    private var out: AnsiRenderWriter? = null

    @Before
    fun setUp() {
        baos = ByteArrayOutputStream()
        out = AnsiRenderWriter(baos!!)
    }

    @After
    fun tearDown() {
        out = null
        baos = null
    }

    @Test
    fun testRenderNothing() {
        out!!.print("foo")
        out!!.flush()

        val result = String(baos!!.toByteArray())
        Assert.assertEquals("foo", result)
    }
}
