// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +ErrorOnCoroutines

class Controller {
    <!EXPERIMENTAL_FEATURE_ERROR!>suspend<!> fun suspendHere(): String = "OK"

    <!EXPERIMENTAL_FEATURE_ERROR!>operator<!> fun handleResult(x: String, y: Continuation<Nothing>) {}

    <!EXPERIMENTAL_FEATURE_ERROR!>operator<!> fun handleException(x: Throwable, y: Continuation<Nothing>) {
    }

    <!EXPERIMENTAL_FEATURE_ERROR!>operator<!> fun interceptResume(x: () -> Unit) {
    }
}

fun builder(<!EXPERIMENTAL_FEATURE_ERROR!>coroutine<!> c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    <!EXPERIMENTAL_FEATURE_ERROR!>builder<!> {
        suspendHere()
    }

    return result
}
