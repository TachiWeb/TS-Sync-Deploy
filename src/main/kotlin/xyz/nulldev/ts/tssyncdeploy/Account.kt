package xyz.nulldev.ts.tssyncdeploy

import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock

class Account(val name: String) {
    val lock = ReentrantLock()
    val folder = File(ROOT, name)
    val configFolder = File(folder, "config")
    val pwFile = File(folder, "auth")
    val token = UUID.randomUUID()
    var lastUsedTime: Long = System.currentTimeMillis()
    val configured
        get() = folder.exists()

    companion object {
        private val ROOT = File("accounts")
    }
}