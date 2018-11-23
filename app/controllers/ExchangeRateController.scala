package controllers

import controllers.ExchangeRateUserForm.Data
import javax.inject.Inject
import models.ExchangeResultDTO
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc._
import services.ExchangeResultService

import scala.concurrent.{ExecutionContext, Future}

class ExchangeRateController @Inject()(messageControllerComponents: MessagesControllerComponents,
                                       exchangeRateService: ExchangeResultService)(implicit ec: ExecutionContext) extends MessagesAbstractController(messageControllerComponents) {

  private val widgets = scala.collection.mutable.ArrayBuffer.apply[ExchangeResultDTO]()

  private val postUrl = routes.ExchangeRateController.createWidget()

  def index = Action {
    Ok(views.html.index())
  }

  def listWidgets = Action.async { implicit request: MessagesRequest[AnyContent] =>
    Future {
      Ok(views.html.listWidgets(widgets, ExchangeRateUserForm.form, postUrl))
    }
  }

  def createWidget = Action.async { implicit request: MessagesRequest[AnyContent] =>
    val errorFunction = { formWithErrors: Form[Data] =>
      Future.successful(BadRequest(views.html.listWidgets(widgets, formWithErrors, postUrl)))
    }

    val successFunction = { data: Data =>
      val widget = ExchangeResultDTO(currencyTo = data.exchangeTo, currencyFrom = data.exchangeFrom, price = data.price, 0)
      exchangeRateService.calculateRates(widget)
        .map(f => {
          widgets.append(ExchangeResultDTO(widget.currencyTo, widget.currencyFrom, f, widget.price))
          Redirect(routes.ExchangeRateController.listWidgets()).flashing("info" -> "Exchange rate counted!")
        })
    }
    val formValidationResult = ExchangeRateUserForm.form.bindFromRequest
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
      exchangeRateService.calculateRates(ExchangeResultDTO(currencyTo, currencyFrom, amount, 0))
        .map(o => Ok(Json.toJson(o)))
  }
}