package models

import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import Q.interpolation

object alltables {
  val speakers = new Speakers
  val talks = new Talks
}

import models.alltables._


case class Speaker(name: String, bio: String, id: Option[Long] = None) {
  def submittedTalks = {
    talks.findTalks(this)
  }
}


class Speakers extends Table[Speaker]("SPEAKERS") {
  def id =   column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def bio = column[String]("bio", O.NotNull)
  def * = name ~ bio ~ id.? <> (Speaker.apply _, Speaker.unapply _)
  def autoInc = * returning id

  def insert(speaker: Speaker) = DB.withSession { implicit session =>
      autoInc.insert(speaker)
  }
}
object Speakers {
  def findAll: List[Speaker] = DB.withSession {implicit session =>
    Query(new Speakers).list
  }

  def deleteAll = DB.withSession { implicit session =>
    sqlu"delete from speakers"
  }
}

case class Talk(description: String, speakerId: Long, id: Option[Long] = None)

class Talks extends Table[Talk]("TALKS") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def description = column[String]("description", O.NotNull)
  def speakerId = column[Long]("speakerId")

  def speaker = foreignKey("talks_speaker_fk", speakerId, speakers)(_.id)

  def * = description ~  speakerId ~ id.? <> (Talk.apply _, Talk.unapply _)

  def autoInc = * returning id

  def insert(t: Talk) = {
    DB.withSession { implicit session =>
      talks.autoInc.insert(t)
    }
  }


  def findTalks(s: Speaker): List[Talk] = {
    DB.withSession { implicit session =>

      val q = for(t <- talks if t.speakerId === s.id.get) yield t
      q.list()
    }

  }
}
