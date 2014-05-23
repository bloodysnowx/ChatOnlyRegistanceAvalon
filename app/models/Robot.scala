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
  var players:List[String] = List()
  var Merlin:String = null
  var Percival:String = null
  var Assassin:String = null
  var Lady:String = null
  var Evils:List[String] = List()
  var leaderCount = 0
  var Ladied:List[String] = List()
  var elected:List[String] = List()
  var voteCount = 0
  var questCount = 0
  var blueWins = 0
  var redWins = 0
  
  val questMembersCount = Array(Array(), Array(), Array(), Array(), Array(), 
      Array(2, 3, 2, 3, 3),
      Array(2, 3, 4, 3, 4),
      Array(2, 3, 3, 4, 4),
      Array(3, 4, 4, 5, 5),
      Array(3, 4, 4, 5, 5),
      Array(3, 4, 4, 5, 5))
  
  def receiveMessage(event: JsValue, chatRoom: ActorRef) {
    Logger("robot").info(event.toString)
    val message = (event \ "message").as[String].trim()
    val username = (event \ "user").as[String]
    val members = (event \ "members").as[List[String]] filter ( member => member != "Robot" )
    val splittedMessage = message.split(" ")
    if(username.equals("Robot")) return
    if(message.startsWith("/")) {
      if(message.startsWith("/game start")) state = state.startGame(username, chatRoom, members)
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
    val helpMessages:Seq[String] = Seq(
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
    
    def enter(chatRoom: ActorRef):GameState = { this }
    def startGame(username: String, chatRoom: ActorRef, members: List[String]):GameState = {
      chatRoom ! Whisper("Robot", username, "Game is already started.")
      this
    }
    def lady(username: String, target: String, chatRoom: ActorRef):GameState = { this }
    def elect(username: String, target: String, chatRoom: ActorRef):GameState = { this }
    def vote(username: String, decision: String, chatRoom: ActorRef):GameState = { this }
    def quest(username: String, decision: String, chatRoom: ActorRef):GameState = { this }
    def assassin(username: String, target: String, chatRoom: ActorRef):GameState = { this }
    def status(username: String, chatRoom: ActorRef):GameState = {
      chatRoom ! Whisper("Robot", username, "current Lady is " + Lady)
      if(Ladied.length > 0) chatRoom ! Whisper("Robot", username, "Ladied are " + Ladied.mkString(", "))
      if(players.length > 0) chatRoom ! Whisper("Robot", username, "current Leader is " + getLeader + ", leaderCount is " + leaderCount)
      if(players.length > 0) chatRoom ! Whisper("Robot", username, "Leadar order is " + players.mkString(", "))
      if(elected.length > 0) chatRoom ! Whisper("Robot", username, "elected are " + elected.mkString(", "))
      chatRoom ! Whisper("Robot", username, "questCount is " + questCount)
      chatRoom ! Whisper("Robot", username, "voteCount is " + voteCount)
      chatRoom ! Whisper("Robot", username, "blueWins is " + blueWins + ", redWins is " + redWins)
      
      if(username == Merlin) whisperToMerlin(chatRoom)
      if(username == Percival) whisperToPercival(chatRoom)
      if(username == Assassin) whisperToAssassin(chatRoom)
      if(Evils.contains(username)) whisperToEvil(username, chatRoom)
      
      this
    }
    def help(username: String, chatRoom: ActorRef):GameState = {
      helpMessages.map(message => chatRoom ! Whisper("Robot", username, message))
      return this
    }
  }
  
  def whisperToMerlin(chatRoom: ActorRef) {
    chatRoom ! System("Robot", Merlin, "You are Merlin. Evils are " + Evils.mkString(", "), Seq("evils" -> JsArray(Evils.map(str => JsString(str))), "roll" -> JsString("Merlin")))
  }
  
  def whisperToPercival(chatRoom: ActorRef) {
    chatRoom ! System("Robot", Percival, "You are Percival. Merlin is " + Merlin, Seq("roll" -> JsString("Percival"), "merlin" -> JsString(Merlin)))
  }
  
  def whisperToAssassin(chatRoom: ActorRef) {
    chatRoom ! System("Robot", Assassin, "You are Assassin.", Seq("roll" -> JsString("Assassin")))
  }
  
  def whisperToEvil(username: String, chatRoom: ActorRef) {
    chatRoom ! System("Robot", username, "You are Evil. Evils are " + Evils.mkString(", "), Seq("evils" -> JsArray(Evils.map(str => JsString(str))), "roll" -> JsString("Evil")))
  }
  
  object GameStartWaitingState extends GameState {
    override def startGame(username: String, chatRoom: ActorRef, members: List[String]):GameState = {
      if(members.length < 5) { chatRoom ! Talk("Robot", "members.length must be greater than 4"); return this }
      if(members.length > 10) { chatRoom ! Talk("Robot", "members.length must be less than 11"); return this }
      chatRoom ! Talk("Robot", "Game will start now!")
      players = members
      setupGames(chatRoom)
      ElectWaitingState.enter(chatRoom)
    }
    
    def setupGames(chatRoom: ActorRef) {
      val forElection = scala.util.Random.shuffle(players)
      Assassin = forElection(2)
      
      if(players.length < 7) Evils = List(forElection(2), forElection(3))
      else if(players.length < 10) Evils = List(forElection(2), forElection(3), forElection(4))
      else Evils = List(forElection(2), forElection(3), forElection(4), forElection(5))
      (players diff Evils).map(blue => chatRoom ! System("Robot", blue, "You are Justice.", Seq("roll" -> JsString("Justice"))))
      Merlin = forElection(0)
      whisperToMerlin(chatRoom)
      if(players.length > 5) {
        Percival = forElection(1)
        whisperToPercival(chatRoom)
      }
      Evils.map(evil => whisperToEvil(evil, chatRoom))
      whisperToAssassin(chatRoom)
      
      players = scala.util.Random.shuffle(players)
      if(players.length > 6) {
        Lady = players.last
        chatRoom ! Talk("Robot", "Lady is " + Lady)
        Ladied = Lady :: Ladied
      }
    }
  }
  
  object LadyWaitingState extends GameState {
    override def enter(chatRoom: ActorRef):GameState = {
      if(Lady == null) return ElectWaitingState.enter(chatRoom)
      chatRoom ! Talk("Robot", "Lady is " + Lady + ", " + helpMessages(2))
      this
    }
    
    override def lady(username: String, target: String, chatRoom: ActorRef):GameState = {
      if(!players.contains(target)) {
        chatRoom ! Talk("Robot", target + " does not exist.")
        return this
      }
      if(Ladied.contains(target)) {
        chatRoom ! Talk("Robot", target + " は既に泉の乙女所持者です")
        return this
      }
      
      chatRoom ! Talk("Robot", Lady + " は " + target + " の陣営を確認しました")
      chatRoom ! Whisper("Robot", Lady, target + " is " + (if (Evils.contains(target)) "red." else "blue."))
      Lady = target
      Ladied = target :: Ladied
      
      ElectWaitingState.enter(chatRoom)
    }
  }
  
  def getLeader:String = { players(leaderCount % players.length) }
  
  object ElectWaitingState extends GameState {
    override def enter(chatRoom: ActorRef):GameState = {
      chatRoom ! Talk("Robot", "Leadar order is " + players.mkString(", "))
      chatRoom ! Talk("Robot", "current Leadar is " + getLeader + "(" + voteCount + "), " + helpMessages(3))
      chatRoom ! Talk("Robot", "Please elect " + questMembersCount(players.length)(questCount) + " members")
      elected = List()
      this
    }
    override def elect(username: String, target: String, chatRoom: ActorRef):GameState = {
      if(username != getLeader) chatRoom ! Talk("Robot", username + " is not Leader.")
      else if(target == "reset") {
        elected = List()
        chatRoom ! Talk("Robot", "選出したメンバーを初期化します")
      } else if(!players.contains(target)) chatRoom ! Talk("Robot", target + " does not exist.")
      else if(elected.contains(target)) chatRoom ! Talk("Robot", target + " is already elected.")
      else {
        elected = target :: elected
        chatRoom ! Talk("Robot", "current Members are " + elected.mkString(", "))
        if(elected.length == questMembersCount(players.length)(questCount)) {
          return VoteWaitingState.enter(chatRoom)
        }
      }
      this
    }
  }
  
  object VoteWaitingState extends GameState {
    var voted = List[String]()
    var supports = List[String]()
    var oppositions = List[String]()
    
    override def enter(chatRoom: ActorRef):GameState = {
      voted = List[String]()
      supports = List[String]()
      oppositions = List[String]()
      voteCount = voteCount + 1
      leaderCount = leaderCount + 1
      chatRoom ! Talk("Robot", voteCount + "回目の投票を始めます。 " + helpMessages(5))
      if(voteCount == 5) QuestWaitingState.enter(chatRoom) else this
    }
    
    override def vote(username: String, decision: String, chatRoom: ActorRef):GameState = {
      if(voted.contains(username)) chatRoom ! Talk("Robot", username + " is already voted.")
      else {
        if(decision == "true") {
          voted = username :: voted
          supports = username :: supports
          chatRoom ! Talk("Robot", username + " is voted.")
        } else if(decision == "false") {
          voted = username :: voted
          oppositions = username :: oppositions
          chatRoom ! Talk("Robot", username + " is voted.")
        } else {
          chatRoom ! Talk("Robot", username + "'s selection is invalid.")
        }
        
        if(voted.length == players.length) {
          chatRoom ! Talk("Robot", "supports are " + supports.mkString(", "))
          chatRoom ! Talk("Robot", "poopsitions are " + oppositions.mkString(", "))
          if(supports.length > oppositions.length) {
            chatRoom ! Talk("Robot", "リーダーの選出が可決されました。")
            return QuestWaitingState.enter(chatRoom)
          } else {
            chatRoom ! Talk("Robot", "リーダーの選出が否決されました。")
            return ElectWaitingState.enter(chatRoom)
          }
        }
      }
      this
    }
  }
  
  object QuestWaitingState extends GameState {
    var success = 0
    var fail = 0
    var voted = List[String]()
    
    override def enter(chatRoom: ActorRef):GameState = {
      voteCount = 0
      voted = List[String]()
      success = 0
      fail = 0
      questCount = questCount + 1
      chatRoom ! Talk("Robot", "current Members are " + elected.mkString(", ") + ". " + helpMessages(6))
      this
    }
    
    override def quest(username: String, decision: String, chatRoom: ActorRef):GameState = {
      if(voted.contains(username)) chatRoom ! Talk("Robot", username + " is already voted.")
      else if(!elected.contains(username)) chatRoom ! Talk("Robot", username + " is not a quest member.")
      else {
        if(decision == "true") {
          voted = username :: voted
          success = success + 1
          chatRoom ! Talk("Robot", username + " is voted.")
        } else if(decision == "false") {
          voted = username :: voted
          fail = fail + 1
          chatRoom ! Talk("Robot", username + " is voted.")
        } else {
          chatRoom ! Talk("Robot", username + "'s selection is invalid.")
        }
        
        if(voted.length == elected.length) {
          if(fail == 0) {
            blueWins = blueWins + 1
            chatRoom ! Talk("Robot", "Quest is successed with " + fail + " fails.")
          } else if(fail == 1 && players.length > 6 && questCount == 4) {
        	blueWins = blueWins + 1
        	chatRoom ! Talk("Robot", "Quest is successed with " + fail + " fails.")
          } else {
            redWins = redWins + 1
            chatRoom ! Talk("Robot", "Quest is faild with " + fail + " fails.")
          }
          
          if(blueWins == 3) {
            chatRoom ! Talk("Robot", "Quest is won by Blue. " + blueWins + " / " + redWins)
            return AssassinateWaitingState.enter(chatRoom)
          } else if(redWins == 3) {
            chatRoom ! Talk("Robot", "Quest is won by Red. " + blueWins + " / " + redWins)
          } else if(questCount > 1) {
            return LadyWaitingState.enter(chatRoom)
          } else {
            return ElectWaitingState.enter(chatRoom)
          }
        }
      }
      this
    }
  }
  
  object AssassinateWaitingState extends GameState {
    override def enter(chatRoom: ActorRef):GameState = {
      chatRoom ! Talk("Robot", "Assassin is " + Assassin + ", " + helpMessages(7))
      this
    }
    
    override def assassin(username: String, target: String, chatRoom: ActorRef):GameState = {
      if(username != Assassin) chatRoom ! Talk("Robot", username + " is not Assassin.")
      else if(!players.contains(target)) chatRoom ! Talk("Robot", target + " does not exist.")
      else if(target == Merlin) {
        chatRoom ! Talk("Robot", username + " assassinated Merlin(" + target + ")!")
      } else {
        chatRoom ! Talk("Robot", username + " assassinated " + target + ". He is not Merlin.")
        chatRoom ! Talk("Robot", "Merlin is " + Merlin + ".")
      }
      this
    }
  }
}