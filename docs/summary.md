The Mobile Paye response
----
Fetch the MobilePayeResponse object.

* **URL**

  `/mobile-paye/nino/:nino/taxYear/:taxYear/summary`

  > `taxYear` can be set to 'current' to return data for the current tax year. This will be relative the the UK timezone rather than being reliant on you passing the date as per device time.

* **Method:**

  `GET`

*  **URL Params**

   **Required:**

   `journeyId=[String]`

   a string which is included for journey tracking purposes but has no functional impact

* **Success Responses:**

    * **Code:** 200 <br />
      **Content:** Full Response

```json
{
  "taxYear": 2021,
  "employments": [
    {
      "name": "SAINSBURY'S PLC",
      "taxCode": "1185L",
      "amount": 4636,
      "payeNumber": "P12345",
      "incomeDetailsLink": "/",
      "yourIncomeCalculationDetailsLink": "/",
      "updateIncomeLink": "/",
      "updateEmployerLink": "/",
      "payments": [
        {
          "date": "2021-07-28",
          "amountYearToDate": 4636,
          "taxAmountYearToDate": 865,
          "nationalInsuranceAmountYearToDate": 468,
          "amount": 1600,
          "taxAmount": 320,
          "nationalInsuranceAmount": 195
        },
        {
          "date": "2021-06-28",
          "amountYearToDate": 3036,
          "taxAmountYearToDate": 545,
          "nationalInsuranceAmountYearToDate": 273,
          "amount": 1566,
          "taxAmount": 289,
          "nationalInsuranceAmount": 146
        },
        {
          "date": "2021-05-28",
          "amountYearToDate": 1470,
          "taxAmountYearToDate": 256,
          "nationalInsuranceAmountYearToDate": 127,
          "amount": 1470,
          "taxAmount": 256,
          "nationalInsuranceAmount": 127
        }
      ],
      "employmentBenefits": {
        "benefits": [
          {
            "benefitType": "CarBenefit",
            "amount": 20000
          },
          {
            "benefitType": "MedicalInsurance",
            "amount": 650
          },
          {
            "benefitType": "OtherBenefits",
            "amount": 450
          }
        ],
        "changeCarBenefitLink": "/",
        "changeMedicalBenefitLink": "/",
        "changeOtherBenefitLink": "/"
      }
    },
    {
      "name": "EASTWOOD CHARTER SCHOOL (7601)",
      "payrollNumber": "96245SLJK88",
      "taxCode": "BR",
      "amount": 5690,
      "payeNumber": "P54321",
      "incomeDetailsLink": "/",
      "yourIncomeCalculationDetailsLink": "/",
      "updateIncomeLink": "/",
      "updateEmployerLink": "/",
      "payments": [
        {
          "date": "2021-05-28",
          "amountYearToDate": 2500,
          "taxAmountYearToDate": 500,
          "nationalInsuranceAmountYearToDate": 280,
          "amount": 1250,
          "taxAmount": 250,
          "nationalInsuranceAmount": 140
        },
        {
          "date": "2021-04-28",
          "amountYearToDate": 1250,
          "taxAmountYearToDate": 250,
          "nationalInsuranceAmountYearToDate": 140,
          "amount": 1250,
          "taxAmount": 250,
          "nationalInsuranceAmount": 140
        }
      ],
      "employmentBenefits": {
        "benefits": [
          {
            "benefitType": "MedicalInsurance",
            "amount": 350
          },
          {
            "benefitType": "OtherBenefits",
            "amount": 100
          }
        ],
        "changeCarBenefitLink": "/",
        "changeMedicalBenefitLink": "/",
        "changeOtherBenefitLink": "/"
      }
    },
    {
      "name": "TESCO",
      "taxCode": "BR",
      "amount": 5101,
      "incomeDetailsLink": "/",
      "yourIncomeCalculationDetailsLink": "/",
      "updateIncomeLink": "/",
      "updateEmployerLink": "/",
    }
  ],
  "previousEmployments": [
    {
      "name": "ALDI PLC",
      "status": "Ceased",
      "taxCode": "1250L",
      "payrollNumber": "EMP0000001",
      "amount": 18900,
      "payeNumber": "P12345",
      "incomeDetailsLink": "/",
      "yourIncomeCalculationDetailsLink": "/",
      "updateIncomeLink": "/",
      "updateEmployerLink": "/",
      "payments": [
        {
          "date": "2020-07-28",
          "amountYearToDate": 4636,
          "taxAmountYearToDate": 865,
          "nationalInsuranceAmountYearToDate": 468,
          "amount": 1600,
          "taxAmount": 320,
          "nationalInsuranceAmount": 195
        },
        {
          "date": "2020-06-28",
          "amountYearToDate": 3036,
          "taxAmountYearToDate": 545,
          "nationalInsuranceAmountYearToDate": 273,
          "amount": 1566,
          "taxAmount": 289,
          "nationalInsuranceAmount": 146
        },
        {
          "date": "2020-05-28",
          "amountYearToDate": 1470,
          "taxAmountYearToDate": 256,
          "nationalInsuranceAmountYearToDate": 127,
          "amount": 1470,
          "taxAmount": 256,
          "nationalInsuranceAmount": 127
        }
      ]
    },
    {
      "name": "Lidl PLC",
      "status": "PotentiallyCeased",
      "taxCode": "1250L",
      "payrollNumber": "EMP0000001",
      "amount": 18900,
      "payeNumber": "P12345",
      "incomeDetailsLink": "/",
      "yourIncomeCalculationDetailsLink": "/",
      "updateIncomeLink": "/",
      "updateEmployerLink": "/",
      "payments": [
        {
          "date": "2020-07-28",
          "amountYearToDate": 4636,
          "taxAmountYearToDate": 865,
          "nationalInsuranceAmountYearToDate": 468,
          "amount": 1600,
          "taxAmount": 320,
          "nationalInsuranceAmount": 195
        },
        {
          "date": "2020-06-28",
          "amountYearToDate": 3036,
          "taxAmountYearToDate": 545,
          "nationalInsuranceAmountYearToDate": 273,
          "amount": 1566,
          "taxAmount": 289,
          "nationalInsuranceAmount": 146
        },
        {
          "date": "2020-05-28",
          "amountYearToDate": 1470,
          "taxAmountYearToDate": 256,
          "nationalInsuranceAmountYearToDate": 127,
          "amount": 1470,
          "taxAmount": 256,
          "nationalInsuranceAmount": 127
        }
      ]
    }
  ],
  "pensions": [
    {
      "name": "HIGHWIRE RETURNS LTD",
      "taxCode": "BR",
      "amount": 4200,
      "payeNumber": "P12345",
      "incomeDetailsLink": "/",
      "yourIncomeCalculationDetailsLink": "/"
    },
    {
      "name": "AVIVA (2410)",
      "payrollNumber": "AB752BR",
      "taxCode": "BR",
      "amount": 5690,
      "payeNumber": "P12345",
      "incomeDetailsLink": "/",
      "yourIncomeCalculationDetailsLink": "/"
    }
  ],
  "otherIncomes": [
    {
      "name": "NON CODED INCOME",
      "amount": 22500
    },
    {
      "name": "UNTAXED INTEREST INCOME",
      "amount": 9301,
      "incomeDetailsLink": "/"
    }
  ],
  "taxCodeChange": {
    "hasChanged": true,
    "startDate": "2024-04-06",
    "taxCodeChangeUrl": "/",
    "taxCodeChangeUrlCy": "/"
  },
  "simpleAssessment": {
    "taxYears": [
      {
        "taxYear": 2022,
        "reconciliations": [
          {
            "reconciliationId": 1,
            "reconciliationStatus": 5,
            "cumulativeAmount": 200,
            "taxLiabilityAmount": 300,
            "taxPaidAmount": 100,
            "reconciliationTimeStamp": "2022-07-30 12:34:56",
            "p800Status": "ISSUED",
            "previousReconciliationId": 2,
            "p800Reasons": [
              {
                "reasonType": "UNDERPAYMENT",
                "reasonCode": 45,
                "estimatedAmount": 175,
                "actualAmount": 185
              }
            ],
            "businessReason": "P302",
            "eligibility": true,
            "totalAmountOwed": 400,
            "chargeReference": "XQ004100001540",
            "dueDate": "2024-01-31",
            "receivableStatus": "OUTSTANDING",
            "receipts": [
              {
                "receiptAmount": 100,
                "receiptDate": "2023-08-02",
                "receiptMethod": "RECEIVED FROM ETMP",
                "receiptStatus": "ALLOCATED",
                "allocatedAmount": 100
              }
            ]
          }
        ],
        "cardPaymentFallbackUrl": "/"
      },
      {
        "taxYear": 2021,
        "reconciliations": [
          {
            "reconciliationId": 2,
            "reconciliationStatus": 5,
            "cumulativeAmount": 100,
            "taxLiabilityAmount": 200,
            "taxPaidAmount": 50,
            "reconciliationTimeStamp": "2021-07-30 12:34:56",
            "p800Status": "ISSUED",
            "p800Reasons": [
              {
                "reasonType": "UNDERPAYMENT",
                "reasonCode": 45,
                "estimatedAmount": 175,
                "actualAmount": 185
              }
            ],
            "businessReason": "P302",
            "eligibility": true,
            "totalAmountOwed": 300,
            "chargeReference": "XQ004100001539",
            "dueDate": "2024-01-31",
            "receivableStatus": "OUTSTANDING",
            "receipts": [
              {
                "receiptAmount": 150,
                "receiptDate": "2022-08-02",
                "receiptMethod": "RECEIVED FROM ETMP",
                "receiptStatus": "ALLOCATED",
                "allocatedAmount": 150
              }
            ]
          }
        ],
        "cardPaymentFallbackUrl": "/"
      }
    ]
  },
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "addMissingBenefitLink": "/",
  "addMissingCompanyCarLink": "/",
  "previousTaxYearLink": "/",
  "updateEstimatedIncomeLink": "/",
  "currentYearPlusOneLink": "/",
  "taxCodeLocation": "rUK"
}
```

