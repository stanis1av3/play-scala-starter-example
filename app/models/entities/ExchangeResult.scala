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

  implicit def of(erd: (ExchangeResultDTO, Double)): ExchangeResult = new ExchangeResult(
    None,
    erd._1.currencyTo,
    erd._1.currencyFrom,
    erd._1.price,
    erd._2,
    new Timestamp(new Date().getTime)
  )
}