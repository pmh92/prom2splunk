/*
 * Copyright 2020. Pedro Morales
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.pmh92.prom2splunk.sink;

import io.github.pmh92.prom2splunk.model.PrometheusSample;
import reactor.core.publisher.Mono;

/**
 * Filter for a {@link SplunkSink}
 */
@FunctionalInterface
public interface SinkFilter {
    Mono<Void> filter(PrometheusSample sample, SplunkSink next);

    /**
     * Compose two filters
     * @param other the next filter in the chain
     * @return a composition of the current and the mext filter
     */
    default SinkFilter andThen(SinkFilter other) {
        return (sample, next) -> this.filter(sample, afterSample -> other.filter(afterSample, next));
    }

    /**
     * Applies a filter to a given sink. Adding the filter to the processing chain
     * @param sink the sink to filter
     * @return the filtered sink
     */
    default SplunkSink apply(SplunkSink sink) {
        return sample -> this.filter(sample, sink);
    }
}
