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

enum class Attribute(val value: Int,
                     private val namePriv: String) {

    RESET(AnsiOutputStream.ATTRIBUTE_RESET, "RESET"),

    BOLD(AnsiOutputStream.ATTRIBUTE_BOLD, "BOLD"),
    BOLD_OFF(AnsiOutputStream.ATTRIBUTE_NORMAL, "BOLD_OFF"),

    FAINT(AnsiOutputStream.ATTRIBUTE_FAINT, "FAINT"),
    FAINT_OFF(AnsiOutputStream.ATTRIBUTE_NORMAL, "FAINT_OFF"),

    ITALIC(AnsiOutputStream.ATTRIBUTE_ITALIC, "ITALIC"),
    ITALIC_OFF(AnsiOutputStream.ATTRIBUTE_ITALIC_OFF, "ITALIC_OFF"),

    UNDERLINE(AnsiOutputStream.ATTRIBUTE_UNDERLINE, "UNDERLINE"),
    UNDERLINE_DOUBLE(AnsiOutputStream.ATTRIBUTE_UNDERLINE_DOUBLE, "UNDERLINE_DOUBLE"),
    UNDERLINE_OFF(AnsiOutputStream.ATTRIBUTE_UNDERLINE_OFF, "UNDERLINE_OFF"),

    BLINK_SLOW(AnsiOutputStream.ATTRIBUTE_BLINK_SLOW, "BLINK_SLOW"),
    BLINK_FAST(AnsiOutputStream.ATTRIBUTE_BLINK_FAST, "BLINK_FAST"),
    BLINK_OFF(AnsiOutputStream.ATTRIBUTE_BLINK_OFF, "BLINK_OFF"),

    NEGATIVE(AnsiOutputStream.ATTRIBUTE_NEGATIVE_ON, "NEGATIVE"),
    NEGATIVE_OFF(AnsiOutputStream.ATTRIBUTE_NEGATIVE_OFF, "NEGATIVE_OFF"),

    CONCEAL(AnsiOutputStream.ATTRIBUTE_CONCEAL_ON, "CONCEAL"),
    CONCEAL_OFF(AnsiOutputStream.ATTRIBUTE_CONCEAL_OFF, "CONCEAL_OFF"),

    STRIKETHROUGH(AnsiOutputStream.ATTRIBUTE_STRIKETHROUGH_ON, "STRIKETHROUGH"),
    STRIKETHROUGH_OFF(AnsiOutputStream.ATTRIBUTE_STRIKETHROUGH_OFF, "STRIKETHROUGH_OFF");

    init {
        // register code names with the ANSI renderer
        AnsiRenderer.reg(this, namePriv)
    }

    override fun toString(): String {
        return namePriv
    }
}
