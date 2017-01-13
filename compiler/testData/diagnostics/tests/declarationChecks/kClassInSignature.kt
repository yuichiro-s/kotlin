// !DIAGNOSTICS: -TYPE_PARAMETER_AS_REIFIED -TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER -UNUSED_VARIABLE -UNUSED_PARAMETER

fun <T> <!SPECIFY_TYPE_EXPLICITLY!>test1<!>() = T::class
fun <T : Any> test2() = T::class

val <T> <!SPECIFY_TYPE_EXPLICITLY!>test3<!> = T::class
val <T> <!SPECIFY_TYPE_EXPLICITLY!>test4<!> get() = T::class

fun <T> <!SPECIFY_TYPE_EXPLICITLY!>test5<!>() = listOf(T::class)

fun <T> test6(): kotlin.reflect.KClass<<!UPPER_BOUND_VIOLATED!>T<!>> = T::class
fun <T> test7(): kotlin.reflect.KClass<*> = T::class


fun <T> listOf(e: T): List<T> = null!!

fun <L> locals() {
    fun <T> test1() = T::class
    fun <T : Any> test2() = T::class

    val test3 = L::class
    fun test4() = L::class
}