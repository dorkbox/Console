package dorkbox.console.util.windows;

import com.sun.jna.Union;

public class CharUnion extends Union {
    public char unicodeChar;
    public byte asciiChar;

    public CharUnion() {
    }

    public CharUnion(char c) {
        setType(char.class);
        unicodeChar = c;
    }

    public CharUnion(byte c) {
        setType(byte.class);
        asciiChar = c;
    }

    public void set(char c) {
        setType(char.class);
        unicodeChar = c;
    }

    public void set(byte c) {
        setType(byte.class);
        asciiChar = c;
    }
}
