/*
 * Copyright 2020. Pedro Morales
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.pmh92.prom2splunk.http;

import io.github.pmh92.prom2splunk.model.PrometheusSample;
import io.github.pmh92.prom2splunk.sink.SplunkSink;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import prometheus.Types;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * The controller that receives Promtheus metrics and forwards them to Splunk
 */
@RestController
public class PrometheusController {

    private final SplunkSink sink;

    public PrometheusController(SplunkSink sink) {
        this.sink = sink;
    }

    @PostMapping(value = "/write")
    public Mono<Void> sendMetric(@RequestBody prometheus.Remote.WriteRequest request) {
        return Flux.fromIterable(request.getTimeseriesList())
                .flatMap(ts -> {
                    Map<String,String> labels = ts.getLabelsList().stream()
                            .collect(Collectors.toMap(Types.Label::getName, Types.Label::getValue));
                    return Flux.fromIterable(ts.getSamplesList())
                            .map(s -> new PrometheusSample(s.getTimestamp(), labels, s.getValue()));
                })
                .flatMap(sink::handle)
                .then();
    }
}
