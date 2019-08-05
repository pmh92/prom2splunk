/*
 * Copyright 2019. Pedro Morales
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.pmh92.prom2splunk.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pmh92.prom2splunk.model.MetricSample;
import io.github.pmh92.prom2splunk.properties.TcpSinkConfigurationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * This configures and sets-up the TCP sink for all the metrics received
 */
@Service
public class TcpSink implements SplunkSink, SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(TcpSink.class);

    private final TcpSinkConfigurationProperties properties;
    private final TcpClient client;
    private final Encoder<Object> encoder;

    private Connection connection;
    private final DataBufferFactory factory = new DefaultDataBufferFactory();

    private Counter bytesCounter;
    private Counter eventsCounter;

    public TcpSink(MeterRegistry metrics, TcpSinkConfigurationProperties properties, ObjectMapper mapper) {
        // Defaults to JSON encoder / Investigate on using alternative encoders?
        registerMetrics(metrics);
        this.encoder = new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON);
        this.properties = properties;
        // Configures the TcpClient to connect to
        TcpClient client = TcpClient.create()
                .option(ChannelOption.SO_KEEPALIVE, true)
                .host(this.properties.getHost())
                .doOnConnected(c -> logger.info(String.format("Connected to: %s", c.address())))
                .doOnDisconnected(c -> logger.info(String.format("Disconnected from: %s", c.address())))
                .port(this.properties.getPort());
        if (this.properties.isSecure())
            client = client.secure();
        // Set customized options
        for (Map.Entry<ChannelOption<Object>, Object> options : this.properties.getOptions().entrySet()) {
            client = client.option(options.getKey(), options.getValue());
        }
        this.client = client;
    }

    private void registerMetrics(MeterRegistry metrics) {
        // Register metrics
        this.bytesCounter = Counter.builder("sink.bytes")
                .baseUnit("bytes")
                .tag("protocol", "tcp")
                .description("Bytes sent to the sink")
                .register(metrics);
        this.eventsCounter = Counter.builder("sink.events")
                .description("Events sent to the sink")
                .tag("protocol", "tcp")
                .register(metrics);

    }

    @Override
    public Mono<Void> apply(MetricSample sample) {
        this.eventsCounter.increment();
        logger.trace("About to send: {}", sample);
        final ByteBuf separator = Unpooled.wrappedBuffer(this.properties.getSeparator().getBytes(StandardCharsets.UTF_8));
        final Flux<ByteBuf> encoded = encoder.encode(Mono.just(sample), factory, ResolvableType.forInstance(sample), MediaType.APPLICATION_JSON, null)
                .map(DataBuffer::asByteBuffer)
                .doOnNext(buf -> this.bytesCounter.increment(buf.capacity()))
                .map(Unpooled::wrappedBuffer);
        if (!isRunning()) {
            throw new IllegalArgumentException("TCP Client is not running");
        }
        return Mono.from(this.connection.outbound().send(Flux.concat(encoded, Mono.just(separator))));
    }

    @Override
    public boolean isAutoStartup() {
        return this.properties.isEagerLoad();
    }

    @Override
    public void start() {
        this.connection = this.client.connectNow();
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
}
