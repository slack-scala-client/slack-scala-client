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
