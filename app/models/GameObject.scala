package models

import scala.collection.mutable.MutableList

class GameObject(members: List[String]) {
    val forElection = scala.util.Random.shuffle(members)
    val players: List[String] = scala.util.Random.shuffle(forElection)
    val Merlin: String = forElection(0)
    val Percival: Option[String] = if (existPercival) Some(forElection(1)) else None
    val Assassin: String = forElection(2)
    val Evils: List[String] = if (members.length < 7) List(forElection(2), forElection(3))
    else if (members.length < 10) List(forElection(2), forElection(3), forElection(4))
    else List(forElection(2), forElection(3), forElection(4), forElection(5))
    var Lady: Option[String] = if (existLady) Some(players.last) else None
    var leaderCount = 0
    val Ladied: MutableList[String] = Lady match { case Some(l) => MutableList(l); case None => MutableList() }
    val elected: MutableList[String] = MutableList[String]()
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
    def getJustices(): List[String] = { players diff Evils }
    def existPercival: Boolean = { players.length > 6 }
    def existLady: Boolean = { players.length > 6 }
    def getQuestMembersCount: Int = { questMembersCount(players.length)(questCount) }
}