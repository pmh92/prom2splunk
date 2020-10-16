/*
 * Copyright 2020. Pedro Morales
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.pmh92.prom2splunk.properties;

import io.netty.channel.ChannelOption;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
@Validated
@ConfigurationProperties("prom2splunk.sink.tcp")
public class TcpSinkConfigurationProperties {

    /**
     * The host where the Splunk sink is listening for connections
     */
    @NotEmpty
    private String host = "localhost";

    /**
     * The TCP port where the Splunk sink is listening for connections
     */
    @NotNull
    @Range(min = 1, max = 65535)
    private int port;

    /**
     * Whether the Splunk sink is using TLS
     */
    private boolean secure = false;
    /**
     * Whether the Splunk sink shall eagerly connect to the target
     */
    private boolean eagerLoad = true;

    /**
     * TCP socket options.
     * @see ChannelOption
     */
    private Map<ChannelOption<Object>, Object> options = new LinkedHashMap<>();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isEagerLoad() {
        return eagerLoad;
    }

    public void setEagerLoad(boolean eagerLoad) {
        this.eagerLoad = eagerLoad;
    }

    public Map<ChannelOption<Object>, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<ChannelOption<Object>, Object> options) {
        this.options = options;
    }
}
