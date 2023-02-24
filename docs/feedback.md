The Mobile Paye feedback request
----

POST the feedback object from the app to the mobile-feedback service

* **URL**

  `/mobile-paye/feedback`

* **Method:**

  `POST`

* **Body**

To hit the endpoint a feedback model must also be provided. The fields are optional so not all need to be included.

```json
{ 
    "ableToDo": true, 
    "howEasyScore": 5, 
    "whyGiveScore": "It was great", 
    "howDoYouFeelScore": 4
}
```

* **Success Responses:**

    * **Code:** 204

* **Error Responses:**

    * **Code:** 401 UNAUTHORIZED <br/>
      **Content:** `{"code":"UNAUTHORIZED","message":"Bearer token is missing or not authorized for access"}`

    * **Code:** 406 NOT_ACCEPTABLE <br/>
      **Content:** `{"code":"NOT_ACCEPTABLE","message":Missing Accept Header"}`

    * **Code:** 400 BAD_REQUEST <br/>



* OR when a user does not exist or server failure

     * **Code:** 500 INTERNAL_SERVER_ERROR <br/>