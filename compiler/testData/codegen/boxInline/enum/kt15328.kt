// FILE: 1.kt
// WITH_RUNTIME
package test

inline fun <reified E : Enum<E>> relaxedStringToEnum(s: String?, default: E): E =
        enumValues<E>().find {it.name.toUpperCase() == s?.toUpperCase()}
        ?: default

enum class Z {
    O, K;

    val myParam = name
}


// FILE: 2.kt

import test.*

fun box(): String {
    return relaxedStringToEnum("o", Z.K).myParam + relaxedStringToEnum(null, Z.K).myParam
}

