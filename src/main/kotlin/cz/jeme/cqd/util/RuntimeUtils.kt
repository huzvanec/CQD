package cz.jeme.cqd.util

fun exec(vararg commands: String): Process = Runtime.getRuntime().exec(commands)

fun sh(command: String) = exec("sh", "-c", command)