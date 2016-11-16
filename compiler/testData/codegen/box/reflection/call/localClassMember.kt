// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND_WITHOUT_CHECK: JS

// WITH_REFLECT

fun box(): String {
    class Local {
        fun result(s: String) = s
    }

    return Local::result.call(Local(), "OK")
}
