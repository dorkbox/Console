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
 * Used for determining what ANSI attribute to use based on a formal name
 */
internal class AnsiCodeMap(private val anEnum: Enum<*>, val isBackgroundColor: Boolean) {

    val isColor: Boolean
        get() = anEnum is Color

    val color: Color
        get() = anEnum as Color

    val isAttribute: Boolean
        get() = anEnum is Attribute

    val attribute: Attribute
        get() = anEnum as Attribute
}
