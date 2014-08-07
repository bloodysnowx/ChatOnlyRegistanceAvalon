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
import scala.collection.mutable.MutableList

object Robot {
    var gameObject: GameObject = null
    var state: GameState = GameStartWaitingState;
    var gameOption: GameOptions = new GameOptions();

    def receiveMessage(event: JsValue, chatRoom: ActorRef) {
        Logger("robot").info(event.toString)
        val message = (event \ "message").as[String].trim()
        val username = (event \ "user").as[String]
        val members = (event \ "members").as[List[String]] filter (member => member != "Robot")
        val splittedMessage = message.split(" ")
        if (username.equals("Robot")) return
        if (message.startsWith("/")) {
            if (message.startsWith("/game start")) state = state.startGame(username, chatRoom, members)
            else if (message.startsWith("/game reset")) resetGame(chatRoom)
            else if (message.startsWith("/lady ")) state = state.lady(username, splittedMessage(1), chatRoom)
            else if (message.startsWith("/elect ")) state = state.elect(username, splittedMessage(1), chatRoom)
            else if (message.startsWith("/vote ")) state = state.vote(username, splittedMessage(1), chatRoom)
            else if (message.startsWith("/quest ")) state = state.quest(username, splittedMessage(1), chatRoom)
            else if (message.startsWith("/assassin ")) state = state.assassin(username, splittedMessage(1), chatRoom)
            else if (message.startsWith("/status")) state = state.status(username, chatRoom)
            else if (message.startsWith("/help")) state.help(username, chatRoom)
            else if (message.startsWith("/assassin ")) state = state.assassin(username, splittedMessage(1), chatRoom)
            else if (message.startsWith("/kick ")) kick(username, splittedMessage(1), chatRoom)
            else state.help(username, chatRoom)
        }
    }

    def kick(username: String, target: String, chatRoom: ActorRef) {
        if (target.equals("Robot")) return
        chatRoom ! Kick(username, target)
    }

    def resetGame(chatRoom: ActorRef) {
        gameObject = null
        state = GameStartWaitingState
        chatRoom ! Talk("Robot", "Reset Game.")
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
        Akka.system.scheduler.schedule(300 seconds, 300 seconds, chatRoom, Talk("Robot", "I'm still alive"))
    }

    class GameState {
        val helpMessages: Seq[String] = Seq(
            "These commands are available:",
            "/game start           --- ゲームを開始します。",
            "/lady [name]          --- [name]を占います。正義陣営の場合は[blue]と、悪陣営の場合は[red]と出力されます。",
            "/elect [name]         --- [name]をメンバーとして選びます。現在選んでいるメンバー一覧が全員に通知されます。",
            "/elect reset          --- 選んだメンバーを初期化します。",
            "/vote [true/false]    --- リーダーが選んだメンバーに対して投票します。賛成の場合は true , 反対の場合は false を入力してください。",
            "/quest [true/false]   --- 任務結果を提出します。成功の場合は true , 失敗の場合は false を入力してください。",
            "/assassin [name]      --- [name]をマーリン候補として暗殺します。",
            "/status               --- 現在の状況を表示します。",
            "/help                 --- ヘルプメッセージを表示します。", 
            "/kick [name]          --- [name]を追放します。")

        def enter(chatRoom: ActorRef): GameState = { this }
        def startGame(username: String, chatRoom: ActorRef, members: List[String]): GameState = {
            chatRoom ! Whisper("Robot", username, "Game is already started.")
            this
        }
        def lady(username: String, target: String, chatRoom: ActorRef): GameState = { this }
        def elect(username: String, target: String, chatRoom: ActorRef): GameState = { this }
        def vote(username: String, decision: String, chatRoom: ActorRef): GameState = { this }
        def quest(username: String, decision: String, chatRoom: ActorRef): GameState = { this }
        def assassin(username: String, target: String, chatRoom: ActorRef): GameState = { this }
        
