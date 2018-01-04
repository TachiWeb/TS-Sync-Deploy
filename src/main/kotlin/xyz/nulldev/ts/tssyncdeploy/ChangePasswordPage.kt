package xyz.nulldev.ts.tssyncdeploy

import spark.Request
import spark.Response
import spark.Route

class ChangePasswordPage: Route {
    override fun handle(request: Request, response: Response): Any {
        val account = request.queryParams("account")
        val token = request.queryParams("token")
        val password = request.queryParams("password")

        val success = AccountManager.authToken(account, token)
        if(!success)
            return "Invalid auth token!"

        return if(password == null) {
            //language=html
            """
                <p>Please enter the new password:</p>
                <form action="" method="POST">
                    <input type="hidden" name="account" value='$account'/>
                    <input type="hidden" name="token" value='$token'/>
                    <input type="password" name="password" value=''/>
                    <input type='submit'/>
                </form>
                """
        } else {
            AccountManager.confAccountPw(account, password)
            "Password successfully changed!"
        }
    }
}