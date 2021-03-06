package models

import akka.actor._
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

object ChatRoom {
    implicit val timeout = Timeout(1 second)

    lazy val default = {
        val roomActor = Akka.system.actorOf(Props[ChatRoom])

        // Create a bot user (GM)
        Robot(roomActor)

        roomActor
    }

    def join(username: String): scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {
        (default ? Join(username)).map {
            case Connected(enumerator) =>
                // Create an Iteratee to consume the feed
                val iteratee = Iteratee.foreach[JsValue] { event => default ! Talk(username, (event \ "text").as[String])
                }.map { _ => default ! Quit(username) }

                (iteratee, enumerator)

            case CannotConnect(error) =>
                // Connection error
                // A finished Iteratee sending EOF
                val iteratee = Done[JsValue, Unit]((), Input.EOF)

                // Send an error and close the socket
                val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

                (iteratee, enumerator)
        }
    }
}

class ChatRoom extends Actor {
    var members = Set.empty[String]
    val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

    def receive = {
        case Join(username) => {
            if (members.contains(username)) sender ! CannotConnect("This username is already used")
            else {
                members = members + username
                sender ! Connected(chatEnumerator)
                self ! NotifyJoin(username)
            }
        }

        case NotifyJoin(username) => { notifyAll("join", username, "has entered the room") }

        case Talk(username, text) => {
            val trimmedText = text.trim()
            if (members.contains(username)) {
                if (trimmedText.startsWith("/")) {
                    if (trimmedText.startsWith("/whisper")) {
                        val splittedTexts = trimmedText.split(' ')
                        val target = splittedTexts(1)
                        notify("talk", username, text, Set(username, target))
                    } else notify("talk", username, text, Set("Robot", username))
                } else notifyAll("talk", username, text)
            }
        }

        case SystemAll(username, text, values) => { notifyAll("talk", username, text, values) }

        case Whisper(username, target, text) => { notify("talk", username, text, Set(username, target)) }

        case System(username, target, text, values) => { notify("talk", username, text, Set(username, target), values) }

        case Quit(username) => {
            members = members - username
            notifyAll("quit", username, "has left the room")
        }

        case Kick(username, target) => {
            members = members - target
            notifyAll("quit", target, "has been kicked.")
        }
    }

    def notify(kind: String, user: String, text: String, targetSet: Set[String], values: Seq[(String, JsValue)]) {
        val allValues = Seq(
            "kind" -> JsString(kind),
            "user" -> JsString(user),
            "message" -> JsString(text),
            "members" -> JsArray(members.toList.map(JsString)),
            "targetSet" -> JsArray(targetSet.toList.map(JsString))) union values
        val msg = JsObject(allValues)
        chatChannel.push(msg)
    }

    def notify(kind: String, user: String, text: String, targetSet: Set[String]) { notify(kind, user, text, targetSet, Seq[(String, JsValue)]()) }

    def notifyAll(kind: String, user: String, text: String, values: Seq[(String, JsValue)]) { notify(kind, user, text, members, values) }

    def notifyAll(kind: String, user: String, text: String) { notify(kind, user, text, members) }
}

case class Join(username: String)
case class Quit(username: String)
case class Talk(username: String, text: String)
case class Whisper(username: String, target: String, text: String)
case class NotifyJoin(username: String)
case class SystemAll(username: String, text: String, values: Seq[(String, JsValue)])
case class System(username: String, target: String, text: String, values: Seq[(String, JsValue)])
case class Kick(username: String, target: String)

case class Connected(enumerator: Enumerator[JsValue])
case class CannotConnect(msg: String)
