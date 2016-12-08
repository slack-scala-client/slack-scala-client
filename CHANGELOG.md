0.2.0 (2016-12-08)
------------------

* Replaced spray-websocket with the akka-http websocket implementation
* Replaced dispatch http client with the akka-http http client
    * Api clients now require an implicit ```ActorSystem``` instead of an ```ExecutionContext```
* Added cross build for Scala 2.12
* Cleaned up library API by properly scoping internal variables and implementation details
