// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +WarnOnCoroutines

class Controller {
    <!EXPERIMENTAL_FEATURE_WARNING!>suspend<!> fun suspendHere(): String = "OK"

    <!EXPERIMENTAL_FEATURE_WARNING!>operator<!> fun handleResult(x: String, y: Continuation<Nothing>) {}

    <!EXPERIMENTAL_FEATURE_WARNING!>operator<!> fun handleException(x: Throwable, y: Continuation<Nothing>) {
    }

    <!EXPERIMENTAL_FEATURE_WARNING!>operator<!> fun interceptResume(x: () -> Unit) {
    }
}

fun builder(<!EXPERIMENTAL_FEATURE_WARNING!>coroutine<!> c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    <!EXPERIMENTAL_FEATURE_WARNING!>builder<!> {
        suspendHere()
    }

    return result
}
