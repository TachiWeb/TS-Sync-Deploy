package xyz.nulldev.ts.tssyncdeploy

import spark.Request
import spark.Response
import spark.Route
import java.io.File

class ResetSyncDataPage : Route {
    override fun handle(request: Request, response: Response): Any {
        val account = request.queryParams("account")
        val token = request.queryParams("token")

        val success = AccountManager.authToken(account, token)
        if(!success)
            return "Invalid auth token!"

        AccountManager.lockAccDown(account) {
            File(it.folder, "tachiserver-data").deleteRecursively()
        }

        return "All sync data deleted!"
    }
}