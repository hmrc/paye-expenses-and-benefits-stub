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

package uk.gov.hmrc.payeexpensesandbenefitsstub.models.requests

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, Json}

class RequestsSpec extends AnyWordSpec with Matchers {

  "ClientListStatusRequest" should {

    val model = ClientListStatusRequest("cred-id", "PAYE", 14400)
    val json  = Json.obj("credentialId" -> "cred-id", "service" -> "PAYE", "gracePeriod" -> 14400)

    "write to JSON" in {
      Json.toJson(model) shouldBe json
    }

    "read from JSON" in {
      json.as[ClientListStatusRequest] shouldBe model
    }

    "fail to read when a field is missing" in {
      Json.obj("credentialId" -> "cred-id").validate[ClientListStatusRequest] shouldBe a[JsError]
    }
  }

  "GetAllClientsRequest" should {

    val model = GetAllClientsRequest("AB1234", "cred-id", 0, 30, 0, "ASC")
    val json  = Json.obj("irAgentId" -> "AB1234", "credentialId" -> "cred-id", "start" -> 0, "count" -> 30, "sort" -> 0, "order" -> "ASC")

    "write to JSON" in {
      Json.toJson(model) shouldBe json
    }

    "read from JSON" in {
      json.as[GetAllClientsRequest] shouldBe model
    }

    "fail to read when a field is missing" in {
      Json.obj("irAgentId" -> "AB1234").validate[GetAllClientsRequest] shouldBe a[JsError]
    }
  }
}
