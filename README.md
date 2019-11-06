# slack-scala-client

[![Build Status](https://travis-ci.com/slack-scala-client/slack-scala-client.svg?branch=master)](https://travis-ci.com/slack-scala-client/slack-scala-client)

A Scala library for interacting with the Slack API and real time messaging interface


## Installation

### SBT

Add SBT dependency:

    libraryDependencies += "com.github.slack-scala-client" %% "slack-scala-client" % "0.2.7"


### Maven

Scala 2.13:

        <dependency>
            <groupId>com.github.slack-scala-client</groupId>
            <artifactId>slack-scala-client_2.13</artifactId>
            <version>0.2.7</version>
        </dependency>

Scala 2.12:

        <dependency>
            <groupId>com.github.slack-scala-client</groupId>
            <artifactId>slack-scala-client_2.12</artifactId>
            <version>0.2.7</version>
        </dependency>

Scala 2.11:

        <dependency>
            <groupId>com.github.slack-scala-client</groupId>
            <artifactId>slack-scala-client_2.11</artifactId>
            <version>0.2.7</version>
        </dependency>

## API Client Usage

There are two different API clients, one exposing an asynchronous interface and the other exposing a synchronous interface. They can be imported from the `slack.api` package:

```scala
import slack.api.SlackApiClient          // Async
import slack.api.BlockingSlackApiClient  // Blocking
```

Creating an instance of either client simply requires passing in a Slack api token:

```scala
val token = "<Your Token Here>"
val client = SlackApiClient(token)
```

Calling any api functions requires an implicit `ActorSystem`... one can be created simply:

```scala
implicit val system = ActorSystem("slack")
```

The async client returns futures as the result of each of its API functions:

```scala
val client = SlackApiClient(token)
val res = client.listChannels() // => Future[Seq[Channel]]

res.onComplete {
    case Success(channels) =>  //...
    case Failure(err) => // ...
}
```

...while the blocking client will block the current thread until the API response has been received:

```scala
val client = BlockingSlackApiClient(token)  // Default timeout of 5 seconds
val channels = client.listChannels()  // => Seq[Channel]
```

The API clients implement the full Slack API. A full list of the available endpoints can be found directly on the classes: [SlackApiClient](src/main/scala/slack/api/SlackApiClient.scala#L83-L507) and [BlockingSlackApiClient](src/main/scala/slack/api/BlockingSlackApiClient.scala#L28-L324)


## RTM Client Usage

The real time messaging client is implemented using akka and requires having an implicit `ActorSystem` in scope. Either an `ActorSystem` or `ActorContext` will work:

```scala
import slack.rtm.SlackRtmClient
import akka.actor.ActorSystem

implicit val system = ActorSystem("slack")
```

Creating an instance of the RTM client requires an API token, just like the API clients:

```scala
val token = "<Your Token Here>"
val client = SlackRtmClient(token)
```

Based on the stream of events coming in, the client maintains active state that contains things like channels and users. It can also be used to look up the ID of a user or channel by name:

```scala
val state = client.state
val selfId = state.self.id
val chanId = state.getChannelIdForName("general") // => Option[String]
```

Sending a message is pretty simple:

```scala
val generalChanId = state.getChannelIdForName("general").get
client.sendMessage(generalChanId, "Hello!")
```

Messages can be received very simply as well:

```scala
client.onMessage { message =>
    println(s"User: ${message.user}, Message: ${message.text}")
}
```

Additionally, the client can be used to receive any event sent from Slack:

```scala
client.onEvent {
    case e: Message => ...
    case e: UserTyping => ...
    case e: ChannelDeleted => ...
}
```

A full list of events can be found in [Events.scala](src/main/scala/slack/models/Events.scala). One thing to note is the two above functions return an `ActorRef` which is a handle to the underlying actor running the above handler function. This can be used to terminate the handler by terminating the actor: ```system.stop(handler)```, or unregistering it as a listener: ```client.removeEventListener(handler)```

An Akka actor can be manually registered as an event listener and all events will be sent to that actor:

```scala
val actor = system.actorOf(Props[SlackEventHandler])
client.addEventListener(actor)
// Time Passes...
client.removeEventListener(actor)
```

Finally, an RTM client can easily be terminated and cleaned up by calling close:

```scala
client.close()
```


## Simple Bot Example

This is a full implementation of a Slack bot that will listen for anyone mentioning it in a message and will respond to that user.

```scala
val token = "..."
implicit val system = ActorSystem("slack")
implicit val ec = system.dispatcher

val client = SlackRtmClient(token)
val selfId = client.state.self.id

client.onMessage { message =>
  val mentionedIds = SlackUtil.extractMentionedIds(message.text)

  if(mentionedIds.contains(selfId)) {
    client.sendMessage(message.channel, s"<@${message.user}>: Hey!")
  }
}
```

## Caveat Emptor

- The Slack API contains a lot methods and not every implemented API method has been executed (i.e. some may not work; pull requests accepted!)
- Responses to RTM messages sent out are not currently checked to verify they were successfully received
- Investigate a way to ensure all missed messages are received during a disconnection
- A small number of response types have yet to be fleshed out


## Changelog

Changelog can be found [here](CHANGELOG.md)
