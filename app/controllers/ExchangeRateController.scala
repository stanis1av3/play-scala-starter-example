package controllers

import javax.inject.Inject
import models.ExchangeResultDTO
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc._
import services.ExchangeResultService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class ExchangeRateController @Inject()(messageControllerComponents: MessagesControllerComponents,
                                       exchangeRateService: ExchangeResultService) extends MessagesAbstractController(messageControllerComponents) {

  implicit val ec = ExecutionContext.global

  import ExchangeRateUserForm._

  private val widgets = scala.collection.mutable.ArrayBuffer.apply[ExchangeResultDTO]()

  private val postUrl = routes.ExchangeRateController.createWidget()

  def index = Action {
    Ok(views.html.index())
  }

  def listWidgets = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.listWidgets(widgets, form, postUrl))
  }

  def createWidget = Action { implicit request: MessagesRequest[AnyContent] =>
    val errorFunction = { formWithErrors: Form[Data] =>
      BadRequest(views.html.listWidgets(widgets, formWithErrors, postUrl))
    }

    val successFunction = { data: Data =>

      val widget = ExchangeResultDTO(currencyTo = data.exchangeTo, currencyFrom=data.exchangeFrom, price = data.price)
      Await.ready(
        exchangeRateService.calculateRates(widget)
          .map(f => widgets.append(ExchangeResultDTO(widget.currencyTo, widget.currencyFrom, f, widget.price))), Duration.Inf)

      Redirect(routes.ExchangeRateController.listWidgets()).flashing("info" -> "Exchange rate counted!")
    }

    val formValidationResult = form.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

  //:/rates
  def getRates = Action.async { implicit result =>
    exchangeRateService.getActualRates
      .map(o => Ok(Json.toJson(o)))
  }
  //:/rates/CUR1/CUR2/AMNT
  def getCalculatedRateAmount(currencyFrom: String, currencyTo: String, amount: Double) = Action.async {
    implicit result =>
      exchangeRateService.calculateRates(ExchangeResultDTO(currencyTo, currencyFrom, amount))
        .map(o => Ok(Json.toJson(o)))
  }
}