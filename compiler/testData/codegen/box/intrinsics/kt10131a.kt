// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND_WITHOUT_CHECK: JS

// WITH_RUNTIME

fun box(): String =
        charArrayOf('O', 'K').fold("", String::plus)
