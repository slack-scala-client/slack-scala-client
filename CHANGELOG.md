0.2.7 (2019-11-06)
------------------
* Scala 2.13 build
* Dependencies bumped
* Minor changes to event mappings
* Failures handling improved
* Message Block support

0.2.6 (2019-02-27)
------------------
* Support for lookupByEmail
* Minor changes to event mappings
* 3rd party dependencies upgraded: Akka, Play

0.2.5 (2018-12-05)
------------------
* Support for Events API (https://github.com/slack-scala-client/slack-scala-client/pull/76)
* Support for customizing the base URI for requests (e.g. for mocking the server) (https://github.com/slack-scala-client/slack-scala-client/pull/67)
* A few amendments to event mappings (https://github.com/slack-scala-client/slack-scala-client/pull/83)

Also, we started to run Travis CI checks for the project.

0.2.4 (2018-11-05)
------------------
* Fixes/amendments in te mapping of Slack API events
* Non-mandatory fields marked as `Option()`
* Support post.ephemeral
* Implemented ping/pong to sustain websocket connection

Since `0.2.4` the library sends a ping message to Slack every minute. Pong
message is received (but not checked upon). That is to sustain a Slack
websocket connection even if idle - see [Slack doc for ping and pong](https://api.slack.com/rtm#ping_and_pong).

Previously the library caused the client to reconnect every 1 or 2 minute
with the following messages:
```
[WebSocketClientActor] WebSocket disconnected.
[SlackRtmConnectionActor] WebSocket Client disconnected, reconnecting
[SlackRtmConnectionActor] Starting web socket client
```



0.2.3 (2018-03-08)
------------------
* Fixes/amendments in the mapping of Slack API events
* Simplified/improved the websocket failure/reconnect logic


0.2.2 (2017-08-15)
------------------

[Scaladoc for 0.2.2](http://doc.bryangilbert.com/slack-scala-client/0.2.2/index.html)

0.2.1 (2017-03-07)
------------------

_missing_


0.2.0 (2016-12-08)
------------------

* Replaced spray-websocket with the akka-http websocket implementation
* Replaced dispatch http client with the akka-http http client
    * Api clients now require an implicit ```ActorSystem``` instead of an ```ExecutionContext```
* Added cross build for Scala 2.12
* Cleaned up library API by properly scoping internal variables and implementation details

[Scaladoc for 0.2.0](http://doc.bryangilbert.com/slack-scala-client/0.2.0/)

0.1.8 (unknown)
---------------

[Scaladoc for 0.1.8](http://doc.bryangilbert.com/slack-scala-client/0.1.8/)