* **Code:** 200 <br />
  **Content:** Single Employment

```json
{
  "taxYear": 2021,
  "employments": [
    {
      "name": "SAINSBURY'S PLC",
      "taxCode": "1185L",
      "amount": 4143,
      "payeNumber": "P12345",
      "incomeDetailsLink": "/",
      "yourIncomeCalculationDetailsLink": "/",
      "updateIncomeLink": "/",
      "payments": [
        {
          "date": "2021-07-28",
          "amountYearToDate": 4636,
          "taxAmountYearToDate": 865,
          "nationalInsuranceAmountYearToDate": 468,
          "amount": 1600,
          "taxAmount": 320,
          "nationalInsuranceAmount": 195
        },
        {
          "date": "2021-06-28",
          "amountYearToDate": 3036,
          "taxAmountYearToDate": 545,
          "nationalInsuranceAmountYearToDate": 273,
          "amount": 1566,
          "taxAmount": 289,
          "nationalInsuranceAmount": 146
        },
        {
          "date": "2021-05-28",
          "amountYearToDate": 1470,
          "taxAmountYearToDate": 256,
          "nationalInsuranceAmountYearToDate": 127,
          "amount": 1470,
          "taxAmount": 256,
          "nationalInsuranceAmount": 127
        }
      ]
    }
  ],
  "taxCodeChange": {
    "hasChanged": false
  },
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "addMissingBenefitLink": "/",
  "addMissingCompanyCarLink": "/",
  "previousTaxYearLink": "/",
  "updateEstimatedIncomeLink": "/",
  "currentYearPlusOneLink": "/",
  "taxCodeLocation": "rUK"
}
```

