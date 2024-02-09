The Mobile Paye tax code response
----
Fetch the current tax code for a user.

If 0 or more than 1 current tax code is found, then a 404 will be returned instead.

* **URL**

  `/mobile-paye/nino/:nino/tax-code`

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
    "taxCode": "CS700100A"
  }
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



