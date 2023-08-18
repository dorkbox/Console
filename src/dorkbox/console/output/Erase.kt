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

enum class Erase(private val value: Int,
                 private val namePriv: String) {

    FORWARD(AnsiOutputStream.ERASE_TO_END, "FORWARD"),
    BACKWARD(AnsiOutputStream.ERASE_TO_BEGINNING, "BACKWARD"),
    ALL(AnsiOutputStream.ERASE_ALL, "ALL");

    override fun toString(): String {
        return namePriv
    }

    fun value(): Int {
        return value
    }
}
