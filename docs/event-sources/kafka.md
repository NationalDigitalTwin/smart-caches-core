# Kafka Event Source

The `event-source-kafka` module provides the `KafkaEventSource` which enables reading of events from a Kafka topic. The
key and value type for the topic, and their associated deserializers, must be provided.

Additionally, there are also several pre-existing subclasses of this for the common case of reading RDF Graphs/Datasets
from a Kafka topic:

- `KafkaDatasetGraphSource`
- `KafkaRdfPayloadSource`

When using these convenience classes you need only provide type parameters and deserializer configuration for the event
key since the value is assumed to be an Apache Jena `DatasetGraph` or our [`RdfPayload`](index.md#rdfpayload) type.

We also provide several Kafka `Deserializer` and `Serializer` implementations which are used by the above, these are as
follows:

- `GraphDeserializer` and `GraphSerializer` for handling Apache Jena `Graph` instances
- `DatasetGraphDeserializer` and `DatasetGraphSerializer` for handling Apache Jena `DatasetGraph` instances
- `RdfPayloadDeserializer` and `RdfPayloadSerializer` for handling mixtures of Apache Jena `DatasetGraph` and Apache
  Jena `RDFPatch` instances

## Behaviours

- Unbounded
- Buffered
- Configurable Read Policy: Yes

## Read Policies

The read policy for a Kafka topic is configured via the `KafkaReadPolicy` interface with common read policies provided
via the `KafkaReadPolicies` static class. Kafka read policies fall into two categories, automatic policies that leverage
Kafka's concept of Consumer Groups to automatically assign topic partitions to consumers, and manual policies that allow
manually reading all partitions of a topic. Automatic policies **SHOULD** be preferred as they provide automatic
scalability via Kafka's Consumer Group Rebalance protocol and automated tracking of the offsets within a topic allowing
consumers to automatically resume from the last processed event.

With automatic policies a consumer is identified by the Consumer Group ID it provides when constructing your
`KafkaEventSource`. An application presenting the same Consumer Group ID receives a share of the topic partitions to
read and has its progress reading the topic tracked by Kafka. Therefore, if the application fails for any reason, upon
restart the application can resume reading from its previous position.

The following automatic policies can be accessed via static methods on `KafkaReadPolicies`:

- `KafkaReadPolicies.fromEarliest()` - Reads all events in a topic starting from the earliest available offsets if the
  consumer has not previously read this topic. This **SHOULD** be used by default since that gives exactly-once
  processing semantics.
- `KafkaReadPolicies.fromLatest()` - Reads events in a topic starting from the latest available offsets if the consumer
  has not previously read this topic. With this policy only events that were added to a topic after the consumer was
  first started will be read, meaning the portion of the topic that was populated prior to the consumer first starting
  will never be read.
- `KafkaReadPolicies.fromBeginning()` - Reads events in a topics always starting from the beginning. With this policy a
  topic is always fully read regardless of whether it has previously been read by this consumer. This can be useful if
  you need to reprocess a topic for any reason e.g. pipeline configuration changed.

## Kafka Auto-Commit

The default behaviour of the Kafka event sources is to automatically commit Kafka offsets as events are read from the
source topic.  Auto-commits happen each time that the internal events buffer is exhausted, and when the event source is
closed.

Alternatively, auto-commit may be disabled, in which case offsets are only committed when the event sources
`processed()` method is called with a batch of events that have been processed.  This mode is primarily intended for
when the Kafka source is connected to a data processing pipeline and wants to only update offsets when it has finished
processing events, rather than merely having read them as with the default auto-commit behaviour.

## Parameters

The primary parameters are the bootstrap servers for connecting to Kafka, the topic to read and the Consumer Group ID.
As already discussed the Consumer Group ID is particularly important since it allows Kafka to automatically balance the
read load for a consumer and track consumer read process provided both auto-scaling and failover.

The secondary parameters provide the key and value deserializer classes for deserializing the key and value of the Kafka
events, which are stored as `byte[]` into strongly typed Java objects.

Next there is the max poll records, which tells Kafka the maximum number of records you wish to retrieve in one fetch
operation, and the [read policy](#read-policies), which tells Kafka how you want to read the topic.

Additionally advanced users may also want to configure further parameters such as lag reporting interval and
auto-commit behaviour.

Given the large number of parameters for a Kafka event source, these sources are built via a Builder API.  Each concrete
class provides a static method used to obtain a builder and these builders provide standard methods for
setting the various Kafka related parameters.

## Example Usage

Here we are going to create a `KafkaEventSource` that is backed by the `knowledge` topic:

```java
EventSource<String, DatasetGraph> source 
  =  KafkaEventSource.create<String, DatasetGraph>()
                     .bootstrapServers("localhost:9092")
                     .topic("knowledge")
                     .consumerGroup("my-example-app")
                     .maxPollRecords(100)
                     .fromEarliest()
                     .keyDeserializer(StringDeserializer.class)
                     .valueDeserializer(DatasetGraphDeserializer.class)
                     .autoCommit(true)
                     .build();
while (!source.isExhausted()) {
    Event<String, DatasetGraph> next = source.poll(Duration.ofSeconds(5));
    if (next == null)
      continue;

    System.out.println("Got a DatasetGraph with " + next.value().size() + " quads");
}
```

Firstly you will note that since creating a Kafka source requires a lot of parameters, some of them optional, a builder
pattern is used in creating a Kafka Source.

Note that because the Kafka source is unbounded our `while` loop here is infinite because `isExhausted()` will always
return `false` unless `close()` is called on the `source`.

As noted earlier you could simplify the event source definition in this example by using the convenience
`KafkaDatsetGraphSource` class e.g.

```java
EventSource<String, Graph> 
  = KafkaDatasetGraphSource.<String>createGraph()
                           .bootstrapServers("localhost:9092")
                           .topic("knowledge")
                           .consumerGroup("my-example-app")
                           .maxPollRecords(100)
                           .fromEarliest()
                           .keyDeserializer(StringDeserializer.class)
                           .build();
```

## Default RDF Language

When using the `GraphDeserializer` or `DatasetGraphDeserializer`, whether directly or indirectly via one of our
convenience classes, the default RDF language used for parsing event keys/values is NTriples or NQuads respectively.

Regardless of the default language if the headers for an event contain a `Content-Type` header denoting the MIME
type used to serialize the RDF then the appropriate parser will be selected instead of assuming the default language.

In order to use `RdfPayload` based event sources any RDF Patch **MUST** have a `Content-Type` header identifying the
format used to serialize the RDF Patch otherwise the event will be assumed to represent a Dataset and be processed as
noted above.

## Lazy Deserialization

As of `0.14.0` the `RdfPayloadDeserializer` now deserializes `RdfPayload` instances lazily.  This helps to avoid head of
line blocking that can occur if the payload bytes on Kafka are not valid RDF data, or have an incorrect `Content-Type`
header present.  This allows for consuming applications to make their own decisions about how they handle malformed RDF
payloads while still making progress on reading the input Kafka topic.  Such payloads will instead throw a
`RdfPayloadException` when accessing the `getDataset()`/`getPatch()` method as appropriate for the payload type. Calling
the `isDataset()` and `isPatch()` methods still indicates the payload type without deserializing it.

If the old eager parsing behaviour is preferred then you can enable that by setting the `rdf.payload.parsing.eager`
configuration key on your event source e.g.

```java
EventSource<String, Graph> 
  = KafkaRdfPayloadSource.<String>createRdfPayload()
                           .bootstrapServers("localhost:9092")
                           .topic("knowledge")
                           .consumerGroup("my-example-app")
                           .maxPollRecords(100)
                           .fromEarliest()
                           .keyDeserializer(StringDeserializer.class)
                           .consumerConfig(RdfPayloadDeserializer.EAGER_PARSING_CONFIG_KEY, "true")
                           .build();
```

Note also that the corresponding `RdfPayloadSerializer` is also capable of handling malformed payloads, when it 
encounters a malformed payload that contains raw bytes it simply writes those bytes back out again.  This pushes the 
decision of how to handle the malformed event onto the subsequent consumer so ultimately some consumer in your pipeline
needs to handle these events somehow e.g. write them to a DLQ, generate an alert etc.

In the case of a well formed payload the `RdfPayloadSerializer` will still call `getPatch()` or `getDataset()` causing
the payload to be parsed, before it is then written back out again.  While this might seem like unnecessary work if we
don't do this we can't honour the `Content-Type` header on the event.  This may have been intentionally modified by the
consuming application in order to effect a data format transformation and not honouring it would create malformed
payloads for the downstream consumers.

## Metrics

The `KafkaEventSource` collects several metrics that may be of interest in observing the performance of an
application:

- `messaging.kafka.poll_timing` - A histogram of how long each internal `KafkaConsumer.poll()` takes in seconds.
- `messaging.kafka.fetch_events_count` - A histogram of how many events were fetched on each `KafkaConsumer.poll()` call.
- `messaging.kafka.lag` - A gauge of the current read lag i.e. how far behind on reading a topic the application is.

All of these metrics are labelled with `messaging.kafka.consumer_group` and `messaging.destination` allowing you to
distinguish between different applications running against the same topic.

## External `OffsetStore`

As of 0.12.4 the `KafkaEventSource` and its descendants supports being configured with an external
[`OffsetStore`](index.md#offsetstore) to use as an additional location to commit offsets to.

Currently, offsets are stored into the external `OffsetStore` whenever the `KafkaEventSource` would normally commit
offsets (see [Auto-Commit](#kafka-auto-commit) notes) **BUT** they are never read back in and used. Future work will
enable rectifying this external offset information with the internal Kafka offsets.

## Other Utilities

This module also provides a [`KafkaSink`](../sinks/kafka.md) that can be used to write events back to a Kafka topic.

## Testing

This module also has a `tests` classifier that includes a `KafkaTestCluster` class that uses the [Test Containers][1]
framework to stand up a single node Kafka cluster for testing that has a single topic `tests` automatically created.
This can be used as follows:


```java
KafkaTestCluster kafka = new KafkaTestCluster();

// Start up Kafka, this blocks until the cluster is ready
kafka.setup();

// Get connection details
String bootstrapServers = kafka.getBootstrapServers();

// Actually do something with the cluster...

// Teardown Kafka
kafka.teardown();
```

Typically, you will want to put the `setup()` and `teardown()` calls in appropriately annotated methods, e.g.
`@BeforeClass` and `@AfterClass`, within your test classes.

This class also provides a `resetTestTopic()` that will delete and recreate the `tests` topic.  Note that as an
asynchronous system resetting a topic is not always instantaneous.  Therefore, we suggest that you inject a short sleep
(~500 milliseconds) after calling this method as otherwise subsequent tests may see wrong/incomplete data as the topic
is deleted and recreated.

If your tests need to work with additional topics you can call `getAdminClient()` to obtain the `AdminClient` for
managing the cluster.  There are also `resetTopic(String)` and `deleteTopic(String)` methods for quick management of
additional topics.

### Testing Secure Clusters

As of `0.12.0` we now also provide a `SecureKafkaTestCluster` which creates a single node Kafka cluster that has
authentication enabled so clients must supply a username and password to connect to the cluster.  By default, this
creates an admin user named `admin` with the password `admin-secret`, and a normal user named `client` with the password
`client-secret`.  There is also a constructor that allows you to configure the admin account and additional user
accounts explicitly.

The `getAdminUsername()` and `getAdminPassword()` methods will allow you to find the admin credentials configured for
the cluster.  The `getAdditionalUsers()` method supplies a `Map<String, String>` detailing any additional users
configured for the cluster.

The `AdminClient` supplied by calling `getAdminClient()` will already be configured with the admin credentials.  If you
are configuring other Kafka related class in your tests, e.g. `KafkaEventSource` or `KafkaSink`, then you can use the
`consumerConfig(Properties)`/`producerConfig(Properties)` methods on the appropriate builders supplying the value from
calling `SecureKafkaTestCluster.getClientProperties(String, String)`.

[1]: https://testcontainers.com