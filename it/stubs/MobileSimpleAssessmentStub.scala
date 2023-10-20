package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.time.TaxYear

object MobileSimpleAssessmentStub {

  val currentTaxYear  = TaxYear.current.startYear
  val previousTaxYear = currentTaxYear - 1
  val cyMinus2TaxYear = previousTaxYear - 1

  def stubSimpleAssessmentResponse: StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/liabilities?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
        )
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "taxYears" : [
                       |    {
                       |      "taxYear": $previousTaxYear,
                       |      "reconciliations": [
                       |        {
                       |          "reconciliationId": 1,
                       |          "reconciliationStatus": 5,
                       |          "cumulativeAmount": 200,
                       |          "taxLiabilityAmount": 300,
                       |          "taxPaidAmount": 100,
                       |          "reconciliationTimeStamp": "$previousTaxYear-07-30 12:34:56",
                       |          "p800Status": "ISSUED",
                       |          "previousReconciliationId": 2,
                       |          "p800Reasons": [
                       |            {
                       |              "reasonType": "UNDERPAYMENT",
                       |              "reasonCode": 45,
                       |              "estimatedAmount": 175,
                       |              "actualAmount": 185
                       |            }
                       |          ],
                       |          "businessReason": "P302",
                       |          "eligibility": true,
                       |          "totalAmountOwed": 400,
                       |          "chargeReference": "XQ004100001540",
                       |          "dueDate": "${currentTaxYear + 1}-01-31",
                       |          "receivableStatus": "OUTSTANDING",
                       |          "receipts": [
                       |            {
                       |              "receiptAmount": 100,
                       |              "receiptDate": "$currentTaxYear-08-02",
                       |              "receiptMethod": "RECEIVED FROM ETMP",
                       |              "receiptStatus": "ALLOCATED",
                       |              "receiptDescription": "PAID-ONLINE",
                       |              "allocatedAmount": 100
                       |            }
                       |          ]
                       |        }
                       |      ]
                       |    },
                       |    {
                       |    "taxYear" : $cyMinus2TaxYear,
                       |    "reconciliations" : [
                       |      {
                     |          "reconciliationId": 2,
                     |          "reconciliationStatus": 5,
                     |          "cumulativeAmount": 100,
                     |          "taxLiabilityAmount": 200,
                     |          "taxPaidAmount": 50,
                     |          "reconciliationTimeStamp": "${previousTaxYear-1}-07-30 12:34:56",
                     |          "p800Status": "ISSUED",
                     |          "p800Reasons": [
                     |            {
                     |              "reasonType": "UNDERPAYMENT",
                     |              "reasonCode": 45,
                     |              "estimatedAmount": 175,
                     |              "actualAmount": 185
                     |            }
                     |          ],
                     |          "businessReason": "P302",
                     |          "eligibility": true,
                     |          "totalAmountOwed": 300,
                     |          "chargeReference": "XQ004100001539",
                     |          "dueDate": "${currentTaxYear + 1}-01-31",
                     |          "receivableStatus": "OUTSTANDING",
                     |          "receipts": [
                     |            {
                     |              "receiptAmount": 150,
                     |              "receiptDate": "$previousTaxYear-08-02",
                     |              "receiptMethod": "RECEIVED FROM ETMP",
                     |              "receiptStatus": "ALLOCATED",
                     |              "allocatedAmount": 150
                     |            }
                     |          ]
                     |        }
                     |      ]
                     |    }
                     |  ]
                     |}
                     |
          """.stripMargin)
      )
    )

}
