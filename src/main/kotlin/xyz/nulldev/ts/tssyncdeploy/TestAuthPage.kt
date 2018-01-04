package xyz.nulldev.ts.tssyncdeploy

import spark.Request
import spark.Response
import spark.Route
import kotlin.concurrent.thread

class TestAuthPage : Route {
    override fun handle(request: Request, response: Response): Any {
        response.disableCache()
        val account = request.params(":account")
        val token = request.headers("TW-Session")

        val success = AccountManager.authToken(account, token)

        //Allocate VM in advance
        if(success)
            thread { AccountManager.allocateVmFast(account) }

        //language=json
        return """{"success": $success}"""
    }
}