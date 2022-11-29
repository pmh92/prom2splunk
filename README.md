# prom2splunk

The **prom2splunk** project aims to integrate a [Prometheus](https://prometheus.io) server with an existing [Splunk](https://www.splunk.com) Universal
Forwarder via TCP. The application leverages on the new Spring Boot's [Reactive](https://spring.io/reactive) framework to build a highly efficient
easy-to-use gateway to send Prometheus metrics to Splunk.

It offers the following features:

1. Spring Boot based configuration
2. Prometheus metrics exposure
3. Netty Reactive implementation

## Features

### 1. Spring Boot based configuration

By leveraging the Spring Framework for the configuration the application allows to easily configure the application using properties files. The
following configuration properties are available to the user.

| Name                                           | Type       | Default value | Description                                                     |
|------------------------------------------------|------------|---------------|-----------------------------------------------------------------|
| `prom2splunk.sink.tcp.host`                    | `String`   | localhost     | The host where the Splunk sink is listening for connections     |
| `prom2splunk.sink.tcp.port`                    | `int`      | --            | The TCP port where the Splunk sink is listening for connections |
| `prom2splunk.sink.tcp.secure`                  | `boolean`  | `false`       | Whether the Splunk sink is using TLS                            |
| `prom2splunk.sink.tcp.options`                 | `Map`      | empty         | TCP socket options                                              |
| `prom2splunk.sink.tcp.max-connections`         | `int`      | 64            | Maximum number of connections to the target                     |
| `prom2splink.sink.tcp.connection-idle-timeout` | `Duration` | 30s           | Time after which, an idle connection will be closed             |

The TLS connection uses the default Netty SslContext. By default, this is backed by JDK's `SSLContext` or OpenSSL's `SSL_CTX`.

The TCP socket options property allows for a detailed configuration of the underlying TCP socket established to the Splunk Universal Forwarder. Refer
to [Netty's documentation](https://netty.io/4.0/api/io/netty/channel/ChannelOption.html) for details of the available options and allowed values.

### 2. Prometheus metrics exposure

As an application intended to integrate Prometheus with other system the most straightforward decision was to also expose service metrics using
Prometheus. **prom2splunk** integrates with Prometheus by exposing an scrape target under the `/actuator/prometheus` endpoint. This can be overriden
by using the appropiate Spring Boot properties. Along with the metrics already exposed by Spring Boot the following metrics are also available for
collection.

###### sink.events

A **counter** for the number of events requested to be sent to the sink. It features the following labels

| Name        | Description                                                       |
|-------------|-------------------------------------------------------------------|
| `protocol`  | The protocol used to connect to the sink                          |
| `encoding`  | The encoding used when serializing the data                       |
| `exception` | Exception thrown when processing the record. `None` if successful |

###### sink.bytes

A **counter** for the number of bytes sent to the sink. It features the following labels

| Name        | Description                                                       |
|-------------|-------------------------------------------------------------------|
| `protocol`  | The protocol used to connect to the sink                          |
| `encoding`  | The encoding used when serializing the data                       |
| `exception` | Exception thrown when processing the record. `None` if successful |

### 3. Netty reactive implementation

The application is powered by Netty NIO TCP implementation. By leveraging NIO applications can reuse threads that otherwise become blocked when making
an IO call. This allows for a very efficient thread managemment system that enables the application to handle several packets per second with a
relatively small memory footprint.

## Usage

The application exposes default Prometheus [remote_write API](https://prometheus.io/docs/operating/integrations/#remote-endpoints-and-storage) and
therefore to configure it you just add a `remote_write` arget to the Prometheus configuration as in the example below:

```yaml
remote_write:
  - url: http://prom2splunk:8080/write
    write_relabel_configs:
      - source_labels: [ __name__ ]
        regex: system_cpu_usage
        action: keep
```

## FAQ

##### How can I send metrics to more than 1 Splunk index?

The driver for the application is to keep it lightweight and simple. Thus, no routing nor filtering algorithms are provided. To send metrics to more
than 1 Splunk index just configure the Splunk Universal forwarded to expose another TCP port and place a new instance of *prom2splunk* with a
different configuration.

##### How can I filter the metrics I want to send to Splunk?

The *remote_write* feature of the Prometheus server allows to filter the desired metrics based on several attributes prior to sending them to the
application and therefore shall be the way to go.



