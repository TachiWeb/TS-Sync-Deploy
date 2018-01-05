package xyz.nulldev.ts.tssyncdeploy

import com.kizitonwose.time.hours
import com.kizitonwose.time.milliseconds
import com.kizitonwose.time.minutes
import com.kizitonwose.time.schedule
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.withLock

object AccountManager {
    val vms = mutableListOf<VM>()
    private val accounts = ConcurrentHashMap<String, Account>()

    val VALID_USERNAME_CHARS = "abcdefghijklmnopqrstuvwxyz".let {
        it + it.toUpperCase() + "_-@" + "0123456789"
    }.toCharArray()
    val MAX_USERNAME_LENGTH = 100

    fun confAccount(account: String, password: String) {
        lockAccDown(account) {
            //Write config
            it.configFolder.mkdirs()
            val configFile = File(it.configFolder, "server.config")
            configFile.writeText(DEFAULT_CONFIG)

            //Conf password
            confAccountPw(account, password)
        }
    }

    fun confAccountPw(account: String, password: String) {
        lockAcc(account) {
            //Write password
            it.pwFile.writeText(if (password.isNotEmpty())
                PasswordHasher.getSaltedHash(password)
            else "")
        }
    }

    fun authAccount(account: String, password: String?): Boolean {
        return lockAcc(account) {
            val hash = it.pwFile.readText().trim()

            if(password == null || password.isEmpty())
                hash.isEmpty()
            else
                hash.isNotEmpty() && PasswordHasher.check(password, hash)
        }
    }

    fun authToken(account: String, token: String?): Boolean {
        return lockAcc(account) {
            if(token != null && token.isNotEmpty())
                it.token.toString() == token.trim()
            else {
                val hash = it.pwFile.readText().trim()

                return hash.isEmpty()
            }
        }
    }

    @Synchronized
    fun getAccount(account: String)
            = synchronized(accounts) {
        accounts.getOrPut(account, {
            Account(account)
        })
    }

    inline fun <T> lockAcc(account: String, block: (Account) -> T): T
            = getAccount(account).let {
        val res = it.lock.withLock {
            block(it)
        }
        it.lastUsedTime = System.currentTimeMillis()
        res
    }

    inline fun <T> lockAccUp(account: String, block: (Account, VM) -> T)
            = lockAcc(account) {
        val vm = allocateVm(account)
        block(it, vm)
        vm.lastUsedTime = System.currentTimeMillis()
    }

    inline fun <T> lockAccDown(account: String, block: (Account) -> T)
            = lockAcc(account) {
        var vm: VM? = null

        //Find running VM
        synchronized(vms) {
            vm = vms.find { it.account == account }
        }

        //Found, shut it down
        if(vm != null) {
            vm!!.shutdown()

            //Remove VM from running list
            synchronized(vms) {
                vms.remove(vm!!)
            }
        }

        block(it)
    }

    //true if shutdown, false if not
    fun tryShutdownVm(): Boolean {
        val vm = synchronized(vms) {
            vms.sortedBy(VM::lastUsedTime).forEach {
                val lockOk = getAccount(it.account).lock.tryLock()
                if(lockOk) return@synchronized it
            }
            return@synchronized null
        }

        if(vm != null) {
            val account = getAccount(vm.account)
            try {
                //Shutdown VM
                vm.shutdown()

                //Remove VM from running list
                synchronized(vms) {
                    vms.remove(vm)
                }

                return true
            } finally {
                //VM != null means we have account, thus we unlock account when we are done
                account.lock.unlock()
            }
        }

        return false
    }

    fun allocateVm(account: String): VM {
        return lockAcc(account) {
            while(true) {
                var toInit: VM? = null

                synchronized(vms) {
                    //Find existing VM
                    var vm = vms.find { it.account == account }

                    if (vm == null) {
                        //Too many VMs, wait to try again
                        if(vms.size >= MAX_VMS)
                            return@synchronized

                        //Init VM
                        vm = VM(account)
                        vms.add(vm)

                        //Init VM after out of synchronized block
                        toInit = vm
                    } else return@lockAcc vm
                }

                if(toInit != null) {
                    toInit!!.startup()
                    return@lockAcc toInit
                }

                //Shutdown existing VM
                if(!tryShutdownVm())
                    Thread.sleep(100) //Nothing shutdown, wait...
            }
        } as VM
    }

    fun allocateVmFast(account: String): VM? {
        return lockAcc(account) {
            var toInit: VM? = null

            synchronized(vms) {
                //Find existing VM
                var vm = vms.find { it.account == account }

                if (vm == null) {
                    //Too many VMs, stop
                    if(vms.size >= MAX_VMS)
                        return@synchronized

                    //Init VM
                    vm = VM(account)
                    vms.add(vm)

                    //Init VM after out of synchronized block
                    toInit = vm
                } else return@lockAcc vm
            }

            if(toInit != null) {
                toInit!!.startup()
                return@lockAcc toInit
            }

            //Shutdown existing VM
            tryShutdownVm()

            return@lockAcc null
        }
    }

    val MAX_VMS = 3

    val INIT_MESSAGE = "TSSyncDeploy Server Initialized!"

    private val DEFAULT_CONFIG = """
|ts.server.ip = 127.0.0.1
|ts.server.allowConfigChanges = false
|ts.server.enableWebUi = false
|ts.server.disabledApiEndpoints = []
|ts.server.enabledApiEndpoints = ["sync"]
|ts.server.httpInitializedPrintMessage = "$INIT_MESSAGE"
    """.trimMargin()

    private val ACCOUNT_REMOVAL_TIMEOUT = 1.hours

    private val ports = mutableListOf<Int>()

    fun reservePort(): Int {
        synchronized(ports) {
            val port = (0 .. 100).find { it !in ports }
                    ?: throw IllegalStateException("No more ports available!")
            ports.add(port)
            return port + 10000
        }
    }

    fun releasePort(port: Int) {
        synchronized(ports) {
            ports.remove(port)
        }
    }

    init {
        val timer = Timer()
        timer.schedule(1.minutes, 1.minutes) {
            println("Reaping accounts...")
            synchronized(accounts) {
                val toRemove = mutableListOf<Account>()
                accounts.forEach { _: String, u: Account ->
                    if(u.lock.tryLock()) {
                        val diff = System.currentTimeMillis() - u.lastUsedTime
                        if (diff.milliseconds > ACCOUNT_REMOVAL_TIMEOUT)
                            toRemove.add(u) //Keep account locked
                        else u.lock.unlock()
                    }
                }
                toRemove.forEach {
                    accounts.remove(it.name)
                    it.lock.unlock()
                }
            }
        }

        //TODO VM reaper
    }
}