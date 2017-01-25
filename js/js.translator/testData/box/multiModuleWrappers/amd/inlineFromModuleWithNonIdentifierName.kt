// MODULE: #my-libr@ry
// FILE: bar.kt
// MODULE_KIND: AMD
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

inline fun foo() = "foo"
@kotlin.internal.InlineOnly
inline fun baz() = "baz"
inline fun <reified T> boo() = "boo"

// MODULE: main(#my-libr@ry)
// FILE: box.kt
// MODULE_KIND: AMD
// CHECK_CONTAINS_NO_CALLS: box except=assertEquals

fun box(): String {
    assertEquals("foo", foo())
    assertEquals("baz", baz())
    assertEquals("boo", boo<Int>())
    return "OK"
}
