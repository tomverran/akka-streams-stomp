# Akka Streams STOMP connector

[![Build Status](https://travis-ci.org/tomverran/akka-streams-stomp.svg?branch=master)](https://travis-ci.org/tomverran/akka-streams-stomp)
[ ![Download](https://api.bintray.com/packages/tomverran/maven/akka-streams-stomp/images/download.svg) ](https://bintray.com/tomverran/maven/akka-streams-stomp/_latestVersion)

Allows you to stream messages from a queue server supporting STOMP.
This library is a *very* minimal implementation of *some* of the STOMP protocol but is sufficient when
you want to treat your STOMP queue like a stream (i.e. you don't need to acknowledge messages).

It emits a stream of `StompMessage` which contain the message verb, headers and the body as a `ByteString`
for further decoding downstream.

## What works

- Connecting to STOMP servers
- Subscribing to a single queue
- Parsing stomp messages as a stream
- Binary content with a content-length header

## What is left to do

- Multiple queues?
- Acknowledging messages?
- Handling STOMP escape sequences

## How to install

Add the following to your `build.sbt`

```scala
resolvers += Resolver.bintrayRepo("tomverran", "maven")
libraryDependencies ++= "io.tvc" %% "akka-streams-stomp" % "0.0.1"
```

## How to use

```scala
import io.tvc.stomp._
val source: Source[StompMessage[ByteString, NotUsed]] = StompSource(
    queue = QueueName("/queue/name"),
    host = InetSocketAddress.createUnresolved("127.0.0.1", 61613),
    credentials = Some(Credentials(login = "your-name", passcode = "your-password")) // or None for unsecured
)

```
