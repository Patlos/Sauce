package model

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import scala.language.postfixOps

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Account(
  firstName: String, 
  lastName: String, 
  userId: String,
  providerId: String, 
  email: Option[String],
  avatarUrl: Option[String],
  userName: Pk[String] = NotAssigned
)

case class SignIn(userName: String, email: Option[String])

object Account {
  implicit object PkFormat extends Format[Pk[String]] {
      def reads(json: JsValue): JsResult[Pk[String]] = JsSuccess (
          json.asOpt[String].map(id => Id(id)).getOrElse(NotAssigned)
      )
      def writes(username: Pk[String]): JsValue = username.map(JsString(_)).getOrElse(JsNull)
  }
  implicit val writer = Json.writes[Account]

  implicit val signUpReader = Json.reads[SignIn]

  def username(email: String) = email.takeWhile(_!='@')

  val simple = {
    get[String]("account.firstName") ~
    get[String]("account.lastName") ~
    get[String]("account.userId") ~
    get[String]("account.providerId") ~
    get[Option[String]]("account.email") ~
    get[Option[String]]("account.avatarUrl") ~
    get[Pk[String]]("account.userName") map {
      case      firstName ~ lastName ~ userId ~ providerId ~ email ~ avatarUrl ~ userName => 
        Account(firstName,  lastName,  userId,  providerId,  email,  avatarUrl,  userName)
    }
  }

  def exists(userName: String) = findUsername(userName).isEmpty

  def findUsername(userName: String): Option[Account] = {
    DB.withConnection { implicit connection =>
      SQL("select * from account where userName = {userName} limit 1;").
        on('userName -> userName).
        as(simple.singleOpt)
    }
  }

  def findProvider(userId: String, providerId: String ): Option[Account] = {
    DB.withConnection { implicit connection =>
      SQL("select * from account where userId = {userId} AND providerId = {providerId} limit 1;").
        on('userId -> userId,
            'providerId -> providerId).
        as(simple.singleOpt)
    }
  }

  def findAll: List[Account] = {
    DB.withConnection { implicit connection =>
      SQL("select * from account;").as(simple *)
    }
  }

  def insert(account: Account) = {
    println(account)
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into account values (
            {userName},
            {firstName},
            {lastName},
            {userId},
            {providerId},
            {email},
            {avatarUrl}
          )
        """
      ).on(
        'userName -> account.email,
        'firstName -> account.firstName,
        'lastName -> account.lastName,
        'userId -> account.userId,
        'providerId -> account.providerId,
        'email -> account.email,
        'avatarUrl -> account.avatarUrl
      ).executeUpdate()
    }
  }

  def delete(userName: String) = {
    DB.withConnection { implicit connection =>
      SQL("delete from account where userName = {userName}").
        on('userName -> userName).executeUpdate()
    }
  }
}
