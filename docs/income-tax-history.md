The Mobile Paye income tax history response
----
Fetch the income tax history for a user.

* **URL**

  `/mobile-paye/nino/:nino/income-tax-history`

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
[
  {
    "taxYear": 2022,
    "incomes": [
      {
        "name": "Sainsbury's PLC",
        "payrollNumber": "9282SAINS22",
        "startDate": "2020-07-10",
        "amount": 18900,
        "taxAmount": 1578,
        "taxCode": "1275L",
        "isPension": false
      }
    ]
  },
  {
    "taxYear": 2021,
    "incomes": [
      {
        "name": "Sainsbury's PLC",
        "payrollNumber": "9282SAINS22",
        "startDate": "2020-07-10",
        "amount": 27900,
        "taxAmount": 2134,
        "taxCode": "1275L",
        "isPension": false
      }
    ]
  },
  {
    "taxYear": 2020,
    "incomes": [
      {
        "name": "Sainsbury's PLC",
        "payrollNumber": "9282SAINS22",
        "startDate": "2020-07-10",
        "amount": 15679,
        "taxAmount": 1345,
        "taxCode": "1275L",
        "isPension": false
      },
      {
        "name": "TESCO LTD",
        "payrollNumber": "1212TESCO12",
        "startDate": "2019-05-25",
        "endDate": "2020-07-05",
        "amount": 3567,
        "taxAmount": 200,
        "taxCode": "1275L",
        "isPension": false
      }
    ]
  },
  {
    "taxYear": 2019,
    "incomes": [
      {
        "name": "TESCO LTD",
        "payrollNumber": "1212TESCO12",
        "startDate": "2019-05-25",
        "endDate": "2020-07-05",
        "amount": 16098,
        "taxAmount": 1789,
        "taxCode": "1275L",
        "isPension": false
      }
    ]
  },
  {
    "taxYear": 2018
  }
]
```

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



