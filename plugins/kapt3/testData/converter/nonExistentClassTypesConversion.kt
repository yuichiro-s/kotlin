// NON_EXISTENT_CLASS
// NO_VALIDATION

@Suppress("UNRESOLVED_REFERENCE")
class Test {
    lateinit var a: ABC
    val b: ABC? = null
    val c: List<ABC>? = null
    val d: List<Map<BCD, ABC<List<BCD>>>>? = null
    lateinit var e: List<out Map<out ABC, out BCD>?>
    lateinit var f: ABC<*>
    lateinit var g: List<*>
    lateinit var h: ABC<Int, String>
    lateinit var i: (ABC, List<BCD>) -> CDE
    lateinit var j: () -> CDE
    lateinit var k: ABC.(List<BCD>) -> CDE
}