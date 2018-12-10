The Mobile Paye response
----
  Fetch the MobilePayeResponse object.
  
* **URL**

  `/summary`

* **Method:**
  
  `GET`
  
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
      "link": "/check-income-tax/income-details/1"
    },
    {
      "name": "EASTWOOD CHARTER SCHOOL (7601)",
      "payrollNumber": "96245SLJK88",
      "taxCode": "BR",
      "amount": 5690,
      "link": "/check-income-tax/income-details/2"
    },
    {
      "name": "TESCO",
      "taxCode": "BR",
      "amount": 5101,
      "link": "/check-income-tax/income-details/3"
    }
  ],
  "pensions": [
    {
      "name": "HIGHWIRE RETURNS LTD",
      "taxCode": "BR",
      "amount": 4200,
      "link": "/check-income-tax/income-details/9"
    },
    {
      "name": "AVIVA (2410)",
      "payrollNumber": "AB752BR",
      "taxCode": "BR",
      "amount": 5690,
      "link": "/check-income-tax/income-details/7"
    }
  ],
  "otherIncomes": [
    {
      "name": "NON CODED INCOME",
      "amount": 22500
    },
    {
      "name": "UNTAXED INTEREST",
      "amount": 9301,
      "link": "/check-income-tax/income/bank-building-society-savings"
    }
  ],
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/check-income-tax/tax-free-allowance",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/check-income-tax/paye-income-tax-estimate",
  "understandYourTaxCodeLink": "/check-income-tax/tax-codes",
  "addMissingEmployerLink": "/check-income-tax/add-employment/employment-name",
  "addMissingPensionLink": "/check-income-tax/add-pension-provider/name",
  "addMissingIncomeLink": "/forms/form/tell-us-about-other-income/guide"
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
      "link": "/check-income-tax/income-details/8"
    }
  ],
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/check-income-tax/tax-free-allowance",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/check-income-tax/paye-income-tax-estimate",
  "understandYourTaxCodeLink": "/check-income-tax/tax-codes",
  "addMissingEmployerLink": "/check-income-tax/add-employment/employment-name",
  "addMissingPensionLink": "/check-income-tax/add-pension-provider/name",
  "addMissingIncomeLink": "/forms/form/tell-us-about-other-income/guide"
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
      "link": "/income-details/1"
    }
  ],
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/check-income-tax/tax-free-allowance",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/check-income-tax/paye-income-tax-estimate",
  "understandYourTaxCodeLink": "/check-income-tax/tax-codes",
  "addMissingEmployerLink": "/check-income-tax/add-employment/employment-name",
  "addMissingPensionLink": "/check-income-tax/add-pension-provider/name",
  "addMissingIncomeLink": "/forms/form/tell-us-about-other-income/guide"
}
```

 * **Code:** 200 <br />
    **Content:** Single Other Income

```json
{
  "taxYear": 2018,
  "otherIncomes": [
    {
      "name": "NON CODED INCOME",
      "amount": 22500
    }
  ],
  "taxFreeAmount": 11850,
  "taxFreeAmountLink": "/check-income-tax/tax-free-allowance",
  "estimatedTaxAmount": 618,
  "estimatedTaxAmountLink": "/check-income-tax/paye-income-tax-estimate",
  "understandYourTaxCodeLink": "/check-income-tax/tax-codes",
  "addMissingEmployerLink": "/check-income-tax/add-employment/employment-name",
  "addMissingPensionLink": "/check-income-tax/add-pension-provider/name",
  "addMissingIncomeLink": "/forms/form/tell-us-about-other-income/guide"
}
```

 * **Code:** 200 <br />
    **Content:** No Live Incomes

```json
{
  "taxYear": 2018,
  "addMissingEmployerLink": "/check-income-tax/add-employment/employment-name",
  "addMissingPensionLink": "/check-income-tax/add-pension-provider/name",
  "addMissingIncomeLink": "/forms/form/tell-us-about-other-income/guide"
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
    
  * **Code:** 406 NOT_ACCEPTABLE <br/>
    **Content:** `{"code":"NOT_ACCEPTABLE","message":Missing Accept Header"}`

  OR when a user does not exist or server failure

  * **Code:** 500 INTERNAL_SERVER_ERROR <br/>



