package models

import scala.collection.mutable.MutableList

class GameHistory {
    val history: MutableList[Game] = MutableList[Game]()
    
	class Game {
	    var history:Option[QuestHistory] = None 
	    var gameObject: Option[GameObject] = None
	    var isAssassinSuccess: Option[Boolean] = None
	    
	    def isJusticeWon: Option[Boolean] = { history match { case Some(qh) => if(qh.getFailureCount > 2) Some(false) else isAssassinSuccess; case None => None } }
	}
}