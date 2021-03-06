package models

import scala.collection.mutable.MutableList

class GameObject(members: List[String]) {
    scala.util.Random.setSeed((new java.util.Date()).getTime());
    private val forElection = scala.util.Random.shuffle(members)
    val players: List[String] = scala.util.Random.shuffle(forElection)
    val Merlin: String = forElection(0)
    val Percival: Option[String] = if (existPercival) Some(forElection(1)) else None
    val Assassin: String = forElection(2)
    val Evils: List[String] = if (members.length < 7) List(forElection(2), forElection(3))
    else if (members.length < 10) List(forElection(2), forElection(3), forElection(4))
    else List(forElection(2), forElection(3), forElection(4), forElection(5))
    private var Lady: Option[String] = if (existLady) Some(players.last) else None
    var leaderCount = 0
    private val Ladied: MutableList[String] = Lady match { case Some(l) => MutableList(l); case None => MutableList() }
    val elected: MutableList[String] = MutableList[String]()
    val supports = MutableList[String]()
    val oppositions = MutableList[String]()
    val successList = MutableList[String]()
    val failList = MutableList[String]()
    var voteCount = 0
    var questCount = 0
    var blueWins = 0
    var redWins = 0

    val questMembersCount = Array(Array(), Array(), Array(), Array(), Array(),
        Array(2, 3, 2, 3, 3), Array(2, 3, 4, 3, 4),
        Array(2, 3, 3, 4, 4), Array(3, 4, 4, 5, 5),
        Array(3, 4, 4, 5, 5), Array(3, 4, 4, 5, 5))

    def isMerlin(username: String): Boolean = { username == Merlin }
    def isPercival(username: String): Boolean = { username == Percival }
    def isAssassin(username: String): Boolean = { username == Assassin }
    def isEvil(username: String): Boolean = { Evils.contains(username) }
    def isJustice(username: String): Boolean = { !isEvil(username) }
    def getJustices(): List[String] = { players diff Evils }
    def existPercival: Boolean = { players.length > 6 }
    def existLady: Boolean = { players.length > 6 }
    def getQuestMembersCount: Int = { questMembersCount(players.length)(questCount) }
    def getCurrentLady() = { Lady }
    def getLadied() = { Ladied }
    def getLeader: String = { players(leaderCount % players.length) }
    def forecastLady(username: String, target: String): Option[Boolean] = {
        if(username != Lady.getOrElse("") || !players.contains(target) || Ladied.contains(target)) return None
        Lady = Option(target)
        Ladied += target
        if(Evils.contains(target)) Some(false)
        else Some(true)
    }
    def resetElection(username: String) { if(username.equals(getLeader)) elected.clear }
    def startNewVote() { supports.clear(); oppositions.clear() }
    def electMember(username: String, target: String): Option[Boolean] = {
        if(username != getLeader || !players.contains(target) || elected.contains(target)) return None
        elected += target;
        if(elected.length < getQuestMembersCount) Option(false)
        else Option(true)
    }
    def getVoted: List[String] = { List.concat(supports, oppositions) }
    def vote(username: String, decision: Boolean): Option[Boolean] = {
        if(getVoted.contains(username)) return None
        (if(decision) supports else oppositions) += username
        Option(getVoted.length == players.length)
    }
    def isVotePassed : Boolean = { supports.length > oppositions.length }
    def startNewQuest() { successList.clear; failList.clear }
    def getQuestVoted(): List[String] = { List.concat(successList, failList) }
    
    def quest(username: String, decision: Boolean): Option[Boolean] = {
        if(getQuestVoted.contains(username) || !elected.contains(username)) return None
        (if(decision) successList else failList) += username
        return Option(getQuestVoted.length == elected.length)
    }
    def isQuestSuccess : Boolean = { failList.length == 0 || (failList.length == 1 && players.length > 6 && questCount == 4) }
    def processQuest { if(isQuestSuccess) blueWins += 1 else redWins += 1 }
    def isBlueWinTheQuests : Option[Boolean] = { if(blueWins == 3) Option(true) else if(redWins == 3) Option(false) else None }
}