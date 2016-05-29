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

package com.dorkbox.console;

import static dorkbox.console.output.Ansi.ansi;
import static dorkbox.console.output.AnsiRenderer.render;
import static dorkbox.console.output.Attribute.BOLD;
import static dorkbox.console.output.Color.RED;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import dorkbox.console.output.Ansi;
import dorkbox.console.output.Color;

/**
 * Tests for the {@link Ansi} class.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class AnsiTest
{
    @Test
    public void testClone() throws CloneNotSupportedException {
        Ansi ansi = ansi().a("Some text").bg(Color.BLACK).fg(Color.WHITE);
        Ansi clone = ansi(ansi);

        assertEquals(ansi.a("test").reset().toString(), clone.a("test").reset().toString());
    }

    @Test
    public void testOutput() throws CloneNotSupportedException {

        // verify the output renderer
        String str = render("@|bold foo|@foo");
        assertEquals(ansi().a(BOLD).a("foo").reset().a("foo").toString(), str);
        assertEquals(ansi().bold().a("foo").reset().a("foo").toString(), str);


        str = render("@|bold,red foo|@");
        assertEquals(ansi().a(BOLD).fg(RED).a("foo").reset().toString(), str);
        assertEquals(ansi().bold().fg(RED).a("foo").reset().toString(), str);

        str = render("@|bold,red foo bar baz|@");
        assertEquals(ansi().a(BOLD).fg(RED).a("foo bar baz").reset().toString(), str);
        assertEquals(ansi().bold().fg(RED).a("foo bar baz").reset().toString(), str);


        str = render("@|bold,red foo bar baz|@ ick @|bold,red foo bar baz|@");
        String expected = ansi().a(BOLD)
                                .fg(RED)
                                .a("foo bar baz")
                                .reset()
                                .a(" ick ")
                                .a(BOLD)
                                .fg(RED)
                                .a("foo bar baz")
                                .reset()
                                .toString();

        assertEquals(expected, str);


        str = render("@|bold foo"); // shouldn't work
        System.err.println(str + " <- shouldn't work");

        str = render("@|bold|@");  // shouldn't work
        System.err.println(str + " <- shouldn't work");

        str = render("@|bold foo|@foo");
        System.out.println(str  + " <- shouldn't work");
    }
}
