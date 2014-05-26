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
  
  def isMerlin(username: String):Boolean = { username == Merlin }
  def isPercival(username: String):Boolean = { username == Percival }
  def isAssassin(username: String):Boolean = { username == Assassin }
  def isEvil(username: String):Boolean = { Evils.contains(username) }
}