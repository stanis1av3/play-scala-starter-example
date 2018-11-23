package services

import java.sql.Timestamp
import java.util.Date

import javax.inject._
import models.{ExchangeRatesDTO, ExchangeResultDTO}
import play.api.libs.ws._
import repositories.{ExchangeResult, ExchangeResultRepository}

import scala.collection.GenSet
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

trait ExchangeRateService {
  def calculateRates(widget: ExchangeResultDTO): Future[Double]

  def getActualRates: Future[ExchangeRatesDTO]
}

@Singleton
class ExchangeRateServiceImpl @Inject()(ws: WSClient,
                                        resultRepository: ExchangeResultRepository) extends ExchangeRateService {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  val URL = "https://api.exchangeratesapi.io/latest"
  val complexRequest: WSRequest = ws.url(URL).addHttpHeaders("Accept" -> "application/json")

  override def calculateRates(resultDTO: ExchangeResultDTO): Future[Double] = {
    resultRepository.findAll().map(e => println(e))
    resultRepository.findByCurrencyPairActual((resultDTO.currencyTo, resultDTO.currencyFrom), new Timestamp(new Date().getTime))
      .map(f => f.head.rate * resultDTO.price).recoverWith {
      case _: Exception => getActualRates
        .filter(erd => GenSet(resultDTO.currencyFrom, resultDTO.currencyTo).subsetOf(erd.rates.keySet + erd.base))
        .map(exchangeRatesDTO =>
          (exchangeRatesDTO.rates.getOrElse(resultDTO.currencyTo.toUpperCase, 1.0),
            exchangeRatesDTO.rates.getOrElse(resultDTO.currencyFrom.toUpperCase, 1.0)))
        .map(f => f._1 /f._2)
        .map(res => {
          resultRepository.save(ExchangeResult.of(resultDTO, res))
          res * resultDTO.price
        })
    }
  }

  override def getActualRates: Future[ExchangeRatesDTO] = {
    complexRequest.get()
      .map(response => response.json.validate[ExchangeRatesDTO].getOrElse(throw new IllegalArgumentException("Response can't be casted")))
  }
}
