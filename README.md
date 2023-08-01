mobile-paye
=============================================

[![Build Status](https://travis-ci.org/hmrc/mobile-paye.svg)](https://travis-ci.org/hmrc/mobile-paye) [ ![Download](https://api.bintray.com/packages/hmrc/releases/mobile-paye/images/download.svg) ](https://bintray.com/hmrc/releases/mobile-paye/_latestVersion)

Return the PAYE information for a given user, including employments, pensions, other incomes, tax free amount and
estimated tax amount.

Requirements
------------

The following services are exposed from the micro-service.

Please note it is mandatory to supply an Accept HTTP header to all below services with the
value ```application/vnd.hmrc.1.0+json```.

API
---

| *Task*                                                           | *Supported Methods* | *Description*                                                                                                                            |
|------------------------------------------------------------------|---------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| ```/mobile-paye/nino/:nino/tax-year/:taxYear/summary```          | GET                 | Fetch the PAYE Summary object for a given NINO for a given Tax Year. [More...](docs/summary.md)                                          |
| ```/mobile-paye/nino/:nino/previous-tax-year/:taxYear/summary``` | GET                 | Fetch the previous tax year PAYE Summary object for a given NINO for a given previous Tax Year. [More...](docs/previous-year-summary.md) |
| ```/mobile-paye/nino/:nino/income-tax-history```                 | GET                 | Fetch the Income Tax History (Default 5 years) for a given NINO. [More...](docs/income-tax-history.md)                                   |
| ```/mobile-paye/feedback```                                      | POST                | Send user feedback to mobile-feedback which will be sent to splunk. [More...](docs/feedback.md)                                          |

Shuttered
---------
When the service is shuttered it will return the following JSON

```json
{
  "shuttered": true,
  "title": "Service Unavailable",
  "message": "You’ll be able to use the PAYE service at 9am on Monday 29 May 2017."
}
```

To enable you need to set the following config:

```
mobilePaye {
  shuttering = {
    shuttered = true
    title = "Service Unavailable"
    message = "You’ll be able to use the PAYE service at 9am on Monday 29 May 2017."
  }
}
```

# Sandbox

To trigger the sandbox endpoints locally, use the "X-MOBILE-USER-ID" header with one of the following values:
208606423740 or 167927702220

To test different scenarios, add a header "SANDBOX-CONTROL" with one of the following values:

| *Value*                    | *Description*                                                                                                  |
|----------------------------|----------------------------------------------------------------------------------------------------------------|
| "SINGLE-EMPLOYMENT"        | Happy path, return a single employment with no pensions or other income                                        |
| "SINGLE-PENSION"           | Happy path, return a single pension with no employments or other income                                        |
| "NO-TAX-YEAR-INCOME"       | Happy path, return only tax year and 'add missing' links when no live or previous incomes                      | 
| "PREVIOUS-INCOME-ONLY"     | Happy path, return all data except employments, pension, other income when previous income but no live incomes | 
| "OTHER-INCOME-ONLY"        | Happy path, return other income when there are previous incomes but no live income                             | 
| "REFUND"                   | Happy path, return the repayment owed to the user, including the link for the claiming the refund              
| "CHEQUE-SENT"              | Happy path, return the fact that the user has been sent a cheque                                               
| "PAYMENT-PAID"             | Happy path, return the fact that the money owed to the user having been paid                                   
| "PAYMENT-PROCESSING"       | Happy path, return the fact that the payment is under processing                                               
| "DECEASED"                 | Happy path, trigger a 410 Gone response when the person is deceased                                            |
| "MCI"                      | Happy path, trigger a 423 Locked response when manual correspondence is required                               |
| "SHUTTERED"                | Unhappy path, trigger a 521 with the shuttered payload                                                         
| "NOT-FOUND"                | Unhappy path, URL not found                                                                                    |
| "ERROR-401"                | Unhappy path, trigger a 401 Unauthorized response                                                              |
| "ERROR-403"                | Unhappy path, trigger a 403 Forbidden response                                                                 |
| "ERROR-500"                | Unhappy path, trigger a 500 Internal Server Error response                                                     |
| Not set or any other value | Happy path, default user with employments, pensions and other income                                           |

# Definition

API definition for the service will be available under `/api/definition` endpoint.
See definition in `/conf/api-definition.json` for the format.

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
