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

import org.apache.pekko.actor.ActorSystem
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.Future

class ClientListControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  implicit val system: ActorSystem = app.actorSystem

  private lazy val controller: ClientListController = app.injector.instanceOf[ClientListController]

  private def postJson(action: play.api.mvc.Action[JsValue], body: JsValue): Future[Result] =
    action(FakeRequest("POST", "/").withHeaders("Content-Type" -> "application/json").withBody(body))

  private def statusFor(credentialId: String): Future[Result] =
    postJson(controller.clientListStatus, Json.obj("credentialId" -> credentialId, "service" -> "PAYE", "gracePeriod" -> 14400))

  private def clientsFor(credentialId: String): Future[Result] =
    postJson(
      controller.allClients,
      Json.obj("irAgentId" -> "AB1234", "credentialId" -> credentialId, "start" -> 0, "count" -> 30, "sort" -> 0, "order" -> "ASC")
    )

  private def statusCode(result: Future[Result]): Int = (contentAsJson(result) \ "status").as[Int]

  ".clientListStatus" should {

    Seq("CL-INITIATE" -> -1, "CL-IN-PROGRESS" -> 0, "CL-CLIENTS" -> 1, "CL-FAILED" -> 2).foreach {
      case (credentialId, expected) =>
        s"return status $expected for $credentialId" in {
          val result = statusFor(credentialId)

          status(result) shouldBe Status.OK
          contentType(result) shouldBe Some("application/json")
          statusCode(result) shouldBe expected
        }
    }

    "return the default status of 1 when there is no file for the credential ID" in {
      statusCode(statusFor("random-cred-id")) shouldBe 1
    }

    "return the default status, not a file lookup, when the credential ID is not safe" in {
      statusCode(statusFor("../CL-FAILED")) shouldBe 1
    }

    "return 400 when the payload is invalid" in {
      val result = postJson(controller.clientListStatus, Json.obj("somethingElse" -> "x"))

      status(result) shouldBe Status.BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] should include("Invalid payload")
    }
  }

  ".allClients" should {

    "return the clients for CL-CLIENTS" in {
      val json = contentAsJson(clientsFor("CL-CLIENTS"))

      (json \ "totalClients").as[Int] shouldBe 6
      (json \ "clientNameChars").as[Seq[String]] should contain("H")
    }

    "return the default clients when there is no file for the credential ID" in {
      (contentAsJson(clientsFor("random-cred-id")) \ "totalClients").as[Int] shouldBe 3
    }

    "return 400 when the payload is invalid" in {
      status(postJson(controller.allClients, Json.obj("irAgentId" -> "AB1234"))) shouldBe Status.BAD_REQUEST
    }
  }

  ".startClientListDownload" should {

    "return 200 with the wait time XML" in {
      val result = controller.startClientListDownload("PAYE", "CL-INITIATE")(FakeRequest())

      status(result) shouldBe Status.OK
      contentAsString(result) shouldBe ClientListController.WaitTimeXml
    }
  }
}
