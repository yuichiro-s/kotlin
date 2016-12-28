external class TypeError(message: String?, fileName: String? = noImpl, lineNumber: Int? = noImpl) : Throwable

fun box(): String {
    try {
        js("null.foo()")
        return "fail: expected exception not thrown"
    }
    catch (e: TypeError) {
        return "OK"
    }
}