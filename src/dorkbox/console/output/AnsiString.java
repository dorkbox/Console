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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * An ANSI string which reports the size of rendered text correctly (ignoring any ANSI escapes).
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public
class AnsiString implements CharSequence {
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private final CharSequence encoded;

    private final CharSequence plain;

    public
    AnsiString(final CharSequence str) {
        assert str != null;
        this.encoded = str;
        this.plain = chew(str);
    }

    private
    CharSequence chew(final CharSequence str) {
        assert str != null;

        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        AnsiOutputStream out = new AnsiOutputStream(buff);

        try {
            out.write(str.toString()
                         .getBytes(CHARSET));
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new String(buff.toByteArray());
    }

    public
    CharSequence getEncoded() {
        return encoded;
    }

    public
    CharSequence getPlain() {
        return plain;
    }

    // FIXME: charAt() and subSequence() will make things barf, need to call toString() first to get expected results

    @Override
    public
    int length() {
        return getPlain().length();
    }

    @Override
    public
    char charAt(final int index) {
        return getEncoded().charAt(index);
    }

    @Override
    public
    CharSequence subSequence(final int start, final int end) {
        return getEncoded().subSequence(start, end);
    }

    @Override
    public
    int hashCode() {
        return getEncoded().hashCode();
    }

    @Override
    public
    boolean equals(final Object obj) {
        return getEncoded().equals(obj);
    }

    @Override
    public
    String toString() {
        return getEncoded().toString();
    }
}
