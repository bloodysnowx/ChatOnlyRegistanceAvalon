package models

import scala.collection.mutable.MutableList

class QuestHistory {
    val history: MutableList[Quest] = MutableList[Quest]()
    
    class Quest {
        val history: MutableList[Elect] = MutableList[Elect]()
        var leader: Option[String] = None
        val members: MutableList[String] = MutableList[String]()
        var isSuccess: Option[Boolean] = None
        val success: MutableList[String] = MutableList[String]()
        val failure: MutableList[String] = MutableList[String]()
        var failCount: Option[Int] = None
        var lady: Option[String] = None
        var ladied: Option[String] = None
        var excalibar: Option[String] = None
        var excalibared: Option[String] = None
        
        class Elect {
            var leader: Option[String] = None
            val elected: MutableList[String] = MutableList[String]()
            val supports: MutableList[String] = MutableList[String]()
            val oppositions: MutableList[String] = MutableList[String]()
            var excalibar: Option[String] = None
        }
    }
}