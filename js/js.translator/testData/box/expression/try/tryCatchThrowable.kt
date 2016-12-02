package foo

fun box(): String {

    var s: String = ""

    try {
        throw Exception()
    } catch (e: Throwable) {
        s = "Throwable"
    }
    assertEquals("Throwable", s)

    try {
        js("throw null")
    } catch (e: Throwable) {
        s = "Throwable"
    } catch (e: dynamic) {
        s = "dynamic"
    }
    assertEquals("dynamic", s)


    try {
        try {
            js("throw null")
        }
        catch (e: Throwable) {
            s = "Throwable"
        }
    } catch (e: dynamic) {
        s = "dynamic2"
    }
    assertEquals("dynamic2", s)


    try {
        js("throw Object.create(null)")
    }
    catch (e: dynamic) {
        s = "dynamic3"
    }
    assertEquals("dynamic3", s)

    return "OK"
}