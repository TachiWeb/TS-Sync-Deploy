package xyz.nulldev.ts.tssyncdeploy

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import spark.Request
import spark.Response
import spark.Route
import java.util.concurrent.TimeUnit

class SyncPage: Route {
    val client = OkHttpClient.Builder()
            .connectTimeout(10000, TimeUnit.SECONDS)
            //Give server plenty of time to generate report
            .readTimeout(3, TimeUnit.MINUTES)
            .writeTimeout(3, TimeUnit.MINUTES)
            .build()

    override fun handle(request: Request, response: Response): Any {
        response.disableCache()
        val account = request.params(":account")
        val token = request.headers("TW-Session")
        val from = request.queryParams("from").toLongOrNull() ?: 0L

        val success = AccountManager.authToken(account, token)
        if(!success)
            //language=json
            return """{"success": false, "error": "Not authenticated!"}"""

        AccountManager.lockAccUp(account) { account, vm ->
            //Forward sync request
            val resp = client.newCall(okhttp3.Request.Builder()
                    .url("http://127.0.0.1:${vm.port}/api/sync?from=$from")
                    .post(RequestBody.create(MEDIA_JSON, request.bodyAsBytes()))
                    .build()).execute()

            //Copy output back to client
            response.status(200)
            response.raw().outputStream.use {
                resp.body()!!.byteStream().copyTo(it)
            }
        }

        //Forcibly shut down VM to avoid memory leaks
        AccountManager.lockAccDown(account) {}

        return ""
    }

    companion object {
        private val MEDIA_JSON = MediaType.parse("application/json; charset=utf-8")
    }
}