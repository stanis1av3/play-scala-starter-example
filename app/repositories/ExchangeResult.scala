package repositories


import java.sql.Timestamp
import java.util.Date

import models.ExchangeResultDTO

case class ExchangeResult(
                           id: Long,
                           currencyTo: String,
                           currencyFrom: String,
                           amount: Double,
                           rate: Double,
                           createdDate: Timestamp) {

}

object ExchangeResult {

  def of(erd: ExchangeResultDTO, rate: Double): ExchangeResult = new ExchangeResult(
    0,
    erd.currencyTo,
    erd.currencyFrom,
    erd.price,
    rate,
    new Timestamp(new Date().getTime)
    )
}