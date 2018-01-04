package xyz.nulldev.ts.tssyncdeploy

import spark.Request
import spark.Response
import spark.Route

class MainPage : Route {
    //language=html
    override fun handle(request: Request?, response: Response?) = """
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <script src='https://www.google.com/recaptcha/api.js'></script>
    <title>Tachiyomi Sync Server</title>
</head>
<body>
<script>
    function onSubmit(token) {
        document.getElementById("login").submit();
    }
 </script>
<p>Welcome to nulldev's Tachiyomi sync server! If you have an account, you can log in below. <b>If you do not have an account and would like to create one, fill in your username and password below and click the button.</b></p>
<form id="login" action="/account" method="POST">
    Username:<br>
    <input type="text" name="username">
    <br>
    Password:<br>
    <input type="password" name="password">
    <br><br>
    <button
            class="g-recaptcha"
            data-sitekey="6LcCGD8UAAAAAP-guBsvwg7e7vMGsuQ8REFOD0cO"
            data-callback="onSubmit">
        Login/Create account
    </button>
</form>
</body>
</html>
        """
}