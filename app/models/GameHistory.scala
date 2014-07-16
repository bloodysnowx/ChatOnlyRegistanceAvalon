package models

import scala.collection.mutable.MutableList

class GameHistory {
    val history: MutableList[Game] = MutableList[Game]()
    
    
	class Game {
	    var history:Option[QuestHistory] = None 
	}
}