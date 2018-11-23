package models

import play.api.libs.json.Json

case class ExchangeRatesDTO(
                             date :String,
                             rates: Map[String, Double],
                             base: String )

object ExchangeRatesDTO{
  implicit val exchangeRatesDTO = Json.format[ExchangeRatesDTO]
}
