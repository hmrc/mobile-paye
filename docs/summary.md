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
  "taxYear": 2018,
  "employments": [
    {
      "name": "SAINSBURY'S PLC",
      "taxCode": "1185L",
      "amount": 4143,
      "link": "/",
      "updateIncomeLink": "/",
      "latestPayment": {
        "date":   "2021-02-28",
        "amount": 1575,
        "link":   "/"
      }
    },
    {
      "name": "EASTWOOD CHARTER SCHOOL (7601)",
      "payrollNumber": "96245SLJK88",
      "taxCode": "BR",
      "amount": 5690,
      "link": "/",
      "updateIncomeLink": "/",
      "latestPayment": {
        "date":   "2021-02-26",
        "amount": 300,
        "link":   "/"
      }
    },
    {
      "name": "TESCO",
      "taxCode": "BR",
      "amount": 5101,
      "link": "/",
      "updateIncomeLink": "/",
      "latestPayment": {
        "date":   "2021-02-14",
        "amount": 475,
        "link":   "/"
      }
    }
  ],
  "pensions": [
    {
      "name": "HIGHWIRE RETURNS LTD",
      "taxCode": "BR",
      "amount": 4200,
      "link": "/"
    },
    {
      "name": "AVIVA (2410)",
      "payrollNumber": "AB752BR",
      "taxCode": "BR",
      "amount": 5690,
      "link": "/"
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
      "link": "/"
    }
  ],
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "previousTaxYearLink": "/",
  "currentYearPlusOneLink": "/",
  "taxCodeLocation": "rUK"
}
```

 * **Code:** 200 <br />
    **Content:** Single Employment

```json
{
  "taxYear": 2018,
  "employments": [
    {
      "name": "SAINSBURY'S PLC",
      "taxCode": "1185L",
      "amount": 4143,
      "link": "/"
    }
  ],
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "previousTaxYearLink": "/",
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
      "link": "/"
    }
  ],
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "previousTaxYearLink": "/",
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
      "link": "/"
    }
  ],
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "previousTaxYearLink": "/"
}
```

 * **Code:** 200 <br />
    **Content:** Previous Income Only

```json
{
  "taxYear": 2018,
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/",
  "understandYourTaxCodeLink": "/",
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "previousTaxYearLink": "/"
}
```

 * **Code:** 200 <br />
    **Content:** No Live Incomes

```json
{
  "taxYear": 2018,
  "addMissingEmployerLink": "/",
  "addMissingPensionLink": "/",
  "addMissingIncomeLink": "/",
  "previousTaxYearLink": "/"
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



