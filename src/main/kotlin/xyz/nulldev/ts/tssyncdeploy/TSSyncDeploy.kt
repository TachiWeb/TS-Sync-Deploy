package xyz.nulldev.ts.tssyncdeploy

import spark.Route
import spark.Spark
import java.io.File

class TSSyncDeploy {
    fun run() {
        if(!File(VM.SERVER_FILE).exists())
            throw IllegalStateException("Server file (${VM.SERVER_FILE}) does not exist!")

        Spark.port(1234)
        Spark.ipAddress("127.0.0.1")
        getRoute("", MainPage())
        postRoute("/account", AccountPage())
        getRoute("/s/:account/test_auth", TestAuthPage())
        getRoute("/s/:account/auth", AuthPage())
        postRoute("/s/:account/sync", SyncPage())
        postRoute("/account/clear-data", ResetSyncDataPage())
        postRoute("/account/change-password", ChangePasswordPage())
    }

    private fun getRoute(path: String, route: Route) {
        Spark.get(path, route)
        Spark.get(path + "/", route)
    }

    private fun postRoute(path: String, route: Route) {
        Spark.post(path, route)
        Spark.post(path + "/", route)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TSSyncDeploy().run()
        }
    }
}