package cz.jeme.cqd

import cz.jeme.cqd.util.ANSI
import cz.jeme.cqd.util.sh

fun main() {
    print(ANSI.ENABLE_ALTERNATE_SCREEN_BUFFER)
    sh("stty raw -echo < /dev/tty").waitFor() == 0 || throw RuntimeException("Failed to set terminal to raw mode")

    try {
        main0()
    } finally {
        print(ANSI.DISABLE_ALTERNATE_SCREEN_BUFFER)
        sh("stty cooked echo < /dev/tty").waitFor()
    }
}

fun main0() {
    print("${ANSI.goto(10, 10)}Hello, World!") // move to row 10, column 10
    System.out.flush()
    while (true) {
        val b = System.`in`.read()
        if (b == 3 || b == 'q'.code) break // Ctrl+C or 'q' quits
    }
}