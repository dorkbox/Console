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

package dorkbox.console.output;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Renders ANSI color escape-codes in strings by parsing out some special syntax to pick up the correct fluff to use.
 * <p>
 * <p/>
 * The syntax for embedded ANSI codes is:
 * <p>
 * <pre>
 *   <tt>@|</tt><em>code</em>(<tt>,</tt><em>code</em>)* <em>text</em><tt>|@</tt>
 * </pre>
 * <p>
 * Examples:
 * <p>
 * <pre>
 *   <tt>@|bold Hello|@</tt>
 * </pre>
 * <p>
 * <pre>
 *   <tt>@|bold,red Warning!|@</tt>
 * </pre>
 *
 * @author dorkbox, llc
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
@SuppressWarnings("WeakerAccess")
public
class AnsiRenderer {
    public static final String BEGIN_TOKEN = "@|";
    public static final String CODE_LIST_SEPARATOR = ",";
    public static final String CODE_TEXT_SEPARATOR = " ";
    public static final String END_TOKEN = "|@";

    private static final int BEGIN_TOKEN_LEN = 2;
    private static final int END_TOKEN_LEN = 2;

    private static Map<String, AnsiCode> codeMap = new HashMap<String, AnsiCode>(32);

    static
    void reg(Enum anEnum, String codeName) {
        reg(anEnum, codeName, false);
    }

    static
    void reg(Enum anEnum, String codeName, boolean isBackgroundColor) {
        codeMap.put(codeName, new AnsiCode(anEnum, codeName, isBackgroundColor));
    }

    /**
     * Renders {@link AnsiCode} names on the given Ansi.
     *
     * @param ansi The Ansi to render upon
     * @param codeNames The code names to render
     */
    public static
    Ansi render(Ansi ansi, final String... codeNames) {
        for (String codeName : codeNames) {
            render(ansi, codeName);
        }
        return ansi;
    }

    /**
     * Renders a {@link AnsiCode} name on the given Ansi.
     *
     * @param ansi The Ansi to render upon
     * @param codeName The code name to render
     */
    public static
    Ansi render(Ansi ansi, String codeName) {
        AnsiCode ansiCode = codeMap.get(codeName.toUpperCase(Locale.ENGLISH));
        assert ansiCode != null : "Invalid ANSI code name: '" + codeName + "'";

        if (ansiCode.isColor()) {
            if (ansiCode.isBackgroundColor()) {
                ansi = ansi.bg(ansiCode.getColor());
            }
            else {
                ansi = ansi.fg(ansiCode.getColor());
            }
        }
        else if (ansiCode.isAttribute()) {
            ansi = ansi.a(ansiCode.getAttribute());
        } else {
            assert false : "Undetermined ANSI code name: '" + codeName + "'";
        }

        return ansi;
    }

    /**
     * Renders text using the {@link AnsiCode} names.
     *
     * @param text The text to render
     * @param codeNames The code names to render
     */
    public static
    String render(final String text, final String... codeNames) {
        Ansi ansi = render(Ansi.ansi(), codeNames);
        return ansi.a(text)
                   .reset()
                   .toString();
    }

    public static
    String render(final String input) throws IllegalArgumentException {
        StringBuilder buff = new StringBuilder();

        int i = 0;
        int j, k;

        while (true) {
            j = input.indexOf(BEGIN_TOKEN, i);
            if (j == -1) {
                if (i == 0) {
                    return input;
                }
                else {
                    buff.append(input.substring(i, input.length()));
                    return buff.toString();
                }
            }
            else {
                buff.append(input.substring(i, j));
                k = input.indexOf(END_TOKEN, j);

                if (k == -1) {
                    return input;
                }
                else {
                    j += BEGIN_TOKEN_LEN;
                    String spec = input.substring(j, k);

                    String[] items = spec.split(CODE_TEXT_SEPARATOR, 2);
                    if (items.length == 1) {
                        return input;
                    }
                    String replacement = render(items[1], items[0].split(CODE_LIST_SEPARATOR));

                    buff.append(replacement);

                    i = k + END_TOKEN_LEN;
                }
            }
        }
    }

    /**
     * Renders {@link AnsiCode} names as an ANSI escape string.
     *
     * @param codeNames The code names to render
     *
     * @return an ANSI escape string.
     */
    public static
    String renderCodeNames(final String codeNames) {
        return render(new Ansi(), codeNames.split("\\s")).toString();
    }
}
