/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.payeexpensesandbenefitsstub.controllers

import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.payeexpensesandbenefitsstub.models.requests.{ClientListStatusRequest, GetAllClientsRequest}
import uk.gov.hmrc.payeexpensesandbenefitsstub.util.StubResource
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton()
class ClientListController @Inject()(cc: ControllerComponents)
  extends BackendController(cc) with StubResource {

  import ClientListController.*

  def clientListStatus: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withBody[ClientListStatusRequest]("clientListStatus") { payload =>
      respond("clientListStatus", payload.credentialId, StatusFile)
    }
  }

  def allClients: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withBody[GetAllClientsRequest]("allClients") { payload =>
      respond("allClients", payload.credentialId, ClientsFile)
    }
  }

  def startClientListDownload(service: String, credentialId: String): Action[AnyContent] = Action.async {
    logger.info(s"[ClientListController][startClientListDownload]: Client list download started for $service")
    Future.successful(Ok(WaitTimeXml).as(XML))
  }

  private def withBody[B: Reads](method: String)(f: B => Future[Result])(implicit request: Request[JsValue]): Future[Result] =
    request.body.validate[B].fold(
      invalid => {
        logger.error(s"[ClientListController][$method]: Failed to validate payload, errors: $invalid")
        Future.successful(BadRequest(Json.obj("message" -> s"Invalid payload: $invalid")))
      },
      f
    )

  private def respond(method: String, credentialId: String, fileName: String): Future[Result] = {
    val credentialPath = s"$basePath/$credentialId/$fileName"
    val path =
      if (credentialId.matches(SafeCredentialId) && resourceExists(credentialPath)) credentialPath
      else s"$basePath/default/$fileName"
    logger.info(s"[ClientListController][$method]: Returning $path")
    Future.successful(jsonResourceAsResponse(path))
  }
}

object ClientListController {
  val WaitTimeXml: String              = "<clientListWaitTime><waitTime>60</waitTime></clientListWaitTime>"
  private val SafeCredentialId: String = "[A-Za-z0-9-]+"
  private val basePath: String         = "/resources.clientList"
  private val StatusFile: String       = "status.json"
  private val ClientsFile: String      = "clients.json"
}
