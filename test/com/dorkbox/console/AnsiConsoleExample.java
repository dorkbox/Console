package com.dorkbox.console;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import dorkbox.console.Console;
import dorkbox.console.output.Ansi;
import dorkbox.console.output.Color;
import dorkbox.console.output.Erase;

/**
 * System output for visual confirmation of ANSI codes.
 * <p>
 * Must enable assertions to verify no errors!! (ie: java -ea -jar blah.jar)
 */
public
class AnsiConsoleExample {
    public static
    void main(String[] args) throws IOException {

        Console.ENABLE_ANSI = true;
        Console.ENABLE_ECHO = true;

        Console.init();

        System.err.println("System Properties");
        Properties properties = System.getProperties();
        for (Map.Entry<Object, Object> prop : properties.entrySet()) {
            System.err.format("\t%s=%s%n", prop.getKey(), prop.getValue());
        }
        System.err.println("");
        System.err.println("");
        System.err.println("");

        System.err.println("Runtime Arguments");
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        System.err.println(Arrays.toString(arguments.toArray()));

        System.err.println("");
        System.err.println("");
        System.err.println("");

        System.out.println(Ansi.ansi()
                               .fg(Color.BLACK).a("black").bg(Color.BLACK).a("black")
                               .reset()
                               .fg(Color.BRIGHT_BLACK).a("b-black").bg(Color.BRIGHT_BLACK).a("b-black")
                               .reset());

        System.out.println(Ansi.ansi()
                               .fg(Color.BLUE).a("blue").bg(Color.BLUE).a("blue")
                               .reset()
                               .fg(Color.BRIGHT_BLUE).a("b-blue").bg(Color.BRIGHT_BLUE).a("b-blue")
                               .reset());

        System.out.println(Ansi.ansi()
                               .fg(Color.CYAN).a("cyan").bg(Color.CYAN).a("cyan")
                               .reset()
                               .fg(Color.BRIGHT_CYAN).a("b-cyan").bg(Color.BRIGHT_CYAN).a("b-cyan")
                               .reset());

        System.out.println(Ansi.ansi()
                               .fg(Color.GREEN).a("green").bg(Color.GREEN).a("green")
                               .reset()
                               .fg(Color.BRIGHT_GREEN).a("b-green").bg(Color.BRIGHT_GREEN).a("b-green")
                               .reset());

        System.out.println(Ansi.ansi()
                                   .fg(Color.MAGENTA).a("magenta").bg(Color.MAGENTA).a("magenta")
                                   .reset()
                                   .fg(Color.BRIGHT_MAGENTA).a("b-magenta").bg(Color.BRIGHT_MAGENTA).a("b-magenta")
                                   .reset());

        System.out.println(Ansi.ansi()
                                   .fg(Color.RED).a("red").bg(Color.RED).a("red")
                                   .reset()
                                   .fg(Color.BRIGHT_RED).a("b-red").bg(Color.BRIGHT_RED).a("b-red")
                                   .reset());

        System.out.println(Ansi.ansi()
                                   .fg(Color.YELLOW).a("yellow").bg(Color.YELLOW).a("yellow")
                                   .reset()
                                   .fg(Color.BRIGHT_YELLOW).a("b-yellow").bg(Color.BRIGHT_YELLOW).a("b-yellow")
                                   .reset());

        System.out.println(Ansi.ansi()
                                   .fg(Color.WHITE).a("white").bg(Color.WHITE).a("white")
                                   .reset()
                                   .fg(Color.BRIGHT_WHITE).a("b-white").bg(Color.BRIGHT_WHITE).a("b-white")
                                   .reset());


        Console.reset(); // reset the ansi stream. Can ALSO have ansi().reset(), but that would be redundant


        System.out.println("The following line should be blank except for the first '>'");
        System.out.println(Ansi.ansi()
                                   .a(">THIS SHOULD BE BLANK")
                                   .cursorToColumn(2)
                                   .eraseLine());

        System.out.println("The following line should be blank");
        System.out.println(Ansi.ansi()
                                   .a(">THIS SHOULD BE BLANK")
                                   .eraseLine(Erase.ALL));

        System.out.println(Ansi.ansi()
                                   .a(">THIS SHOULD BE BLANK")
                                   .eraseLine(Erase.BACKWARD)
                                   .a("Everything on this line before this should be blank"));

        System.out.println(Ansi.ansi()
                                   .a("Everything on this line after this should be blank")
                                   .saveCursorPosition()
                                   .a(">THIS SHOULD BE BLANK")
                                   .restoreCursorPosition()
                                   .eraseLine());

        System.out.println("00000000000000000000000000");
        System.out.println("00000000000000000000000000");
        System.out.println("00000000000000000000000000");
        System.out.println("00000000000000000000000000");
        System.out.println("00000000000000000000000000");

        System.out.println(Ansi.ansi()
                                   .a("Should have two blank spots in the above 0's")
                                   .saveCursorPosition()
                                   .cursorUp(4)
                                   .cursorLeft(30)
                                   .a("   ")
                                   .cursorDownLine()
                                   .cursorRight(5)
                                   .a("   ")
                                   .restoreCursorPosition());

        System.out.println("ver : " + Console.getVersion());

        System.out.println();

        System.out.println("Now testing the input console. 'q' to quit");
        int read;
        while ((read = Console.in().read()) != 'q') {
            if (Character.isDigit(read)) {
                int numericValue = Character.getNumericValue(read);
                // reverse if pressing 2
                if (numericValue == 2) {
                    System.out.print(Ansi.ansi().scrollDown(1));
                    System.out.flush(); // flush guarantees the terminal moves the way we want
                }
                else {
                    System.out.print(Ansi.ansi().scrollUp(numericValue));
                    System.out.flush();  // flush guarantees the terminal moves the way we want
                }
            }
            System.err.println("char :" + read + " (" + (char) read + ")");
        }

        System.out.println();

        System.out.println("Now testing the input console LINE input. 'q' to quit");
        String line;
        while (!(line = Console.in().readLine()).equals("q")) {
            System.err.println("line: " + line);
        }
    }
}