* **Code:** 200 <br />
  **Content:** Single Pension

```json
{
  "taxYear": 2018,
  "pensions": [
    {
      "name": "HIGHWIRE RETURNS LTD",
      "taxCode": "BR",
      "amount": 4200,
      "payeNumber": "P12345",
      "incomeDetailsLink": "/",
      "yourIncomeCalculationDetailsLink": "/"
    }
  ],
  "taxCodeChange": {
    "hasChanged": false
  },
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "addMissingBenefitLink": "/",
  "addMissingCompanyCarLink": "/",
  "previousTaxYearLink": "/",
  "updateEstimatedIncomeLink": "/",
  "currentYearPlusOneLink": "/",
  "taxCodeLocation": "rUK"
}
```

* **Code:** 200 <br />
  **Content:** Other Income Only

```json
{
  "taxYear": 2018,
  "otherIncomes": [
    {
      "name": "NON CODED INCOME",
      "amount": 22500
    },
    {
      "name": "UNTAXED INTEREST INCOME",
      "amount": 9301,
      "incomeDetailsLink": "/"
    }
  ],
  "taxCodeChange": {
    "hasChanged": false
  },
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "addMissingBenefitLink": "/",
  "addMissingCompanyCarLink": "/",
  "previousTaxYearLink": "/",
  "updateEstimatedIncomeLink": "/"
}
```

* **Code:** 200 <br />
  **Content:** Previous Income Only

```json
{
  "taxYear": 2018,
  "taxCodeChange": {
    "hasChanged": false
  },
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "addMissingBenefitLink": "/",
  "addMissingCompanyCarLink": "/",
  "previousTaxYearLink": "/",
  "updateEstimatedIncomeLink": "/"
}
```

* **Code:** 200 <br />
  **Content:** No Live Incomes

```json
{
  "taxYear": 2018,
  "taxCodeChange": {
    "hasChanged": false
  },
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "addMissingBenefitLink": "/",
  "addMissingCompanyCarLink": "/",
  "previousTaxYearLink": "/",
  "updateEstimatedIncomeLink": "/"
}
```

* **Code:** 410 <br />
  **Note:** Person is deceased <br />

* **Code:** 423 <br />
  **Note:** Person data is corrupt <br />

* **Error Responses:**

    * **Code:** 401 UNAUTHORIZED <br/>
      **Content:** `{"code":"UNAUTHORIZED","message":"Bearer token is missing or not authorized for access"}`

    * **Code:** 403 FORBIDDEN <br/>
      **Content:** `{"code":"FORBIDDEN","message":Authenticated user is not authorised for this resource"}`

    * **Code:** 404 NOT_FOUND <br/>

    * **Code:** 406 NOT_ACCEPTABLE <br/>
      **Content:** `{"code":"NOT_ACCEPTABLE","message":Missing Accept Header"}`

  OR when a user does not exist or server failure

    * **Code:** 500 INTERNAL_SERVER_ERROR <br/>

    * **Code:** 521 SHUTTERED <br/>
      **Content:** ```{
      "shuttered": true,
      "title": "Service Unavailable",
      "message": "You’ll be able to use the PAYE service at 9am on Monday 29 May 2017."
      }```


