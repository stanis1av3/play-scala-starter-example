package services

import com.typesafe.config.Config
import models.entities.ExchangeResult
import models.{ExchangeRatesDTO, ExchangeResultDTO}
import javax.inject._
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

  val complexRequest: WSRequest = ws.url(URL).addHttpHeaders("Accept" -> "application/json")

  def calculateRates(resultDTO: ExchangeResultDTO): Future[Double] = {
    resultRepository.findByCurrencyPairActual((resultDTO.currencyTo, resultDTO.currencyFrom), CACHE_TTL)
      .map(f => f.head.rate * resultDTO.price)
      .recoverWith {
        case _: Exception => getActualRates
          .filter(erd => GenSet(resultDTO.currencyFrom, resultDTO.currencyTo).subsetOf(erd.rates.keySet + erd.base))
          .map(exchangeRatesDTO =>
            (exchangeRatesDTO.rates.getOrElse(resultDTO.currencyTo.toUpperCase, 1.0),
              exchangeRatesDTO.rates.getOrElse(resultDTO.currencyFrom.toUpperCase, 1.0)))
          .map(f => f._1 / f._2)
          .map(res => {
            resultRepository.save(ExchangeResult.of(resultDTO, res))
            res * resultDTO.price
          })
      }
  }

  def getActualRates: Future[ExchangeRatesDTO] = {
    complexRequest.get().map(response =>
      response.json.validate[ExchangeRatesDTO]
        .getOrElse(throw new IllegalArgumentException("Response can't be casted")))
  }
}
