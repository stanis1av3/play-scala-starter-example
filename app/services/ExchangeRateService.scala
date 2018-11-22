package services

import javax.inject._
import models.{ExchangeRatesDTO, ExchangeResultDTO}
import play.api.libs.ws._

import scala.collection.GenSet
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

trait ExchangeRateService {
  def calculateRates(widget: ExchangeResultDTO): Future[Double]

  def getActualRates: Future[ExchangeRatesDTO]
}

@Singleton
class ExchangeRateServiceImpl @Inject()(ws: WSClient) extends ExchangeRateService {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  val URL = "https://api.exchangeratesapi.io/latest"
  val complexRequest: WSRequest = ws.url(URL).addHttpHeaders("Accept" -> "application/json")

  override def calculateRates(resultDTO: ExchangeResultDTO): Future[Double] = {

    complexRequest.get()
      .map(response => response.json.as[ExchangeRatesDTO])
      .filter(erd => GenSet(resultDTO.currencyFrom, resultDTO.currencyTo).subsetOf(erd.rates.keySet.+(erd.base)))
      .map(exchangeRatesDTO =>
        (exchangeRatesDTO.rates.getOrElse(resultDTO.currencyTo.toUpperCase, 1.0),
          exchangeRatesDTO.rates.getOrElse(resultDTO.currencyFrom.toUpperCase, 1.0)))
      .map(f => (f._1 / f._2) * resultDTO.price)
  }

  override def getActualRates: Future[ExchangeRatesDTO] = {
    complexRequest.get()
      .map(response => response.json.as[ExchangeRatesDTO])
  }
}
