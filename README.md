mobile-paye
=============================================

[![Build Status](https://travis-ci.org/hmrc/mobile-paye.svg)](https://travis-ci.org/hmrc/mobile-paye) [ ![Download](https://api.bintray.com/packages/hmrc/releases/mobile-paye/images/download.svg) ](https://bintray.com/hmrc/releases/mobile-paye/_latestVersion)

Return the PAYE information for a given user, including employments, pensions, other incomes, tax free amount and estimated tax amount.

Requirements
------------

The following services are exposed from the micro-service.

Please note it is mandatory to supply an Accept HTTP header to all below services with the value ```application/vnd.hmrc.1.0+json```.

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/mobile-paye/:nino/summary/current-income``` | GET | Fetch the Tax Credits Summary object for a given NINO. [More...](docs/summary.md)|

# Sandbox
All the above endpoints are accessible on sandbox with `/sandbox` prefix on each endpoint,e.g.
```
    GET /sandbox/summary
```

To trigger the sandbox endpoints locally, use the "X-MOBILE-USER-ID" header with one of the following values:
208606423740 or 167927702220

To test different scenarios, add a header "SANDBOX-CONTROL" with one of the following values:

| *Value* | *Description* |
|--------|----|
| "SINGLE-EMPLOYMENT" | Happy path, return a single employment with no pensions or other income |
| "SINGLE-PENSION" | Happy path, return a single pension with no employments or other income |
| "NO-TAX-YEAR-INCOME" | Happy path, return only tax year and 'add missing' links when no live or previous incomes | 
| "PREVIOUS-INCOME-ONLY" | Happy path, return all data except employments, pension, other income when previous income but no live incomes | 
| "DECEASED"  | Happy path, trigger a 410 Gone response when the person is deceased |
| "MCI"       | Happy path, trigger a 423 Locked response when manual correspondence is required |
| "NOT-FOUND" | Unhappy path, URL not found |
| "ERROR-401" | Unhappy path, trigger a 401 Unauthorized response |
| "ERROR-403" | Unhappy path, trigger a 403 Forbidden response |
| "ERROR-500" | Unhappy path, trigger a 500 Internal Server Error response |
| Not set or any other value | Happy path, default user with employments, pensions and other income |

# Definition
API definition for the service will be available under `/api/definition` endpoint.
See definition in `/conf/api-definition.json` for the format.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
