// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND_WITHOUT_CHECK: JS

// WITH_RUNTIME

@JvmName("bar")
fun foo() = "foo"

fun box(): String {
    val f = (::foo)()
    if (f != "foo") return "Fail: $f"

    return "OK"
}
