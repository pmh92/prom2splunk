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

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.xerial.snappy.SnappyInputStream;
import reactor.core.publisher.Flux;

/**
 * By default, all
 */
public class SnappyContentEncodedRequest extends ServerHttpRequestDecorator {

    public SnappyContentEncodedRequest(ServerHttpRequest delegate) {
        super(delegate);
    }

    /**
     * The content body shall is encoded with Snappy.
     *
     * @return a new {@code Flux<DataFuffer>} containing the decoded body
     */
    @Override
    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
    public Flux<DataBuffer> getBody() {
        return DataBufferUtils.join(super.getBody()).flatMapMany(body ->
                DataBufferUtils.readInputStream(() -> new SnappyInputStream(body.asInputStream(true)), body.factory(), body.capacity()));
    }

}
