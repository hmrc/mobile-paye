The Mobile Paye response
----
  Fetch the MobilePayeResponse object.
  
* **URL**

  `/summary`

* **Method:**
  
  `GET`
  
* **Success Responses:**

  * **Code:** 200 <br />
    **Content:**

```json
{
  "employments": [
    {
      "name": "SAINSBURY'S PLC",
      "taxCode": "1185L",
      "amount": 4143,
      "link": "https://www.tax.service.gov.uk/check-income-tax/income-details/<data.employmentId>"
    },
    {
      "name": "EASTWOOD CHARTER SCHOOL (7601)",
      "payrollNumber": "96245SLJK88",
      "taxCode": "BR",
      "amount": 5690,
      "link": "https://www.tax.service.gov.uk/check-income-tax/income-details/<data.employmentId>"
    },
    {
      "name": "TESCO",
      "taxCode": "BR",
      "amount": 5101,
      "link": "https://www.tax.service.gov.uk/check-income-tax/income-details/<data.employmentId>"
    }
  ],
  "pensions": [
    {
      "name": "HIGHWIRE RETURNS LTD",
      "taxCode": "BR",
      "amount": 4200,
      "link": "https://www.tax.service.gov.uk/check-income-tax/income-details/<data.employmentId>"
    },
    {
      "name": "AVIVA (2410)",
      "payrollNumber": "AB752BR",
      "taxCode": "BR",
      "amount": 5690,
      "link": "https://www.tax.service.gov.uk/check-income-tax/income-details/<data.employmentId>"
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
      "link": "https://www.tax.service.gov.uk/check-income-tax/income/bank-building-society-savings"
    }
  ],
  "taxFreeAmount": 11850,
  "estimatedTaxAmount": 618,
  "understandYourTaxCode": "https://www.tax.service.gov.uk/check-income-tax/tax-codes",
  "addMissingEmployer": "https://www.tax.service.gov.uk/check-income-tax/add-employment/employment-name",
  "addMissingPension": "https://www.tax.service.gov.uk/check-income-tax/add-pension-provider/name",
  "addMissingIncome": "https://www.tax.service.gov.uk/forms/form/tell-us-about-other-income/guide"
}
```

  * **Code:** 404 <br />
    **Note:** No record found <br />
    
* **Error Responses:**

  * **Code:** 401 UNAUTHORIZED <br/>
    **Content:** `{"code":"UNAUTHORIZED","message":"Bearer token is missing or not authorized for access"}`

  * **Code:** 403 FORBIDDEN <br/>
    **Content:** `{"code":"FORBIDDEN","message":Authenticated user is not authorised for this resource"}`

  OR when a user does not exist or server failure

  * **Code:** 500 INTERNAL_SERVER_ERROR <br/>



