package com.dorkbox.console;

import static dorkbox.console.output.Ansi.ansi;

import java.io.IOException;

import dorkbox.console.Console;
import dorkbox.console.output.Ansi;
import dorkbox.console.output.Erase;

/**
 * System output for visual confirmation of ANSI codes.
 * <p>
 * Must enable assertions to verify no errors!! (ie: java -ea -jar blah.jar)
 */
public
class AnsiRendererTestExample {
    public static
    void main(String[] args) throws IOException {
        Ansi.systemInstall();
        Ansi.setEnabled(true);

        System.out.println(ansi()
                .fgBlack().a("black").bgBlack().a("black")
                .reset()
                .fgBrightBlack().a("b-black").bgBrightBlack().a("b-black")
                .reset());

        System.out.println(ansi()
                .fgBlue().a("blue").bgBlue().a("blue")
                .reset()
                .fgBrightBlue().a("b-blue").bgBrightBlue().a("b-blue")
                .reset());

        System.out.println(ansi()
                .fgCyan().a("cyan").bgCyan().a("cyan")
                .reset()
                .fgBrightCyan().a("b-cyan").bgBrightCyan().a("b-cyan")
                .reset());

        System.out.println(ansi()
                .fgGreen().a("green").bgGreen().a("green")
                .reset()
                .fgBrightGreen().a("b-green").bgBrightGreen().a("b-green")
                .reset());

        System.out.println(ansi()
                .fgMagenta().a("magenta").bgMagenta().a("magenta")
                .reset()
                .fgBrightMagenta().a("b-magenta").bgBrightMagenta().a("b-magenta")
                .reset());

        System.out.println(ansi()
                .fgRed().a("red").bgRed().a("red")
                .reset()
                .fgBrightRed().a("b-red").bgBrightRed().a("b-red")
                .reset());

        System.out.println(ansi()
                .fgYellow().a("yellow").bgYellow().a("yellow")
                .reset()
                .fgBrightYellow().a("b-yellow").bgBrightYellow().a("b-yellow")
                .reset());

        System.out.println(ansi()
                .fgWhite().a("white").bgWhite().a("white")
                .reset()
                .fgBrightWhite().a("b-white").bgBrightWhite().a("b-white")
                .reset());


        System.out.println(ansi()); // reset the ansi stream. Can ALSO have ansi().reset(), but that would be redundant

        System.out.println("The following line should be blank except for the first '>'");
        System.out.println(ansi()
                .a(">THIS SHOULD BE BLANK")
                .cursorToColumn(2)
                .eraseLine());

        System.out.println("The following line should be blank");
        System.out.println(ansi()
                .a(">THIS SHOULD BE BLANK")
                .eraseLine(Erase.ALL));

        System.out.println(ansi()
                .a(">THIS SHOULD BE BLANK")
                .eraseLine(Erase.BACKWARD)
                .a("Everything before this should be blank"));

        System.out.println(ansi()
                .a("Everything after this should be blank")
                .saveCursorPosition()
                .a(">THIS SHOULD BE BLANK")
                .restoreCursorPosition()
                .eraseLine(Erase.FORWARD));

        System.out.println("00000000000000000000000000");
        System.out.println("00000000000000000000000000");
        System.out.println("00000000000000000000000000");
        System.out.println("00000000000000000000000000");
        System.out.println("00000000000000000000000000");

        System.out.println(ansi()
                .a("Should have two blank spots in the above 0's")
                .saveCursorPosition()
                .cursorUp(4)
                .cursorLeft(30)
                .a("   ")
                .cursorDownLine()
                .cursorRight(5)
                .a("   ")
                .restoreCursorPosition());


        // verify the output renderer
//        String str = render("@|bold foo|@foo");
//        System.out.println(str);
//        assertEquals(ansi().a(BOLD).a("foo").reset().a("foo").toString(), str);
//        assertEquals(ansi().bold().a("foo").reset().a("foo").toString(), str);
//
//
//        str = render("@|bold,red foo|@");
//        System.out.println(str);
//        assertEquals(ansi().a(BOLD).fg(RED).a("foo").reset().toString(), str);
//        assertEquals(ansi().bold().fgRed().a("foo").reset().toString(), str);
//
//        str = render("@|bold,red foo bar baz|@");
//        System.out.println(str);
//        assertEquals(ansi().a(BOLD).fg(RED).a("foo bar baz").reset().toString(), str);
//        assertEquals(ansi().bold().fgRed().a("foo bar baz").reset().toString(), str);
//
//
//        str = render("@|bold,red foo bar baz|@ ick @|bold,red foo bar baz|@");
//        System.out.println(str);
//        String expected = ansi().a(BOLD)
//                                .fg(RED)
//                                .a("foo bar baz")
//                                .reset()
//                                .a(" ick ")
//                                .a(BOLD)
//                                .fg(RED)
//                                .a("foo bar baz")
//                                .reset()
//                                .toString();
//
//        assertEquals(expected, str);
//
//
//        str = render("@|bold foo"); // shouldn't work
//        System.err.println(str + " <- shouldn't work");
//
//        str = render("@|bold|@");  // shouldn't work
//        System.err.println(str + " <- shouldn't work");
//
//        Ansi.systemUninstall();
//
//        str = render("@|bold foo|@foo");
//        System.out.println(str  + " <- shouldn't work");


        System.err.println("ver : " + Console.getVersion());


        System.out.println("Now testing the input console. '?' to quit");
        Console.ENABLE_ECHO = true;

        int read;
        while ((read = Console.in.read()) != '?') {
//            System.err.println("READ :" + read + " (" + (char) read + ")");
        }
    }
}
