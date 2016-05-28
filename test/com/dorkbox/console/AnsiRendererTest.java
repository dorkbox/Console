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

package com.dorkbox.console;

import static dorkbox.console.output.AnsiRenderer.render;
import static java.awt.Font.BOLD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import dorkbox.console.output.Ansi;
import dorkbox.console.output.AnsiRenderer;
import dorkbox.console.output.Color;

/**
 * Tests for the {@link AnsiRenderer} class.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class AnsiRendererTest
{
    public static
    boolean test(final String text) {
        return text != null && text.contains(AnsiRenderer.BEGIN_TOKEN);
    }

    @Before
    public void setUp() {
        Ansi.setEnabled(true);
    }

    @Test
    public void testTest() throws Exception {
        assertFalse(test("foo"));
        assertTrue(test("@|foo|"));
        assertTrue(test("@|foo"));
    }

    @Test
    public void testRender() {
        String str = render("@|bold foo|@");
        System.out.println(str);
        assertEquals(Ansi.ansi().a(BOLD).a("foo").reset().toString(), str);
        assertEquals(Ansi.ansi().bold().a("foo").reset().toString(), str);
    }

    @Test
    public void testRender2() {
        String str = render("@|bold,red foo|@");
        System.out.println(str);
        assertEquals(Ansi.ansi().a(BOLD).fg(Color.RED).a("foo").reset().toString(), str);
        assertEquals(Ansi.ansi().bold().fgRed().a("foo").reset().toString(), str);
    }

    @Test
    public void testRender3() {
        String str = render("@|bold,red foo bar baz|@");
        System.out.println(str);
        assertEquals(Ansi.ansi().a(BOLD).fg(Color.RED).a("foo bar baz").reset().toString(), str);
        assertEquals(Ansi.ansi().bold().fgRed().a("foo bar baz").reset().toString(), str);
    }

    @Test
    public void testRender4() {
        String str = render("@|bold,red foo bar baz|@ ick @|bold,red foo bar baz|@");
        System.out.println(str);
        assertEquals(Ansi.ansi()
                         .a(BOLD).fg(Color.RED).a("foo bar baz").reset()
                         .a(" ick ")
                         .a(BOLD).fg(Color.RED).a("foo bar baz").reset()
                         .toString(), str);
    }
    
    @Test
    public void testRender5() {
        // Check the ansi() render method.
        String str = Ansi.ansi().render("@|bold Hello|@").toString();
        System.out.println(str);
        assertEquals(Ansi.ansi().a(BOLD).a("Hello").reset().toString(), str);
    }
    

    @Test
    public void testRenderNothing() {
        assertEquals("foo", render("foo"));
    }

    @Test
    public void testRenderInvalidMissingEnd() {
        String str = render("@|bold foo");
        assertEquals("@|bold foo", str);
    }

    @Test
    public void testRenderInvalidMissingText() {
        String str = render("@|bold|@");
        assertEquals("@|bold|@", str);
    }
}
