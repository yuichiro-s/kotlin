package kotlin.js

public external val noImpl: Nothing

public external val definedExternally: Nothing

public external fun eval(expr: String): dynamic

public external val undefined: Nothing?


public external fun parseInt(s: String, radix: Int = definedExternally): Int

public external fun parseFloat(s: String, radix: Int = definedExternally): Double

public external fun js(code: String): dynamic

/**
 * Function corresponding to JavaScript's `typeof` operator
 */
public inline fun jsTypeOf(a: Any?): String = js("typeof a")

internal inline fun deleteProperty(obj: Any, property: Any) {
    js("delete obj[property]")
}