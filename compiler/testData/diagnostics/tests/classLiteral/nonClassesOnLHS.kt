// !DIAGNOSTICS: -UNUSED_VARIABLE

class A

val <!SPECIFY_TYPE_EXPLICITLY!>a1<!> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A?::class<!>
val <!SPECIFY_TYPE_EXPLICITLY!>a2<!> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A??::class<!>

val <!SPECIFY_TYPE_EXPLICITLY!>l1<!> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>List<String>?::class<!>
val <!SPECIFY_TYPE_EXPLICITLY!>l2<!> = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>List?::class<!>

fun <T : Any> foo() {
    val t1 = <!TYPE_PARAMETER_AS_REIFIED!>T::class<!>
    val t2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>T?::class<!>
}

inline fun <reified T : Any> bar() {
    val t3 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>T?::class<!>
}

val m = Map<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>::class
