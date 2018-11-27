package models.entities

import java.sql.Timestamp
import java.util.Date

import models.ExchangeResultDTO

case class ExchangeResult(
                           id: Option[Long],
                           currencyTo: String,
                           currencyFrom: String,
                           amount: Double,
                           rate: Double,
                           createdDate: Timestamp
                         )

object ExchangeResult {

  def of(erd: ExchangeResultDTO, rate: Double): ExchangeResult = new ExchangeResult(
    None,
    erd.currencyTo,
    erd.currencyFrom,
    erd.price,
    rate,
    new Timestamp(new Date().getTime)
  )
}