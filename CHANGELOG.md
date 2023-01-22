0.4.2 (2023-01-22)
------------------
* Support for user_status_change (#258)
* Dependency version upgrades

0.4.1 (2022-12-16)
------------------
* Support for user_profile_changed and user_huddle_changed events
* Dependency version upgrades

0.4.0 (2022-09-28)
------------------
* `rtm.connect` instead of `rtm.start` (#210)
* Dependency version upgrades

0.3.1 (2022-05-01)
------------------
* Dependency version ugprades - incl. Akka 2.6
* Update deprecated `im.open` and `im.close` API endpoints (#227) 

0.3.0 (2022-02-21)
-------------------
* Deprecating `listChannels` (#197)
* Code re-arrangement (#204)
* Dependency version upgrades - resulting in a separate artifact: slack-scala-client-models
* Drop support for Scala 2.11 (#201)

0.2.17 (2021-08-17)
-------------------
* Fix for no group members in the rtm.start API response (#193)

0.2.16 (2021-04-27)
-------------------
* Fix for tokens not supported in querystring (#184)

0.2.15 (2021-04-15)
-------------------
* Simplifying the model for Message, subtypes and replies (#182)
* Dependency version upgrades

0.2.14 (2021-02-25)
-------------------
* Add support for conversations.setTopic (#174)
* Dependency version upgrades

0.2.13 (2021-01-05)
-------------------
* Fix for unfurl_links and unfurl_media support (#166)
* Dependency version upgrades

0.2.12 (2021-01-02)
-------------------
* Add support for unfurl_links & unfurl_media (#162)
* Dependency version upgrades

0.2.11 (2020-10-29)
-------------------
* Adding support for attachments in plain Messages (#152)
* Also a few 3rd-party library upgrades

0.2.10 (2020-05-31)
-------------------
* Fixing ping (#136)
* Fixing mapping for replies to bot messages (#135)

0.2.9 (2020-04-06)
------------------
* proxy support added
* dependency versions bumped
* `conversations.lists` support
* New events mapped - subteams and app actions

Internal efforts:
* code clean-up, incl. some warnings removed
* employing scala-steward

0.2.8 (2020-02-17)
------------------
* Content-type fix (#110)
* getDetailedFileInfo/getFileInfo support (#111)

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
