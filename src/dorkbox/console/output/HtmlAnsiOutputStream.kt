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

import java.io.IOException
import java.io.OutputStream

/**
 * @author dorkbox, llc
 * @author [Daniel Doubrovkine](http://code.dblock.org)
 */
class HtmlAnsiOutputStream(os: OutputStream?) : AnsiOutputStream(os) {
    companion object {
        private val regex = " ".toRegex()

        private val ANSI_COLOR_MAP: Array<String>

        init {
            @Suppress("UNCHECKED_CAST")
            ANSI_COLOR_MAP = arrayOfNulls<String>(8) as Array<String>
            ANSI_COLOR_MAP[BLACK] = "black"
            ANSI_COLOR_MAP[RED] = "red"
            ANSI_COLOR_MAP[GREEN] = "green"
            ANSI_COLOR_MAP[YELLOW] = "yellow"
            ANSI_COLOR_MAP[BLUE] = "blue"
            ANSI_COLOR_MAP[MAGENTA] = "magenta"
            ANSI_COLOR_MAP[CYAN] = "cyan"
            ANSI_COLOR_MAP[WHITE] = "white"
        }

        private val BYTES_QUOT = "&quot;".toByteArray()
        private val BYTES_AMP = "&amp;".toByteArray()
        private val BYTES_LT = "&lt;".toByteArray()
        private val BYTES_GT = "&gt;".toByteArray()
    }

    private var concealOn = false
    private val closingAttributes: MutableList<String> = ArrayList()

    @Throws(IOException::class)
    private fun write(s: String) {
        super.out.write(s.toByteArray())
    }

    @Throws(IOException::class)
    private fun writeAttribute(s: String) {
        write("<$s>")

        closingAttributes.add(0, s.split(regex, limit = 2).toTypedArray()[0])
    }

    @Throws(IOException::class)
    private fun closeAttributes() {
        for (attr in closingAttributes) {
            write("</$attr>")
        }
        closingAttributes.clear()
    }

    @Throws(IOException::class)
    override fun write(data: Int) {
        when (data) {
            34   -> out.write(BYTES_QUOT)
            38   -> out.write(BYTES_AMP)
            60   -> out.write(BYTES_LT)
            62   -> out.write(BYTES_GT)
            else -> super.write(data)
        }
    }

    @Throws(IOException::class)
    override fun processSetAttribute(attribute: Int) {
        when (attribute) {
            ATTRIBUTE_CONCEAL_ON    -> {
                write("\u001B[8m")
                concealOn = true
            }

            ATTRIBUTE_BOLD          -> writeAttribute("b")
            ATTRIBUTE_NORMAL        -> closeAttributes()
            ATTRIBUTE_UNDERLINE     -> writeAttribute("u")
            ATTRIBUTE_UNDERLINE_OFF -> closeAttributes()
            ATTRIBUTE_NEGATIVE_ON   -> {}
            ATTRIBUTE_NEGATIVE_OFF  -> {}
        }
    }

    @Throws(IOException::class)
    override fun processSetForegroundColor(color: Int) {
        writeAttribute("span style=\"color: ${ANSI_COLOR_MAP[color]};\"")
    }

    @Throws(IOException::class)
    override fun processSetBackgroundColor(color: Int) {
        writeAttribute("span style=\"background-color: ${ANSI_COLOR_MAP[color]};\"")
    }

    @Throws(IOException::class)
    override fun processAttributeReset() {
        if (concealOn) {
            write("\u001B[0m")
            concealOn = false
        }
        closeAttributes()
    }

    @Throws(IOException::class)
    override fun close() {
        closeAttributes()
        super.close()
    }

    @Throws(IOException::class)
    fun writeLine(buf: ByteArray, offset: Int, len: Int) {
        write(buf, offset, len)
        closeAttributes()
    }
}
