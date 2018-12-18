// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 6 -> sentence 1
 * NUMBER: 7
 * DESCRIPTION: Multi smartcasts
 * UNEXPECTED BEHAVIOUR
 */

// FILE: other_types.kt

package othertypes

// TESTCASE NUMBER: 13
class A {}

// TESTCASE NUMBER: 14
typealias B = String?

// TESTCASE NUMBER: 16
typealias C = Nothing

// FILE: main.kt

import othertypes.*

// TESTCASE NUMBER: 6
class A {}

// TESTCASE NUMBER: 7
object B {}

// TESTCASE NUMBER: 8
typealias C = String?

// TESTCASE NUMBER: 10
typealias D = Float

// TESTCASE NUMBER: 11
typealias E = C

// TESTCASE NUMBER: 15
typealias F = othertypes.B

// TESTCASE NUMBER: 16
typealias G = othertypes.C?

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x != null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28159
 */
fun case_2(x: Nothing?) {
    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!> && <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    }
}

// TESTCASE NUMBER: 3
object A3 {
    val x: Number? = null
}

fun case_3() {
    if (A3.x == null && <!SENSELESS_COMPARISON!>A3.x == null<!>)
    else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>A3.x<!>
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Char?) {
    if (x != null && true || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    val x: Unit? = null

    if (x !== null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!> && <!SENSELESS_COMPARISON!>x !== null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!> && <!SENSELESS_COMPARISON!>x !== null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>
}

// TESTCASE NUMBER: 6
fun case_6(x: A?) {
    val y = true

    if (((false || x != null || false) && !y) || x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("A & A?")!>x<!>
    }
}

// TESTCASE NUMBER: 7
val x7: B? = null

fun case_7() {
    if (x7 != null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x7<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("B? & kotlin.Nothing?")!>x7<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("B & B?")!>x7<!>
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: C) {
    if (x !== null && <!SENSELESS_COMPARISON!><!DEBUG_INFO_EXPRESSION_TYPE("C /* = kotlin.String */ & C /* = kotlin.String? */")!>x<!> != null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("C /* = kotlin.String */ & C /* = kotlin.String? */")!>x<!>
}

// TESTCASE NUMBER: 9
fun case_9(x: C<!REDUNDANT_NULLABLE!>?<!>) {
    if (x === null && <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> === null<!> || <!SENSELESS_COMPARISON!>x === null<!>) {

    } else if (<!SENSELESS_COMPARISON!>x === null<!> || <!SENSELESS_COMPARISON!>x === null<!>) {
    } else if (false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("C /* = kotlin.String */ & C? /* = kotlin.String? */")!>x<!>
    }
}

// TESTCASE NUMBER: 10
class A10 {
    val x: D? = null
}

fun case_10() {
    val a = A10()

    if (a.x === null || <!SENSELESS_COMPARISON!>a.x === null<!> || true) {
        if (a.x != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("D /* = kotlin.Float */ & D? /* = kotlin.Float? */")!>a.x<!>
        }
    }
}

// TESTCASE NUMBER: 11
val x11: E = null
fun case_11(x: E<!REDUNDANT_NULLABLE!>?<!>, y: E) {
    val t: E = null

    if (x == null) {

    } else {
        if (<!SENSELESS_COMPARISON!>x != null<!>) {
            if (y != null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>y<!> != null<!>) {
                if (x11 == null && <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x11<!> == null<!>) {
                    if (t != null || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>t<!> != null<!>) {
                        <!DEBUG_INFO_EXPRESSION_TYPE("E /* = kotlin.String */ & E? /* = kotlin.String? */")!>x<!>
                    }
                }
            }
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(x: E, y: E) = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (x == null) "1"
    else if (y === null || <!SENSELESS_COMPARISON!>y === null<!>) <!DEBUG_INFO_EXPRESSION_TYPE("E /* = kotlin.String */ & E /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>
    else "-1"<!>

// TESTCASE NUMBER: 13
fun case_13(x: othertypes.A?, y: Nothing?) =
<!DEBUG_INFO_EXPRESSION_TYPE("othertypes.A")!>if (x == null || x === <!DEBUG_INFO_CONSTANT!>y<!>) {
    throw Exception()
} else <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.A & othertypes.A?"), DEBUG_INFO_SMARTCAST!>x<!><!>

// TESTCASE NUMBER: 14
class A14 {
    val x: othertypes.B<!REDUNDANT_NULLABLE!>?<!>
    var y = null
    init {
        x = othertypes.B()
    }
}

fun case_14() {
    val a = A14()
    if (a.x != null) {
        if (a.x != a.y) {
            <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.B /* = kotlin.String */ & othertypes.B? /* = kotlin.String? */")!>a.x<!>
        }
    }
}

// TESTCASE NUMBER: 15
fun case_15(x: F) {
    var y = null
    val <!UNUSED_VARIABLE!>t<!> = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>if (x === null || x == <!DEBUG_INFO_CONSTANT!>y<!> && x === <!DEBUG_INFO_CONSTANT!>y<!>) "" else {
        <!DEBUG_INFO_EXPRESSION_TYPE("F /* = kotlin.String */ & F /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>x<!>
    }<!>
}

// TESTCASE NUMBER: 16
fun case_16() {
    val x: G = null
    val y: Nothing? = null

    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!> || <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> !== null<!> || <!DEBUG_INFO_CONSTANT!>x<!> != <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("G /* = kotlin.Nothing? */")!>x<!>
    }
}

// TESTCASE NUMBER: 17
val x17: Int? = 1
val y17: Nothing? = null

val t = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>if (x17 == null || <!DEBUG_INFO_CONSTANT!>y17<!> === x17) 0 else {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x17<!>
}<!>

/*
 * TESTCASE NUMBER: 18
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28328
 */
object A18 {
    object B18 {
        object C18 {
            object D18 {
                object E18 {
                    object F18 {
                        object G18 {
                            object H18 {
                                object I18 {
                                    val x: Int? = 10
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun case_18(a: A18.B18.C18.D18.E18.F18.G18.H18.I18?, b: Nothing?) {
    if (a != null || <!DEBUG_INFO_CONSTANT!>b<!> !== <!DEBUG_INFO_CONSTANT!>a<!> || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("A18.B18.C18.D18.E18.F18.G18.H18.I18?")!>a<!>
    }
}

// TESTCASE NUMBER: 19
fun case_19(b: Boolean) {
    val a = if (b) {
        object {
            var y = null
            val B19 = if (b) {
                object {
                    val C19 = if (b) {
                        object {
                            val D19 = if (b) {
                                object {
                                    val x: Number? = 10
                                }
                            } else null
                        }
                    } else null
                }
            } else null
        }
    } else null

    if (a != null && <!DEBUG_INFO_SMARTCAST!>a<!>.B19 != <!DEBUG_INFO_SMARTCAST!>a<!>.y && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19 != <!DEBUG_INFO_SMARTCAST!>a<!>.y && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19 != <!DEBUG_INFO_SMARTCAST!>a<!>.y && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x != <!DEBUG_INFO_SMARTCAST!>a<!>.y) {
        <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x
    }
}

// TESTCASE NUMBER: 20
fun case_20(b: Boolean) {
    val a = object {
        val y = null
        val B19 = object {
            val C19 = object {
                val D19 =  if (b) {
                    object {}
                } else null
            }
        }
    }

    if (a.B19.C19.D19 !== null || a.y != a.B19.C19.D19) {
        <!DEBUG_INFO_EXPRESSION_TYPE("case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided> & case_20.<no name provided>.B19.<no name provided>.C19.<no name provided>.D19.<no name provided>?")!>a.B19.C19.D19<!>
    }
}

// TESTCASE NUMBER: 21
enum class A21(val c: Int?) {
    A(1),
    B(5),
    D(null)
}

fun case_21() {
    val y = null
    if (A21.B.c !== null && <!DEBUG_INFO_CONSTANT!>y<!> != A21.B.c) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>A21.B.c<!>
    }
}

// TESTCASE NUMBER: 22
fun case_22(a: (() -> Unit)?) {
    var y = null
    if (a != null || <!DEBUG_INFO_CONSTANT!>y<!> != <!DEBUG_INFO_CONSTANT!>a<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!><!DEBUG_INFO_EXPRESSION_TYPE("\(\(\) -> kotlin.Unit\)? & \(\) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>()<!>
    }
}

// TESTCASE NUMBER: 23
fun case_23(a: ((Float) -> Int?)?, b: Float?, c: Nothing?) {
    if (a != null && b !== null || a != <!DEBUG_INFO_CONSTANT!>c<!> && b !== <!DEBUG_INFO_CONSTANT!>c<!>) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("\(\(kotlin.Float\) -> kotlin.Int?\)? & \(kotlin.Float\) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x != null || <!DEBUG_INFO_CONSTANT!>c<!> !== <!DEBUG_INFO_CONSTANT!>x<!>) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        }
    }
}

// TESTCASE NUMBER: 24
var y24 = null

fun case_24(a: ((() -> Unit) -> Unit)?, b: (() -> Unit)?) =
    if (a !== null && b !== null || <!DEBUG_INFO_CONSTANT!>y24<!> != a && <!DEBUG_INFO_CONSTANT!>y24<!> !== b) {
        <!DEBUG_INFO_EXPRESSION_TYPE("\(\(\(\) -> kotlin.Unit\) -> kotlin.Unit\)? & \(\(\) -> kotlin.Unit\) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("\(\(\) -> kotlin.Unit\)? & \(\) -> kotlin.Unit"), DEBUG_INFO_SMARTCAST!>b<!>)
    } else null

// TESTCASE NUMBER: 25
fun case_25(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
            val b = null
        } else null
    }

    val y = if (b) x else null

    if (y !== null || x()!!.b != <!DEBUG_INFO_CONSTANT!>y<!>) {
        if (x()!!.b != y) {
            val z = <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>?")!><!DEBUG_INFO_EXPRESSION_TYPE("\(\(\) -> case_25.<anonymous>.<no name provided>?\)? & \(\) -> case_25.<anonymous>.<no name provided>?"), DEBUG_INFO_SMARTCAST!>y<!>()<!>

            if (z != null) {
                <!DEBUG_INFO_EXPRESSION_TYPE("case_25.<anonymous>.<no name provided>? & case_25.<anonymous>.<no name provided>"), DEBUG_INFO_SMARTCAST!>z<!>.a
            }
        }
    }
}

// TESTCASE NUMBER: 26
fun case_26(a: ((Float) -> Int?)?, b: Float?) {
    var c: Nothing? = null

    if (a != null == true && b != null == true || <!DEBUG_INFO_CONSTANT!>c<!> != a == true && b != <!DEBUG_INFO_CONSTANT!>c<!> == true) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("\(\(kotlin.Float\) -> kotlin.Int?\)? & \(kotlin.Float\) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x != null == true) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        }
    }
}

// TESTCASE NUMBER: 27
fun case_27(y: Nothing?) {
    if (A3.x == null == true == true == true == true == true == true == true == true == true == true == true == true == true == true || <!DEBUG_INFO_CONSTANT!>y<!> == A3.x == true == true == true == false == false)
    else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>A3.x<!>
    }
}

//TESTCASE NUMBER: 28
object A28 {
    object B28 {
        object C28 {
            object D28 {
                object E28 {
                    object F28 {
                        object G28 {
                            object H28 {
                                val y: Nothing? = null
                                object I28 {
                                    val x: Int? = 10
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun case_28(a: A28.B28.C28.D28.E28.F28.G28.H28.I28?) =
    if (a != null == true == false == false == false == true == false == true == false == false == true == true || A28.B28.C28.D28.E28.F28.G28.H28.y !== <!DEBUG_INFO_CONSTANT!>a<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("A28.B28.C28.D28.E28.F28.G28.H28.I28? & A28.B28.C28.D28.E28.F28.G28.H28.I28"), DEBUG_INFO_SMARTCAST!>a<!>.x
    } else -1

// TESTCASE NUMBER: 29
fun case_29(a: Int?, b: Nothing?, <!UNUSED_PARAMETER!>c<!>: Int = if (a === <!DEBUG_INFO_CONSTANT!>b<!>) 0 else <!DEBUG_INFO_SMARTCAST!>a<!>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>b<!>
}

// TESTCASE NUMBER: 30
fun case_30(a: Int?, b: Nothing?, <!UNUSED_PARAMETER!>c<!>: Int = if (a === <!DEBUG_INFO_CONSTANT!>b<!> || <!SENSELESS_COMPARISON!>a == null<!>) 0 else <!DEBUG_INFO_SMARTCAST!>a<!>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>b<!>
}
