/*
 * Copyright 2020. Pedro Morales
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.pmh92.prom2splunk.http.snappy;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * By default Prometheus sends the metrics to the remote_write endpoint using Snappy compression. This filter decompresses the payload upon arrival
 *
 * @see SnappyContentEncodedRequest
 */
@Component
public class SnappyWebFilter implements WebFilter {

    /**
     * Filter the request to perform Snappy decompression if required
     *
     * @param exchange the current server exchange
     * @param chain    provides a way to delegate to the next filter
     *
     * @return {@code Mono<Void>} to indicate when request processing is complete
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isSnappy(request)) {
            request = new SnappyContentEncodedRequest(request);
        }
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isSnappy(ServerHttpRequest request) {
        final List<String> contentEncoding = request.getHeaders().get(HttpHeaders.CONTENT_ENCODING);
        return contentEncoding != null && contentEncoding.stream()
                .anyMatch(h -> h.trim().equalsIgnoreCase("snappy"));
    }
}
