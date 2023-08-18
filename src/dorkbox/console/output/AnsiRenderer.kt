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
package dorkbox.console.output

/**
 * Renders ANSI color escape-codes in strings by parsing out some special syntax to pick up the correct fluff to use.
 *
 *
 * The syntax for embedded ANSI codes is:
 *
 * ```
 * @|*code*(,*code*)* *text*|@
 *
 * Examples:
 *
 * @|bold Hello|@
 * @|bold,red Warning!|@
 * ```
 * For Colors, FG_x and BG_x are supported, as are BRIGHT_x (and consequently, FG_BRIGHT_x)
 *
 * @author dorkbox, llc
 * @author [Jason Dillon](mailto:jason@planet57.com)
 * @author [Hiram Chirino](http://hiramchirino.com)
 */
object AnsiRenderer {
    private val regex = "\\s".toRegex()

    const val BEGIN_TOKEN = "@|"
    const val CODE_LIST_SEPARATOR = ","
    const val CODE_TEXT_SEPARATOR = " "
    const val END_TOKEN = "|@"

    private val regex1 = CODE_TEXT_SEPARATOR.toRegex()
    private val regex2 = CODE_LIST_SEPARATOR.toRegex()

    private const val BEGIN_TOKEN_LEN = 2
    private const val END_TOKEN_LEN = 2

    private val codeMap: MutableMap<String, AnsiCodeMap> = HashMap(32)

    init {
        // have to make sure that all the different categories are added to our map.
        val red = Color.RED
        val bold = Attribute.BOLD
    }

    fun reg(anEnum: Enum<*>, codeName: String, isBackgroundColor: Boolean = false) {
        codeMap[codeName] = AnsiCodeMap(anEnum, isBackgroundColor)
    }

    /**
     * Renders [AnsiCodeMap] names on the given Ansi.
     *
     * @param ansi The Ansi to render upon
     * @param codeNames The code names to render
     */
    fun render(ansi: Ansi, vararg codeNames: String): Ansi {
        for (codeName in codeNames) {
            render(ansi, codeName)
        }
        return ansi
    }

    /**
     * Renders a [AnsiCodeMap] name on the given Ansi.
     *
     * @param ansi The Ansi to render upon
     * @param codeName The code name to render
     */
    fun render(ansi: Ansi, codeName: String): Ansi {
        var ansi = ansi

        val ansiCodeMap = codeMap[codeName.uppercase()] ?: error("Invalid ANSI code name: '$codeName'")
        if (ansiCodeMap.isColor) {
            ansi = if (ansiCodeMap.isBackgroundColor) {
                ansi.bg(ansiCodeMap.color)
            }
            else {
                ansi.fg(ansiCodeMap.color)
            }
        }
        else if (ansiCodeMap.isAttribute) {
            ansi = ansi.a(ansiCodeMap.attribute)
        }
        else {
            assert(false) { "Undetermined ANSI code name: '$codeName'" }
        }
        return ansi
    }

    /**
     * Renders text using the [AnsiCodeMap] names.
     *
     * @param text The text to render
     * @param codeNames The code names to render
     */
    fun render(text: String, vararg codeNames: String): String {
        val ansi: Ansi = render(Ansi.ansi(), *codeNames)
        return ansi.a(text).reset().toString()
    }

    @Throws(IllegalArgumentException::class)
    fun render(input: String): String {
        val buff = StringBuilder()
        var i = 0
        var j: Int
        var k: Int
        while (true) {
            j = input.indexOf(BEGIN_TOKEN, i)
            if (j == -1) {
                return if (i == 0) {
                    input
                }
                else {
                    buff.append(input.substring(i, input.length))
                    buff.toString()
                }
            }
            else {
                buff.append(input.substring(i, j))
                k = input.indexOf(END_TOKEN, j)
                if (k == -1) {
                    return input
                }
                else {
                    j += BEGIN_TOKEN_LEN
                    val spec = input.substring(j, k)

                    val items = spec.split(regex1, limit = 2).toTypedArray()
                    if (items.size == 1) {
                        return input
                    }

                    val replacement =
                        render(items[1], *items[0].split(regex2).dropLastWhile { it.isEmpty() }.toTypedArray())
                    buff.append(replacement)
                    i = k + END_TOKEN_LEN
                }
            }
        }
    }

    /**
     * Renders [AnsiCodeMap] names as an ANSI escape string.
     *
     * @param codeNames The code names to render
     *
     * @return an ANSI escape string.
     */
    fun renderCodeNames(codeNames: String): String {

        return render(Ansi(), *codeNames.split(regex).dropLastWhile { it.isEmpty() }.toTypedArray()).toString()
    }
}
