package services

import com.typesafe.config.Config
import javax.inject._
import models.{ExchangeRatesDTO, ExchangeResultDTO}
import org.joda.time.Duration
import play.api.libs.ws._
import repositories.ExchangeResultRepository

import scala.collection.GenSet
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExchangeResultService @Inject()(ws: WSClient,
                                      resultRepository: ExchangeResultRepository,
                                      config: Config)(implicit val ec: ExecutionContext) {

  val URL = config.getString("rate.source.url")
  val CACHE_TTL = new Duration(config.getLong("cache.ttl.ms"))

  def calculateRates(resultDTO: ExchangeResultDTO): Future[Double] = {

    calculateRatesFromInternalSource(resultDTO)
      .recoverWith {
        case _: UnsupportedOperationException => calculateRatesFromExternalSource(resultDTO)
        case e: Exception => throw e
      }
  }

  def calculateRatesFromInternalSource(resultDTO: ExchangeResultDTO): Future[Double] = {
    resultRepository.findByCurrencyPairActual((resultDTO.currencyTo, resultDTO.currencyFrom), CACHE_TTL)
      .map(f => f.head.rate * resultDTO.price)
  }

  def calculateRatesFromExternalSource(resultDTO: ExchangeResultDTO): Future[Double] = {
    getActualRates
      .filter(erd => GenSet(resultDTO.currencyFrom, resultDTO.currencyTo).subsetOf(erd.rates.keySet + erd.base))
      .map(exchangeRatesDTO =>
        (exchangeRatesDTO.rates.getOrElse(resultDTO.currencyTo.toUpperCase, 1.0),
          exchangeRatesDTO.rates.getOrElse(resultDTO.currencyFrom.toUpperCase, 1.0)))
      .map(f => f._1 / f._2)
      .map(res => {
        resultRepository.save(resultDTO -> res)
        res * resultDTO.price
      })
  }

  def getActualRates(implicit sourceUrl:String = URL): Future[ExchangeRatesDTO] = {
    val complexRequest: WSRequest = ws.url(sourceUrl).addHttpHeaders("Accept" -> "application/json")
    complexRequest.get().map(response =>
      response.json.validate[ExchangeRatesDTO]
        .getOrElse(throw new IllegalArgumentException("Response can't be casted")))
  }
}
