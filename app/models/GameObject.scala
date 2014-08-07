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
    val voted: MutableList[String] = MutableList[String]()
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
        if(!username.equals(Lady.getOrElse("")) || !players.contains(target) || Ladied.contains(target)) return None
        Lady = Option(target)
        Ladied += target
        if(Evils.contains(target)) Some(false)
        else Some(true)
    }
    def resetElection(username: String) { if(username.equals(getLeader)) elected.clear }
    def electMember(username: String, target: String): Option[Boolean] = {
        if(!username.equals(getLeader) || !players.contains(target) || elected.contains(target)) return None
        elected += target;
        if(elected.length < getQuestMembersCount) Option(false)
        else Option(true)
    }
}