// MODULE: lib
// FILE: lib.kt

package utils

inline fun <reified T> rrr(f: Any.()->String): String {
    return 4.f()
}

inline fun <reified T> zzz(f: ()->String): String {
    return f()
}

inline fun <reified T, reified K> Any.xxx(f: Any.()->String, g: () -> String): String {
    return 4.f() + g()
}

// MODULE: main(lib)
// FILE: main.kt

import utils.*

fun box(): String {
    if (rrr<Any> { "O" } + zzz<Any> { "K" } != "OK") return "fail1"
    if (0.xxx<Int, String>({"O"}, {"K"}) != "OK") return "fail2"

    return "OK"
}