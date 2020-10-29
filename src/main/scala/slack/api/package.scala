package slack

import cats.Id

package object api {
  type BlockingSlackApiClient = SlackApiClientF[Id] //only for backward compatibility
}
