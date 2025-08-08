package com.netcracker.cloud.maas.client;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {
    @SneakyThrows
    public static String readResourceAsString(String filename) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String path = stackTrace[2].getClassName().replaceAll("\\.", "/") + "/" + filename;
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url == null) {
            throw new IOException("No resource found by classpath:" + path);
        }
        return Files.readString(Paths.get(url.toURI()), StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    public interface OmnivoreRunnable {
        void run() throws Exception;
    }

    public static void withProp(String prop, String value, OmnivoreRunnable test) {
        String save = System.getProperty(prop);
        if (value == null) {
            System.clearProperty(prop);
        } else {
            System.setProperty(prop, value);
        }

        try {
            test.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (save == null) {
                System.clearProperty(prop);
            } else {
                System.setProperty(prop, save);
            }
        }
    }

   public static class DoubleOutputStream extends OutputStream {
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


}