        def sendData(chatRoom: ActorRef): Unit = {
            if (gameObject == null) return
            sendRoll(chatRoom)
            
            chatRoom ! SystemAll("Robot", "", Seq(
                    "lady" -> JsString(gameObject.getCurrentLady.getOrElse("")),
                    "ladied" -> JsArray(gameObject.getLadied.map(str => JsString(str))),
                    "leaderOrder" -> JsArray(Range(0, 5).map(x => JsString(gameObject.players((gameObject.leaderCount - gameObject.voteCount + x) % gameObject.players.length)))),
                    "elected" -> JsArray(gameObject.elected.map(str => JsString(str))),
                    "voted" -> JsArray(gameObject.getVoted.map(str => JsString(str))),
                    "players" -> JsArray(gameObject.players.map(str => JsString(str)))
                    ))
        }
        def sendRoll(chatRoom: ActorRef): Unit = {
            gameObject.getJustices.map(blue => chatRoom ! System("Robot", blue, "", Seq("roll" -> JsString("Justice"), "evils" -> JsArray())))
            chatRoom ! System("Robot", gameObject.Merlin, "", Seq("roll" -> JsString("Merlin"), "evils" -> JsArray(gameObject.Evils.map(str => JsString(str)))))
            gameObject.Percival match { case None => ; case Some(p) => chatRoom ! System("Robot", p, "" + gameObject.Merlin, Seq("roll" -> JsString("Percival(Merlin is " + gameObject.Merlin + ")"), "evils" -> JsArray())) }
            gameObject.Evils.map(red => chatRoom ! System("Robot", red, "", Seq("evils" -> JsArray(gameObject.Evils.map(str => JsString(str))), "roll" -> JsString("Evil"))))
            chatRoom ! System("Robot", gameObject.Assassin, "", Seq("roll" -> JsString("Assassin"))) 
        }
        def sendVoted(chatRoom: ActorRef): Unit = { chatRoom ! SystemAll("Robot", "", Seq("voted" -> JsArray(gameObject.getVoted.map(str => JsString(str))))) }
        
        def status(username: String, chatRoom: ActorRef): GameState = {
            if (gameObject == null) return this
            sendData(chatRoom)
            chatRoom ! Whisper("Robot", username, "questCount is " + gameObject.questCount)
            chatRoom ! Whisper("Robot", username, "voteCount is " + gameObject.voteCount)
            chatRoom ! Whisper("Robot", username, "blueWins is " + gameObject.blueWins + ", redWins is " + gameObject.redWins)

            this
        }
        def help(username: String, chatRoom: ActorRef): Unit = { helpMessages.map(message => chatRoom ! Whisper("Robot", username, message)) }
    }

    def talkLady(chatRoom: ActorRef) {
        gameObject.getCurrentLady match {
            case Some(l) => chatRoom ! SystemAll("Robot", "Lady is " + l, Seq("lady" -> JsString(l)))
            case None => ;
        }
    }

    object GameStartWaitingState extends GameState {
        override def startGame(username: String, chatRoom: ActorRef, members: List[String]): GameState = {
            if (members.length < 5) { chatRoom ! Talk("Robot", "members.length must be greater than 4"); return this }
            if (members.length > 10) { chatRoom ! Talk("Robot", "members.length must be less than 11"); return this }
            chatRoom ! Talk("Robot", "Game will start now!")
            setupGames(chatRoom, members)
            ElectWaitingState.enter(chatRoom)
        }

        def setupGames(chatRoom: ActorRef, members: List[String]) {
            gameObject = new GameObject(members)
            sendData(chatRoom)
        }
    }

    object LadyWaitingState extends GameState {
        override def enter(chatRoom: ActorRef): GameState = {
            return gameObject.getCurrentLady match {
                case Some(l) => talkLady(chatRoom); this;
                case None => ElectWaitingState.enter(chatRoom)
            }
        }

        override def lady(username: String, target: String, chatRoom: ActorRef): GameState = {
            gameObject.forecastLady(username, target) match {
                case Some(l) => { 
                    Whisper("Robot", username, target + " is " + (if(l) "blue." else "red."))
                    chatRoom ! SystemAll("Robot", username + " は " + target + " の陣営を確認しました", Seq("lady" -> JsString(target), "ladied" -> JsArray(gameObject.getLadied.map(str => JsString(str)))))
                    ElectWaitingState.enter(chatRoom)
                }
                case None => { return this }
            }
        }
    }

    def talkCurrentLeader(chatRoom: ActorRef) { chatRoom ! SystemAll("Robot", "現在のリーダーは " + (gameObject.voteCount + 1) + " 番目の " + gameObject.getLeader + " さんです。", Seq("leaderOrder" -> JsArray(Range(0, 5).map(x => JsString(gameObject.players((gameObject.leaderCount - gameObject.voteCount + x) % gameObject.players.length)))), "leader" -> JsString(gameObject.getLeader))) }

    def talkCurrentMembers(chatRoom: ActorRef) { chatRoom ! SystemAll("Robot", "current Members are " + gameObject.elected.mkString(", "), Seq("elected" -> JsArray(gameObject.elected.map(str => JsString(str))))) }

    object ElectWaitingState extends GameState {
        override def enter(chatRoom: ActorRef): GameState = {
            talkCurrentLeader(chatRoom)
            chatRoom ! SystemAll("Robot", "Please elect " + gameObject.getQuestMembersCount + " members", Seq("elected" -> JsArray()))
            gameObject.elected.clear
            this
        }
        override def elect(username: String, target: String, chatRoom: ActorRef): GameState = {
            if(target == "reset") gameObject.resetElection(username)
            gameObject.electMember(username, target) match {
                case Some(true) => { return VoteWaitingState.enter(chatRoom) }
                case Some(false) => { }
                case None => { chatRoom ! Talk("Robot", "error!") }
            }
            talkCurrentMembers(chatRoom)
            this
        }
    }

