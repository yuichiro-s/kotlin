// MODULE: lib
// FILE: lib.kt

package utils

inline fun <reified T> rrr(f: Any.()->String): String {
    return 4.f()
}

inline fun <reified T> zzz(f: ()->String): String {
    return f()
}

// MODULE: main(lib)
// FILE: main.kt

import utils.*

fun box(): String {
    return rrr<Any> { "O" } + zzz<Any> { "K" }
}