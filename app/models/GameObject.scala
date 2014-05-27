package models

class GameObject(members:List[String]) {
  val forElection = scala.util.Random.shuffle(members)
  val players:List[String] = scala.util.Random.shuffle(forElection)
  val Merlin:String = forElection(0)
  val Percival:String = if(existPercival) forElection(1) else null
  val Assassin:String = forElection(2)
  val Evils:List[String] = if(members.length < 7) List(forElection(2), forElection(3))
      else if(members.length < 10) List(forElection(2), forElection(3), forElection(4))
      else List(forElection(2), forElection(3), forElection(4), forElection(5))
  var Lady:String = if(existLady) players.last else null
  var leaderCount = 0
  var Ladied:List[String] = if(existLady) List(Lady) else List()
  var elected:List[String] = List()
  var voteCount = 0
  var questCount = 0
  var blueWins = 0
  var redWins = 0
  
  val questMembersCount = Array(Array(), Array(), Array(), Array(), Array(), 
    Array(2, 3, 2, 3, 3), Array(2, 3, 4, 3, 4),
    Array(2, 3, 3, 4, 4), Array(3, 4, 4, 5, 5),
    Array(3, 4, 4, 5, 5), Array(3, 4, 4, 5, 5))
  
  def isMerlin(username: String):Boolean = { username == Merlin }
  def isPercival(username: String):Boolean = { username == Percival }
  def isAssassin(username: String):Boolean = { username == Assassin }
  def isEvil(username: String):Boolean = { Evils.contains(username) }
  def getJustices():List[String] = { players diff Evils }
  def existPercival:Boolean = { players.length > 5 }
  def existLady:Boolean = { players.length > 6 }
  def getQuestMembersCount:Int = { questMembersCount(players.length)(questCount) }
}