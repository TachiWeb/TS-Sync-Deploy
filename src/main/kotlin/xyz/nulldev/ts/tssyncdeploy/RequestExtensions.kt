package xyz.nulldev.ts.tssyncdeploy

import spark.Response

fun Response.disableCache() = header("Cache-Control", "no-cache, no-store, must-revalidate")