package models

class GameObject {
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
  
  def setupGames(members:List[String]) {
    players = members
    val forElection = scala.util.Random.shuffle(players)
    Assassin = forElection(2)
    Evils = 
      if(players.length < 7) List(forElection(2), forElection(3))
      else if(players.length < 10) List(forElection(2), forElection(3), forElection(4))
      else List(forElection(2), forElection(3), forElection(4), forElection(5))
    
    Merlin = forElection(0)
    if(existPercival) Percival = forElection(1)
      
    players = scala.util.Random.shuffle(players)
    if(existLady) {
      Lady = players.last
      Ladied = Lady :: Ladied
    }
  }
}