    object VoteWaitingState extends GameState {
        override def enter(chatRoom: ActorRef): GameState = {
            gameObject.startNewVote
            gameObject.voteCount = gameObject.voteCount + 1
            gameObject.leaderCount = gameObject.leaderCount + 1
            chatRoom ! SystemAll("Robot", gameObject.voteCount + "回目の投票を始めます。 " + helpMessages(5), Seq("voted" -> JsArray()))
            if (gameObject.voteCount == 5) QuestWaitingState.enter(chatRoom) else this
        }

        override def vote(username: String, decisionStr: String, chatRoom: ActorRef): GameState = {
            if (gameObject.getVoted.contains(username)) chatRoom ! Talk("Robot", username + " is already voted.")
            var decision: Option[Boolean] = decisionStr match {
                case "true" => Option(true)
                case "false" => Option(false)
                case _ => None
            }
            if (decision == None) { chatRoom ! Talk("Robot", "error!"); return this; }
            
            gameObject.vote(username, decision.get) match {
                case None => chatRoom ! Talk("Robot", "error!") 
                case Some(false) => sendVoted(chatRoom)
                case Some(true) => {  
                    chatRoom ! Talk("Robot", "賛成 : " + gameObject.supports.mkString(", "))
                    chatRoom ! Talk("Robot", "反対 : " + gameObject.oppositions.mkString(", "))
                    chatRoom ! Talk("Robot", "リーダーの選出が" + (if(gameObject.isVotePassed) "可決" else "否決") + "されました。")
                    return if(gameObject.isVotePassed) QuestWaitingState.enter(chatRoom) else ElectWaitingState.enter(chatRoom)
                }
            }
            this
        }
    }

    object QuestWaitingState extends GameState {
        var success = 0
        var fail = 0
        var voted = List[String]()

        override def enter(chatRoom: ActorRef): GameState = {
            gameObject.voteCount = 0
            voted = List[String]()
            success = 0
            fail = 0
            gameObject.questCount = gameObject.questCount + 1
            talkCurrentMembers(chatRoom)
            this
        }

        override def quest(username: String, decision: String, chatRoom: ActorRef): GameState = {
            if (voted.contains(username)) chatRoom ! Talk("Robot", username + " is already voted.")
            else if (!gameObject.elected.contains(username)) chatRoom ! Talk("Robot", username + " is not a quest member.")
            else {
                if (decision == "true") {
                    voted = voted :+ username
                    success = success + 1
                    chatRoom ! Talk("Robot", username + " is voted.")
                } else if (decision == "false") {
                    voted = voted :+ username
                    fail = fail + 1
                    chatRoom ! Talk("Robot", username + " is voted.")
                } else chatRoom ! Talk("Robot", username + "'s selection is invalid.")

                if (voted.length == gameObject.elected.length) {
                    if (fail == 0) {
                        gameObject.blueWins = gameObject.blueWins + 1
                        chatRoom ! Talk("Robot", "Quest is successed with " + fail + " fails.")
                    } else if (fail == 1 && gameObject.players.length > 6 && gameObject.questCount == 4) {
                        gameObject.blueWins = gameObject.blueWins + 1
                        chatRoom ! Talk("Robot", "Quest is successed with " + fail + " fails.")
                    } else {
                        gameObject.redWins = gameObject.redWins + 1
                        chatRoom ! Talk("Robot", "Quest is faild with " + fail + " fails.")
                    }

                    if (gameObject.blueWins == 3) {
                        chatRoom ! Talk("Robot", "Quest is won by Blue. " + gameObject.blueWins + " / " + gameObject.redWins)
                        return AssassinateWaitingState.enter(chatRoom)
                    } else if (gameObject.redWins == 3) chatRoom ! Talk("Robot", "Quest is won by Red. " + gameObject.blueWins + " / " + gameObject.redWins)
                    else if (gameObject.questCount > 1) return LadyWaitingState.enter(chatRoom) 
                    else return ElectWaitingState.enter(chatRoom)
                }
            }
            this
        }
    }

    object AssassinateWaitingState extends GameState {
        override def enter(chatRoom: ActorRef): GameState = {
            chatRoom ! Talk("Robot", "Assassin is " + gameObject.Assassin + ", " + helpMessages(7))
            this
        }

        override def assassin(username: String, target: String, chatRoom: ActorRef): GameState = {
            chatRoom ! Talk("Robot", 
                    if (username != gameObject.Assassin) username + " is not Assassin."
                    else if (!gameObject.players.contains(target)) target + " does not exist."
                    else if (target == gameObject.Merlin) username + " assassinated Merlin(" + target + ")!"
                    else username + " assassinated " + target + ". He is not Merlin. Merlin is " + gameObject.Merlin + ".")
            this
        }
    }
}