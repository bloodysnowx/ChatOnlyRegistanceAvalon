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
            else if (message.startsWith("/help")) state = state.help(username, chatRoom)
            else if (message.startsWith("/assassin ")) state = state.assassin(username, splittedMessage(1), chatRoom)
            else if (message.startsWith("/kick ")) kick(username, splittedMessage(1), chatRoom)
            else state = state.help(username, chatRoom)
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
        def status(username: String, chatRoom: ActorRef): GameState = {
            if (gameObject == null) return this
            talkLady(chatRoom)
            if (gameObject.Ladied.length > 0) chatRoom ! System("Robot", username, "Ladied are " + gameObject.Ladied.mkString(", "), Seq("lady" -> JsString(gameObject.Lady.getOrElse("")), "ladied" -> JsArray(gameObject.Ladied.map(str => JsString(str)))))
            if (gameObject.players.length > 0) talkCurrentLeader(chatRoom)
            if (gameObject.players.length > 0) talkLeaderOrder(chatRoom)
            if (gameObject.elected.length > 0) talkCurrentMembers(chatRoom)
            chatRoom ! Whisper("Robot", username, "questCount is " + gameObject.questCount)
            chatRoom ! Whisper("Robot", username, "voteCount is " + gameObject.voteCount)
            chatRoom ! Whisper("Robot", username, "blueWins is " + gameObject.blueWins + ", redWins is " + gameObject.redWins)

            if (gameObject.isMerlin(username)) whisperToMerlin(chatRoom)
            if (gameObject.isPercival(username)) whisperToPercival(chatRoom)
            if (gameObject.isAssassin(username)) whisperToAssassin(chatRoom)
            if (gameObject.isEvil(username)) whisperToEvil(username, chatRoom)

            this
        }
        def help(username: String, chatRoom: ActorRef): GameState = {
            helpMessages.map(message => chatRoom ! Whisper("Robot", username, message))
            return this
        }
    }

    def whisperToMerlin(chatRoom: ActorRef) {
        chatRoom ! System("Robot", gameObject.Merlin, "You are Merlin. Evils are " + gameObject.Evils.mkString(", "), Seq("evils" -> JsArray(gameObject.Evils.map(str => JsString(str))), "roll" -> JsString("Merlin")))
    }

    def whisperToPercival(chatRoom: ActorRef) {
        gameObject.Percival match {
            case Some(p) => chatRoom ! System("Robot", p, "You are Percival. Merlin is " + gameObject.Merlin, Seq("roll" -> JsString("Percival(Merlin is " + gameObject.Merlin + ")")))
            case None => ;
        }
    }

    def whisperToAssassin(chatRoom: ActorRef) {
        chatRoom ! System("Robot", gameObject.Assassin, "You are Assassin.", Seq("roll" -> JsString("Assassin")))
    }

    def whisperToEvil(username: String, chatRoom: ActorRef) {
        chatRoom ! System("Robot", username, "You are Evil. Evils are " + gameObject.Evils.mkString(", "), Seq("evils" -> JsArray(gameObject.Evils.map(str => JsString(str))), "roll" -> JsString("Evil")))
    }

    def talkLady(chatRoom: ActorRef) {
        gameObject.Lady match {
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

            gameObject.getJustices.map(blue => chatRoom ! System("Robot", blue, "You are Justice.", Seq("roll" -> JsString("Justice"), "evils" -> JsArray())))
            whisperToMerlin(chatRoom)
            if (gameObject.existPercival) whisperToPercival(chatRoom)
            gameObject.Evils.map(evil => whisperToEvil(evil, chatRoom))
            whisperToAssassin(chatRoom)
            if (gameObject.existLady) talkLady(chatRoom)
        }
    }

    object LadyWaitingState extends GameState {
        override def enter(chatRoom: ActorRef): GameState = {
            return gameObject.Lady match {
                case Some(l) => talkLady(chatRoom); this;
                case None => ElectWaitingState.enter(chatRoom)
            }
        }

        override def lady(username: String, target: String, chatRoom: ActorRef): GameState = {
            if (!gameObject.players.contains(target)) {
                chatRoom ! Talk("Robot", target + " does not exist.")
                return this
            }
            if (gameObject.Ladied.contains(target)) {
                chatRoom ! Talk("Robot", target + " は既に泉の乙女所持者です")
                return this
            }

            gameObject.Lady match {
                case Some(l) => {
                    chatRoom ! Whisper("Robot", l, target + " is " + (if (gameObject.Evils.contains(target)) "red." else "blue."))
                    gameObject.Lady = Option(target)
                    gameObject.Ladied += target
                    chatRoom ! SystemAll("Robot", l + " は " + target + " の陣営を確認しました", Seq("lady" -> JsString(l), "ladied" -> JsArray(gameObject.Ladied.map(str => JsString(str)))))
                }
                case None => ;
            }

            ElectWaitingState.enter(chatRoom)
        }
    }

    def getLeader: String = { gameObject.players(gameObject.leaderCount % gameObject.players.length) }

    def talkCurrentLeader(chatRoom: ActorRef) {
        chatRoom ! SystemAll("Robot", "現在のリーダーは " + (gameObject.voteCount + 1) + " 番目の " + getLeader + " さんです。", Seq("leader" -> JsString(getLeader)))
    }

    def talkLeaderOrder(chatRoom: ActorRef) {
        chatRoom ! SystemAll("Robot", "Leadar order is " + gameObject.players.mkString(", "), Seq("leader" -> JsString(getLeader),
            "players" -> JsArray(gameObject.players.map(str => JsString(str))),
            "leaderOrder" -> JsArray(Range(0, 5).map(x => JsString(gameObject.players((gameObject.leaderCount - gameObject.voteCount + x) % gameObject.players.length))))))
    }

    def talkCurrentMembers(chatRoom: ActorRef) {
        chatRoom ! SystemAll("Robot", "current Members are " + gameObject.elected.mkString(", "), Seq("elected" -> JsArray(gameObject.elected.map(str => JsString(str)))))
    }

    object ElectWaitingState extends GameState {
        override def enter(chatRoom: ActorRef): GameState = {
            talkLeaderOrder(chatRoom)
            talkCurrentLeader(chatRoom)
            chatRoom ! SystemAll("Robot", "Please elect " + gameObject.getQuestMembersCount + " members", Seq("elected" -> JsArray()))
            gameObject.elected.clear
            this
        }
        override def elect(username: String, target: String, chatRoom: ActorRef): GameState = {
            if (username != getLeader) chatRoom ! Talk("Robot", username + " is not Leader.")
            else if (target == "reset") {
                gameObject.elected.clear
                chatRoom ! Talk("Robot", "選出したメンバーを初期化します")
                talkCurrentMembers(chatRoom)
            } else if (!gameObject.players.contains(target)) chatRoom ! Talk("Robot", target + " does not exist.")
            else if (gameObject.elected.contains(target)) chatRoom ! Talk("Robot", target + " is already elected.")
            else {
                gameObject.elected += target
                talkCurrentMembers(chatRoom)
                if (gameObject.elected.length == gameObject.getQuestMembersCount) {
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

        override def enter(chatRoom: ActorRef): GameState = {
            voted = List[String]()
            supports = List[String]()
            oppositions = List[String]()
            gameObject.voteCount = gameObject.voteCount + 1
            gameObject.leaderCount = gameObject.leaderCount + 1
            chatRoom ! SystemAll("Robot", gameObject.voteCount + "回目の投票を始めます。 " + helpMessages(5), Seq("voted" -> JsArray()))
            if (gameObject.voteCount == 5) QuestWaitingState.enter(chatRoom) else this
        }

        override def vote(username: String, decision: String, chatRoom: ActorRef): GameState = {
            if (voted.contains(username)) chatRoom ! Talk("Robot", username + " is already voted.")
            else {
                if (decision == "true") {
                    voted = voted :+ username
                    supports = supports :+ username
                    chatRoom ! SystemAll("Robot", "", Seq("voted" -> JsArray(voted.map(str => JsString(str)))))
                } else if (decision == "false") {
                    voted = voted :+ username
                    oppositions = oppositions :+ username
                    chatRoom ! SystemAll("Robot", "", Seq("voted" -> JsArray(voted.map(str => JsString(str)))))
                } else {
                    chatRoom ! Talk("Robot", username + "'s selection is invalid.")
                }

                if (voted.length == gameObject.players.length) {
                    chatRoom ! Talk("Robot", "賛成 : " + supports.mkString(", "))
                    chatRoom ! Talk("Robot", "反対 : " + oppositions.mkString(", "))
                    if (supports.length > oppositions.length) {
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
            if (username != gameObject.Assassin) chatRoom ! Talk("Robot", username + " is not Assassin.")
            else if (!gameObject.players.contains(target)) chatRoom ! Talk("Robot", target + " does not exist.")
            else if (target == gameObject.Merlin) chatRoom ! Talk("Robot", username + " assassinated Merlin(" + target + ")!")
            else {
                chatRoom ! Talk("Robot", username + " assassinated " + target + ". He is not Merlin.")
                chatRoom ! Talk("Robot", "Merlin is " + gameObject.Merlin + ".")
            }
            this
        }
    }
}