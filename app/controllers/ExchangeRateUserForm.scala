package controllers

object ExchangeRateUserForm {
  import play.api.data.Forms._
  import play.api.data.Form

  case class Data(
                   exchangeTo: String,
                   exchangeFrom: String,
                   price: Int)

  val form = Form(
    mapping(
      "exchange to" -> nonEmptyText,
      "exchange from" -> nonEmptyText,
      "Amount" -> number(min = 0)
    )(Data.apply)(Data.unapply)
  )
}