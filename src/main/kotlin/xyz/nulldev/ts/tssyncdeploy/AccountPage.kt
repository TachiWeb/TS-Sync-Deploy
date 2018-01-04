package xyz.nulldev.ts.tssyncdeploy

import com.google.gson.JsonParser
import okhttp3.FormBody
import okhttp3.OkHttpClient
import spark.Request
import spark.Response
import spark.Route

class AccountPage: Route {
    val client = OkHttpClient.Builder().build()
    val jsonParser = JsonParser()

    override fun handle(request: Request, response: Response): Any {
        response.disableCache()
        val ip = request.headers("X-Real-IP")
        val username = request.queryParams("username")
        val password = request.queryParams("password")
        val captcha = request.queryParams("g-recaptcha-response")

        if(username.isBlank())
            return "Username cannot be empty!"

        if(username.any { it !in AccountManager.VALID_USERNAME_CHARS })
            return "Username can only contain alphabetical characters, numerical characters, '@', '-' and '_'!"

        if(username.length > AccountManager.MAX_USERNAME_LENGTH)
            return "Username cannot be longer than: ${AccountManager.MAX_USERNAME_LENGTH} characters!"

        if(captcha == null || captcha.isBlank())
            return "Please complete the RECAPTCHA challenge!"

        val resp = client.newCall(okhttp3.Request.Builder()
                .url("https://www.google.com/recaptcha/api/siteverify")
                .post(FormBody.Builder()
                        .add("secret", RECAPTCHA_SECRET)
                        .add("response", captcha)
                        .add("remoteip", ip)
                        .build()).build()).execute()

        val captchaStatus = jsonParser.parse(resp.body()!!.string()).asJsonObject["success"].asBoolean

        if(!captchaStatus)
            return "RECAPTCHA challenge failed!"

        lateinit var token: String

        //Auth account
        AccountManager.lockAcc(username) {
            if(it.configured) {
                //Auth as account exists
                if(!AccountManager.authAccount(username, password)) {
                    return "The supplied password does not match the username! If you are trying to sign-up for a new account, this username is already taken, please choose a different username!"
                }
            } else {
                //Create new account
                AccountManager.confAccount(username, password)
            }

            token = it.token.toString()
        }

        //language=html
        fun action(name: String, url: String) = """
        <form action='$url' method='POST' target="_blank">
            <input type='hidden' name='account' value='$username'/>
            <input type='hidden' name='token' value='$token'/>
            <input type='submit' value='$name'/>
        </form>
            """

        //language=html
        return """
<html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Account Panel</title>
    </head>
    <body>
        <p>Welcome back $username!</p>
        <br>
        <p><b>Sync Credentials:</b></p>
        <p><b>URL:</b> <span style='font-family: monospace;'>https://tachiyomi.nd.vu/s/$username/</span></p>
        <p><b>Password:</b> <i>Use your account password</i></p>
        <br>
        <p>Delete all sync data:</p>
        ${action("Clear sync data", "/account/clear-data")}
        <p>Change your password:</p>
        ${action("Change password", "/account/change-password")}
    </body>
</html>
            """
    }

    companion object {
        //TODO THIS IS SECRET!
        private val RECAPTCHA_SECRET = System.getProperty("recaptcha.key")!!
    }
}