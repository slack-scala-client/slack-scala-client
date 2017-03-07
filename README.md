slack-scala-client
==================

A Scala library for interacting with the Slack API and real time messaging interface


Installation
------------

Add SBT dependency:

    libraryDependencies += "com.github.gilbertw1" %% "slack-scala-client" % "0.2.1"

Scaladoc
--------

* [0.2.0](http://doc.bryangilbert.com/slack-scala-client/0.2.0/)
* [0.1.8](http://doc.bryangilbert.com/slack-scala-client/0.1.8/)


API Client Usage
----------------

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


RTM Client Usage
----------------

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


Simple Bot Example
------------------

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


WebSocket Re-Connection Behavior
--------------------------------

The WebSocket connection sends a `PingFrame` every second and if it ever goes more than 10 seconds without receiving a `PongFrame`, will terminate the WebSocket connection and attempt to establish a new connection. It will continue to do this using an exponential back-off until it is able to successfully reconnect to the RTM WebSocket API.


Caveat Emptor
-------------

- The Slack API contains a lot methods and not every implemented API method has been executed (i.e. some may not work; pull requests accepted!)
- Responses to RTM messages sent out are not currently checked to verify they were successfully received (Coming Soon!)
- Investigate a way to ensure all missed messages are received during a disconnection
- A small number of response types have yet to be fleshed out


Changelog
---------

Changelog can be found [here](CHANGELOG.md)
