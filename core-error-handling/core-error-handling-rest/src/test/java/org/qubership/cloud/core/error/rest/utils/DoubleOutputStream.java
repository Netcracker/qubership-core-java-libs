package org.qubership.cloud.core.error.rest.utils;

import java.io.IOException;
import java.io.OutputStream;

public class DoubleOutputStream extends OutputStream {
    OutputStream s1;
    OutputStream s2;

    public DoubleOutputStream(OutputStream s1, OutputStream s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    @Override
    public void write(int b) throws IOException {
        s1.write(b);
        s2.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        s1.write(b, off, len);
        s2.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        s1.flush();
        s2.flush();
    }

    @Override
    public void close() throws IOException {
        s1.close();
        s2.close();
    }
}
