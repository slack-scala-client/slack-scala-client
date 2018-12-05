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


0.2.3 (2018-03-08)
------------------
* Fixes/amendments in the mapping of Slack API events
* Simplified/improved the websocket failure/reconnect logic


0.2.2 (2017-08-15)
------------------

_missing_

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
