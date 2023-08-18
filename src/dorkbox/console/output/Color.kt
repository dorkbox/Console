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

enum class Color(
    private val value: Int,
    /** is this a BRIGHT color or NORMAL color?  */
    val isNormal: Boolean,
    private val namePriv: String
) {
    BLACK(AnsiOutputStream.BLACK, true, "BLACK"),
    RED(AnsiOutputStream.RED, true, "RED"),
    GREEN(AnsiOutputStream.GREEN, true, "GREEN"),
    YELLOW(AnsiOutputStream.YELLOW, true, "YELLOW"),
    BLUE(AnsiOutputStream.BLUE, true, "BLUE"),
    MAGENTA(AnsiOutputStream.MAGENTA, true, "MAGENTA"),
    CYAN(AnsiOutputStream.CYAN, true, "CYAN"),
    WHITE(AnsiOutputStream.WHITE, true, "WHITE"),

    // Brighter versions of those colors, ie: BRIGHT_BLACK is gray.
    BRIGHT_BLACK(AnsiOutputStream.BLACK, false, "BRIGHT_BLACK"),
    BRIGHT_RED(AnsiOutputStream.RED, false, "BRIGHT_RED"),
    BRIGHT_GREEN(AnsiOutputStream.GREEN, false, "BRIGHT_GREEN"),
    BRIGHT_YELLOW(AnsiOutputStream.YELLOW, false, "BRIGHT_YELLOW"),
    BRIGHT_BLUE(AnsiOutputStream.BLUE, false, "BRIGHT_BLUE"),
    BRIGHT_MAGENTA(AnsiOutputStream.MAGENTA, false, "BRIGHT_MAGENTA"),
    BRIGHT_CYAN(AnsiOutputStream.CYAN, false, "BRIGHT_CYAN"),
    BRIGHT_WHITE(AnsiOutputStream.WHITE, false, "BRIGHT_WHITE"),

    // SPECIAL use case here. This is intercepted (so the color doesn't matter)
    /**
     * DEFAULT is the color of console BEFORE any colors/settings are applied
     */
    DEFAULT(AnsiOutputStream.WHITE, true, "DEFAULT"),

    /**
     * DEFAULT is the color of console BEFORE any colors/settings are applied
     */
    BRIGHT_DEFAULT(AnsiOutputStream.WHITE, false, "BRIGHT_DEFAULT");

    init {
        // register code names with the ANSI renderer
        AnsiRenderer.reg(this, namePriv, false)
        AnsiRenderer.reg(this, "FG_$namePriv", false)
        AnsiRenderer.reg(this, "BG_$namePriv", true)
    }

    override fun toString(): String {
        return namePriv
    }

    fun fg(): Int {
        return value + 30
    }

    fun bg(): Int {
        return value + 40
    }

    fun fgBright(): Int {
        return value + 90
    }

    fun bgBright(): Int {
        return value + 100
    }
}
