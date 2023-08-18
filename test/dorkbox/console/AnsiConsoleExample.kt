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

package dorkbox.console

import dorkbox.console.Console
import dorkbox.console.output.Ansi
import dorkbox.console.output.Color
import dorkbox.console.output.Erase
import java.io.IOException
import java.lang.management.ManagementFactory

/**
 * System output for visual confirmation of ANSI codes.
 *
 *
 * Must enable assertions to verify no errors!! (ie: java -ea -jar blah.jar)
 */
object AnsiConsoleExample {
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        Console.ENABLE_ANSI = true
        Console.ENABLE_ECHO = true

        // needed to hook the output streams, so "normal" System.out/err work (rather than having to use Console.err/out
        Console.hookSystemOutputStreams()
        System.err.println("System Properties")
        val properties = System.getProperties()
        for ((key, value) in properties) {
            System.err.format("\t%s=%s%n", key, value)
        }

        System.err.println("")
        System.err.println("")
        System.err.println("")
        System.err.println("Runtime Arguments")
        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        val arguments = runtimeMxBean.inputArguments
        System.err.println(arguments.toTypedArray().contentToString())
        System.err.println("")
        System.err.println("")
        System.err.println("")

        System.out.println(
            Ansi.ansi().fg(Color.BLACK).a("black").bg(Color.BLACK).a("black")
                .reset()
                .fg(Color.BRIGHT_BLACK).a("b-black")
                .bg(Color.BRIGHT_BLACK).a("b-black")
                .reset()
        )
        System.out.println(
            Ansi.ansi().fg(Color.BLUE).a("blue").bg(Color.BLUE).a("blue").reset().fg(Color.BRIGHT_BLUE).a("b-blue").bg(Color.BRIGHT_BLUE)
                .a("b-blue").reset()
        )
        System.out.println(
            Ansi.ansi().fg(Color.CYAN).a("cyan").bg(Color.CYAN).a("cyan").reset().fg(Color.BRIGHT_CYAN).a("b-cyan").bg(Color.BRIGHT_CYAN)
                .a("b-cyan").reset()
        )
        System.out.println(
            Ansi.ansi().fg(Color.GREEN).a("green").bg(Color.GREEN).a("green").reset().fg(Color.BRIGHT_GREEN).a("b-green")
                .bg(Color.BRIGHT_GREEN).a("b-green").reset()
        )
        System.out.println(
            Ansi.ansi().fg(Color.MAGENTA).a("magenta").bg(Color.MAGENTA).a("magenta").reset().fg(Color.BRIGHT_MAGENTA).a("b-magenta")
                .bg(Color.BRIGHT_MAGENTA).a("b-magenta").reset()
        )
        System.out.println(
            Ansi.ansi().fg(Color.RED).a("red").bg(Color.RED).a("red").reset().fg(Color.BRIGHT_RED).a("b-red").bg(Color.BRIGHT_RED)
                .a("b-red").reset()
        )
        System.out.println(
            Ansi.ansi().fg(Color.YELLOW).a("yellow").bg(Color.YELLOW).a("yellow").reset().fg(Color.BRIGHT_YELLOW).a("b-yellow")
                .bg(Color.BRIGHT_YELLOW).a("b-yellow").reset()
        )
        System.out.println(
            Ansi.ansi().fg(Color.WHITE).a("white").bg(Color.WHITE).a("white").reset().fg(Color.BRIGHT_WHITE).a("b-white")
                .bg(Color.BRIGHT_WHITE).a("b-white").reset()
        )

        Console.reset() // reset the ansi stream. Can ALSO have ansi().reset(), but that would be redundant

        println("The following line should be blank except for the first '>'")
        System.out.println(
            Ansi.ansi().a(">THIS SHOULD BE BLANK").cursorToColumn(2).eraseLine()
        )

        println("The following line should be blank")
        System.out.println(
            Ansi.ansi().a(">THIS SHOULD BE BLANK").eraseLine(Erase.ALL)
        )

        System.out.println(
            Ansi.ansi().a(">THIS SHOULD BE BLANK").eraseLine(Erase.BACKWARD).a("Everything on this line before this should be blank")
        )

        System.out.println(
            Ansi.ansi().a("Everything on this line after this should be blank").saveCursorPosition().a(">THIS SHOULD BE BLANK")
                .restoreCursorPosition().eraseLine()
        )

        println("00000000000000000000000000")
        println("00000000000000000000000000")
        println("00000000000000000000000000")
        println("00000000000000000000000000")
        println("00000000000000000000000000")


        System.out.println(
            Ansi.ansi().a("Should have two blank spots in the above 0's").saveCursorPosition().cursorUp(4).cursorLeft(30).a("   ")
                .cursorDownLine().cursorRight(5).a("   ").restoreCursorPosition()
        )


        println("ver : " + Console.version)
        println()

        println("Now testing the input console. 'q' to quit")

        var read: Int
        while (Console.`in`().read().also { read = it } != 'q'.code) {
            if (Character.isDigit(read)) {
                val numericValue = Character.getNumericValue(read)
                // reverse if pressing 2
                if (numericValue == 2) {
                    System.out.print(Ansi.ansi().scrollDown(1))
                    System.out.flush() // flush guarantees the terminal moves the way we want
                }
                else {
                    System.out.print(Ansi.ansi().scrollUp(numericValue))
                    System.out.flush() // flush guarantees the terminal moves the way we want
                }
            }
            System.err.println("char :" + read + " (" + read.toChar() + ")")
        }

        println()
        println("Now testing the input console LINE input. 'q' to quit")

        var line: String
        while (Console.`in`().readLine().also { line = it!! } != "q") {
            System.err.println("line: $line")
        }
    }
}
