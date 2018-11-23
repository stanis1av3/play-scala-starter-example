package repositories

import java.sql.Timestamp
import java.util.Date

import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.DatabasePublisher
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class ExchangeResultRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class ExchangeResultTable(tag: Tag) extends Table[ExchangeResult](tag, "exchange_result") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def currencyTo = column[String]("currency_to")

    def currencyFrom = column[String]("currency_from")

    def amount = column[Double]("amount")

    def rate = column[Double]("rate")

    def createdDate = column[Timestamp]("created_date")

    def * = (id, currencyTo, currencyFrom, amount, rate, createdDate) <> ((ExchangeResult.apply _).tupled, ExchangeResult.unapply)
  }

  private val exchangeResult = TableQuery[ExchangeResultTable]

  def save(result: ExchangeResult): Future[ExchangeResult] = save(
    result.currencyTo,
    result.currencyFrom,
    result.amount,
    result.rate,
    result.createdDate
  )

  def save(currencyTo: String, currencyFrom: String, amount: Double, rate: Double, createdDate: Timestamp): Future[ExchangeResult] = db.run {

    (exchangeResult.map(p => (p.currencyTo, p.currencyFrom, p.amount, p.rate, p.createdDate))
      returning exchangeResult.map(_.id)
      into ((data, id) => ExchangeResult(id, data._1, data._2, data._3, data._4, data._5))
      += (currencyFrom, currencyTo, amount, rate, createdDate))
  }

  def findAll(): Future[Seq[ExchangeResult]] = db.run {
    exchangeResult.result
  }

  def findAllAsDP(): DatabasePublisher[ExchangeResult] = db.stream {
    exchangeResult.result
  }


  def findByCurrencyPairActual(currencyPair: (String, String), dateFrom: Timestamp): Future[Seq[ExchangeResult]] = db.run {
    exchangeResult
      //.filter(_.createdDate<new Timestamp(new Date().getTime))
      .filter(_.currencyTo===currencyPair._2)
      .filter(_.currencyFrom===currencyPair._1)
      .result
  }
}
