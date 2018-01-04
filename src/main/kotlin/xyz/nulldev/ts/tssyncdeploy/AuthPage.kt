package xyz.nulldev.ts.tssyncdeploy

import spark.Request
import spark.Response
import spark.Route
import kotlin.concurrent.thread

class AuthPage: Route {
    override fun handle(request: Request, response: Response): Any {
        response.disableCache()
        val account = request.params(":account")
        val password = request.queryParams("password")

        val success = AccountManager.authAccount(account, password)

        return if(success) {
            val token = AccountManager.lockAcc(account, Account::token)

            //Allocate VM in advance
            thread { AccountManager.allocateVmFast(account) }

            //language=json
            """{"success": true, "token": "$token"}"""
        } else {
            //language=json
            """{"success": false, "error": "Incorrect password!"}"""
        }
    }
}