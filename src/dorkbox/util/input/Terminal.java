package dorkbox.util.input;

import java.io.IOException;

public abstract class Terminal {
    protected final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());


    public static final int DEFAULT_WIDTH = 80;
    public static final int DEFAULT_HEIGHT = 24;

    private volatile boolean echoEnabled;

    public Terminal() {
    }

    public abstract void init() throws IOException;

    public abstract void restore() throws IOException;

    public void setEchoEnabled(boolean enabled) {
        this.echoEnabled = enabled;
    }

    public boolean isEchoEnabled() {
        return this.echoEnabled;
    }

    public abstract int getWidth();
    public abstract int getHeight();

    /**
     * @return a character from whatever underlying input method the terminal has available.
     */
    public abstract int read();
}
