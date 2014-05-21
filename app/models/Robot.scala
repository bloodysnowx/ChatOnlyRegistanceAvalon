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

object Robot {
  var state:GameState = GameStartWaitingState;
  
  def receiveMessage(event: JsValue, chatRoom: ActorRef) {
    Logger("robot").info(event.toString)
    val message = (event \ "message").as[String].trim()
    val username = (event \ "user").as[String]
    val splittedMessage = message.split(" ")
    if(username.equals("Robot")) return
    if(message.startsWith("/")) {
      if(message.startsWith("/game start")) state = state.startGame(chatRoom)
      else if(message.startsWith("/lady ")) state = state.lady(username, splittedMessage(1), chatRoom)
      else if(message.startsWith("/elect ")) state = state.elect(username, splittedMessage(1), chatRoom)
      else if(message.startsWith("/vote ")) state = state.vote(username, splittedMessage(1), chatRoom)
      else if(message.startsWith("/quest ")) state = state.quest(username, splittedMessage(1), chatRoom)
      else if(message.startsWith("/assassin ")) state = state.assassin(username, splittedMessage(1), chatRoom)
      else if(message.startsWith("/status")) state = state.status(username, chatRoom)
      else if(message.startsWith("/help")) state = state.help(username, chatRoom)
      else state = state.help(username, chatRoom)
    }
  }
  
  def apply(chatRoom: ActorRef) {
    // Create an Iteratee that logs all messages to the console.
    val loggerIteratee = Iteratee.foreach[JsValue](event => receiveMessage(event, chatRoom))
    
    implicit val timeout = Timeout(1 second)
    // Make the robot join the room
    chatRoom ? (Join("Robot")) map {
      case Connected(robotChannel) => 
        // Apply this Enumerator on the logger.
        robotChannel |>> loggerIteratee
    }
    
    // Make the robot talk every 60 seconds
    Akka.system.scheduler.schedule(
      300 seconds,
      300 seconds,
      chatRoom,
      Talk("Robot", "I'm still alive")
    )
  }
  
  class GameState {
    def startGame(chatRoom: ActorRef):GameState = { this }
    def lady(username: String, target: String, chatRoom: ActorRef):GameState = { this }
    def elect(username: String, target: String, chatRoom: ActorRef):GameState = { this }
    def vote(username: String, decision: String, chatRoom: ActorRef):GameState = { this }
    def quest(username: String, decision: String, chatRoom: ActorRef):GameState = { this }
    def assassin(username: String, target: String, chatRoom: ActorRef):GameState = { this }
    def status(username: String, chatRoom: ActorRef):GameState = { this }
    def help(username: String, chatRoom: ActorRef):GameState = {
      var helpMessages:Seq[String] = Seq(
          "These commands are available:",
          "/game start           --- ゲームを開始します。",
          "/lady [name]          --- [name]を占います。正義陣営の場合は[blue]と、悪陣営の場合は[red]と出力されます。",
          "/elect [name]         --- [name]をメンバーとして選びます。現在選んでいるメンバー一覧が全員に通知されます。",
          "/elect reset          --- 選んだメンバーを初期化します。",
          "/vote [true/false]    --- リーダーが選んだメンバーに対して投票します。賛成の場合は true , 反対の場合は false を入力してください。",
          "/quest [true/false]   --- 任務結果を提出します。成功の場合は true , 失敗の場合は false を入力してください。",
          "/assasin [name]       --- [name]をマーリン候補として暗殺します。",
          "/status               --- 現在の状況を表示します。",
          "/help                 --- ヘルプメッセージを表示します。"
      )
      helpMessages.map(message => chatRoom ! Whisper("Robot", username, message))
      return this
    }
  }
  
  object GameStartWaitingState extends GameState {
    
  }
  
  object LadyWaitingState extends GameState {
    
  }
}