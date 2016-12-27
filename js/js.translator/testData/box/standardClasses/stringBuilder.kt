package foo

//
//inline fun foo(): Char? {
//    return 'K'
//}

fun foo(c: Any): Boolean {
    return c is Char
}

fun box(): String {
    val a7: Char? = 'A'.plus(1)
    if (a7!! != 'B') return "fail 7"

    if (!foo('c')) return "fail is"

    val q : Any = 's'

    if (q != 's') return "fail assignment"

    if ('O'.toString() + "K" != "OK") return "fail toString"

    if (('O' as Any) != 'O') return "fail eq"

//
//    if (!' '.isWhitespace()) return "fail white"
//
//    var q = "O${foo()}"
//    if (q != "OK") return "fail122"
//
//    val s = StringBuilder()
//    s.append("a")
//    s.append("b").append("c")
//    s.append('d').append("e")
//
//    if (s.toString() != "abcde") return s.toString()
    return "OK"
}