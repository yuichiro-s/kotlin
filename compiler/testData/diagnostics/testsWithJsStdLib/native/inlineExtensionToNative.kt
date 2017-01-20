external class A

inline fun A.foo(x: Int): String = asDynamic().foo(x)

inline operator fun A.get(x: Int): String = asDynamic()[x]