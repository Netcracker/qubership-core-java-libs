package com.netcracker.cloud.junit.cloudcore.extension.service;

import lombok.Data;

import java.net.URL;
import java.util.function.Function;

public interface PortForwardResultBiFunction<T> extends Function<NetSocketAddress, T> {

    class NetSocketAddressFunction implements PortForwardResultBiFunction<NetSocketAddress> {
        @Override
        public NetSocketAddress apply(NetSocketAddress netSocketAddress) {
            return netSocketAddress;
        }
    }

    @Data
    class URLFunction implements PortForwardResultBiFunction<URL> {
        private final String scheme;

        @Override
        public URL apply(NetSocketAddress netSocketAddress) {
            return netSocketAddress.toUrl(scheme);
        }
    }

    class HttpURLFunction extends URLFunction {
        public HttpURLFunction() {
            super("http");
        }
    }

    class HttpsURLFunction extends URLFunction {
        public HttpsURLFunction() {
            super("https");
        }
    }


}

