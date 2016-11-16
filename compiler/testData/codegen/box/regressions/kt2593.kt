// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND_WITHOUT_CHECK: JS

// WITH_RUNTIME

fun foo() {
    if (1==1) {
        1.javaClass
    } else {
    }
}

fun box() = "OK"
