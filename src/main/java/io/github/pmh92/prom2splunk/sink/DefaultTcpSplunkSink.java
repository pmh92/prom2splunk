/*
 * Copyright 2021. Pedro Morales
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.pmh92.prom2splunk.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pmh92.prom2splunk.model.PrometheusSample;
import io.github.pmh92.prom2splunk.properties.TcpSinkConfigurationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpClient;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This configures and sets-up the TCP sink for all the metrics received
 */
@Service
public class DefaultTcpSplunkSink implements SplunkSink, SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTcpSplunkSink.class);
    private static final Tag NONE_EXCEPTION_TAG = Tag.of("exception", "None");
    private static final Tag JSON_ENCODING_TAG = Tag.of("encoding", MediaType.APPLICATION_JSON_VALUE);

    private final TcpSinkConfigurationProperties properties;
    private final TcpClient client;
    private final Encoder<? super PrometheusSample> encoder;
    private final MeterRegistry registry;

    private Connection connection;
    private DataBufferFactory bufferFactory;

    public DefaultTcpSplunkSink(MeterRegistry metrics, TcpSinkConfigurationProperties properties, ObjectMapper mapper) {
        // Defaults to JSON encoder / Investigate on using alternative encoders?
        this.encoder = new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON);
        this.properties = properties;
        // Configures the TcpClient to connect to
        TcpClient builder = TcpClient.create()
                .option(ChannelOption.SO_KEEPALIVE, true)
                .host(this.properties.getHost())
                .doOnConnected(c -> logger.info(String.format("Connected to: %s", c.address())))
                .doOnDisconnected(c -> logger.info(String.format("Disconnected from: %s", c.address())))
                .port(this.properties.getPort());
        if (this.properties.isSecure()) {
            builder = builder.secure();
        }
        // Set customized options
        for (Map.Entry<ChannelOption<Object>, Object> options : this.properties.getOptions().entrySet()) {
            builder = builder.option(options.getKey(), options.getValue());
        }
        this.client = builder;
        this.registry = metrics;
    }

    @Override
    public Mono<Void> handle(PrometheusSample sample) {
        logger.trace("About to send: {}", sample);
        if (isRunning()) {
            AtomicInteger bytes = new AtomicInteger(0);
            final Flux<DataBuffer> encoded = encoder.encode(Mono.just(sample), bufferFactory, ResolvableType.forInstance(sample), MediaType.APPLICATION_JSON, null)
                    .concatWith(encodeText("\r\n", StandardCharsets.UTF_8, bufferFactory))
                    .doOnNext(buf -> bytes.addAndGet(buf.readableByteCount()));
            return Mono.from(this.connection.outbound()
                    .send(encoded.doOnDiscard(PooledDataBuffer.class, PooledDataBuffer::release).map(NettyDataBufferFactory::toByteBuf)))
                    .doOnSuccessOrError((r, ex) -> recordMetrics(bytes.get(), 1, sample, ex));
        } else {
            throw new IllegalArgumentException("TCP Client is not running");
        }
    }

    private void recordMetrics(int bytes, int events, PrometheusSample sample, Throwable error) {
        Counter.builder("sink.bytes").baseUnit("bytes").description("Bytes sent to the sink")
                .tags(tags(sample, error)).register(this.registry)
                .increment(bytes);
        Counter.builder("sink.events").description("Events sent to the sink")
                .tags(tags(sample, error)).register(this.registry)
                .increment(events);
    }

    private Iterable<Tag> tags(PrometheusSample sample, Throwable error) {
        return Arrays.asList(
                Tag.of("protocol", this.client.isSecure() ? "tls" : "tcp"),
                JSON_ENCODING_TAG,
                error != null ? Tag.of("exception", error.getClass().getName()) : NONE_EXCEPTION_TAG
        );
    }

    @Override
    public boolean isAutoStartup() {
        return this.properties.isEagerLoad();
    }

    @Override
    public void start() {
        this.connection = this.client.connectNow();
        // Configures bytes allocator
        final ByteBufAllocator alloc = this.connection.outbound().alloc();
        this.bufferFactory = new NettyDataBufferFactory(alloc);
    }

    @Override
    public void stop() {
        this.connection.disposeNow();
        this.connection = null;
    }

    @Override
    public boolean isRunning() {
        return this.connection != null;
    }

    private Mono<DataBuffer> encodeText(CharSequence text, Charset charset, DataBufferFactory bufferFactory) {
        byte[] bytes = text.toString().getBytes(charset);
        return Mono.just(bufferFactory.wrap(bytes)); // wrapping, not allocating
    }
}
