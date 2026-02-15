import java.io.IOException;
import java.io.InputStream;

class FixedSizeInputStream extends InputStream {
    private int remaining;
    private final InputStream src;

    FixedSizeInputStream(InputStream src, int size) {
        this.remaining = size;
        this.src = src;
    }

    @Override
    public int read() throws IOException {
        if (remaining == 0) {
            return -1;
        }

        int b = src.read();
        // not sure about that
        if (b != -1) {  // Only decrement if we actually read a byte
            remaining--;
        }
        assert remaining >= 0;

        return b;
    }
}