package dorkbox.util.input.unsupported;

import java.io.IOException;
import java.io.InputStream;

import dorkbox.util.bytes.ByteBuffer2;
import dorkbox.util.input.Terminal;

public class UnsupportedTerminal extends Terminal {

    private ByteBuffer2 buffer = new ByteBuffer2(8, -1);

    private int readerCount = -1;
    private InputStream in;

    public UnsupportedTerminal() {
        this.in = System.in;
    }

    @Override
    public final void init() throws IOException {
    }

    @Override
    public final void restore() {
    }

    @Override
    public final int getWidth() {
        return 0;
    }

    @Override
    public final int getHeight() {
        return 0;
    }

    @Override
    public final int read() {
        // if we are reading data (because we are in IDE mode), we want to return ALL the chars of the line!

        // so, 'readerCount' is REALLY the index at which we return letters (until the whole string is returned)
        if (this.readerCount == -1) {
            // we have to wait for more data.
            try {
                InputStream sysIn = this.in;
                int read;
                char asChar;
                this.buffer.clearSecure();

                while ((read = sysIn.read()) != -1) {
                    asChar = (char)read;
                    if (asChar == '\n') {
                        this.readerCount = this.buffer.position();
                        this.buffer.rewind();
                        break;
                    } else {
                        this.buffer.writeChar(asChar);
                    }
                }
            } catch (IOException e1) {
            }
        }


        // EACH thread will have it's own count!
        if (this.readerCount == this.buffer.position()) {
            this.readerCount = -1;
            return '\n';
        } else {
            char c = this.buffer.readChar();
            return c;
        }
    }
}