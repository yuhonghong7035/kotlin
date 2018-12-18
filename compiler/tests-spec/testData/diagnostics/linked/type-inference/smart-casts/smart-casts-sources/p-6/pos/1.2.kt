// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 6 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Nullability condition (value & reference equality) smartcast source using if expression and simple builtin and custom types for one variable.
 * UNSPECIFIED BEHAVIOR
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
    if (x != null || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    }
}

// TESTCASE NUMBER: 2
object A2 {
    object B2 {
        object C2 {
            object D2 {
                object E2 {
                    object F2 {
                        object G2 {
                            object H2 {
                                object I2 {
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

fun case_2(a: A2.B2.C2.D2.E2.F2.G2.H2.I2?) =
    if (false || a != null == true == false == false == false == true == false == true == false == false == true == true || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("A2.B2.C2.D2.E2.F2.G2.H2.I2? & A2.B2.C2.D2.E2.F2.G2.H2.I2"), DEBUG_INFO_SMARTCAST!>a<!>.x
    } else -1

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28328
 */
fun case_3(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else null
    }

    val y = if (b) x else null

    if (false || false || false || false || y !== null) {
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("case_3.<anonymous>.<no name provided>?")!><!UNSAFE_CALL!>y<!>()<!>

        if (z != null || false) {
            <!DEBUG_INFO_EXPRESSION_TYPE("case_3.<anonymous>.<no name provided>? & case_3.<anonymous>.<no name provided>"), DEBUG_INFO_SMARTCAST!>z<!>.a
        }
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: ((Float) -> Int?)?, b: Float?) {
    if (a != null == true && b != null == true || false || false || false || false || false || false || false || false || false) {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("\(\(kotlin.Float\) -> kotlin.Int?\)? & \(kotlin.Float\) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (false || x != null == true) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        }
    }
}

// TESTCASE NUMBER: 5
fun case_5(b: Boolean) {
    val a = if (b) {
        object {
            val B5 = if (b) {
                object {
                    val C5 = if (b) {
                        object {
                            val D5 = if (b) {
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

    if (a != null && <!DEBUG_INFO_SMARTCAST!>a<!>.B5 != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5 != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5<!>.D5 != null && <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5<!>.D5<!>.x != null && b || false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B5<!>.C5<!>.D5<!>.x<!>
    }
}

// TESTCASE NUMBER: 6
enum class A6(val c: Int?) {
    A(1),
    B(5),
    D(null)
}

fun case_6(z: Boolean?) {
    if (false || A6.B.c != null && z != null && <!DEBUG_INFO_SMARTCAST!>z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>A6.B.c<!>
    }
}

// TESTCASE NUMBER: 7
object A7 {
    object B7 {
        object C7 {
            object D7 {
                object E7 {
                    object F7 {
                        object G7 {
                            object H7 {
                                object I7 {
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

fun case_7(a: A7.B7.C7.D7.E7.F7.G7.H7.I7?) {
    val g = false

    if (a != null && g) {
        <!DEBUG_INFO_EXPRESSION_TYPE("A7.B7.C7.D7.E7.F7.G7.H7.I7 & A7.B7.C7.D7.E7.F7.G7.H7.I7?")!>a<!>
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
class A8 {
    val x: othertypes.B<!REDUNDANT_NULLABLE!>?<!>
    init {
        x = othertypes.B()
    }
}

fun case_8(b: Boolean, c: Boolean?) {
    val a = A8()

    if (a.x !== null && false) {
        if (false || false || false || false || <!SENSELESS_COMPARISON!>a.x != null<!> || false || false || false) {
            if (<!SENSELESS_COMPARISON!>a.x !== null<!> && true) {
                if (<!SENSELESS_COMPARISON!>a.x != null<!> && b) {
                    if (<!SENSELESS_COMPARISON!>a.x != null<!> && b && !b) {
                        if (<!SENSELESS_COMPARISON!>a.x != null<!> && c != null && !<!DEBUG_INFO_SMARTCAST!>c<!>) {
                            if (<!SENSELESS_COMPARISON!>a.x !== null<!> && <!DEBUG_INFO_SMARTCAST!>c<!>) {
                                if (<!SENSELESS_COMPARISON!>a.x != null<!> && b && b && b && b && b && b && b && b && b && b && b) {
                                    if (<!SENSELESS_COMPARISON!>a.x != null<!> && !b && !b && !b && !b && !b && !b && !b && !b && !b) {
                                        if (<!SENSELESS_COMPARISON!>a.x !== null<!> && <!SENSELESS_COMPARISON!>null == null<!>) {
                                            if (<!SENSELESS_COMPARISON!>a.x != null<!> && null!!) {
                                                if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
                                                    if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
                                                        if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
                                                            if (<!SENSELESS_COMPARISON!>a.x != null<!>) {
                                                                if (<!SENSELESS_COMPARISON!>a.x !== null<!>) {
                                                                    <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.B /* = kotlin.String */ & othertypes.B? /* = kotlin.String? */")!>a.x<!>
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: Any?) {
    if (x == null || false || false || false || false || false || false) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>
    }
}

// TESTCASE NUMBER: 10
object A10 {
    object B10 {
        object C10 {
            object D10 {
                object E10 {
                    object F10 {
                        object G10 {
                            object H10 {
                                object I10 {
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

fun case_10(a: A10.B10.C10.D10.E10.F10.G10.H10.I10?) =
    if (a == null == true == false == false == false == true == false == true == false == false == true == true && true) {
        -1
    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("A10.B10.C10.D10.E10.F10.G10.H10.I10? & A10.B10.C10.D10.E10.F10.G10.H10.I10"), DEBUG_INFO_SMARTCAST!>a<!>.x
    }

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28328
 */
fun case_11(b: Boolean) {
    val x = {
        if (b) object {
            val a = 10
        } else null
    }

    val y = if (b) x else null

    if (y === null && true) else {
        val z = <!DEBUG_INFO_EXPRESSION_TYPE("case_11.<anonymous>.<no name provided>?")!><!UNSAFE_CALL!>y<!>()<!>

        if (z != null || b) {

        } else {
            <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("case_11.<anonymous>.<no name provided>? & kotlin.Nothing?")!>z<!>
        }
    }
}

// TESTCASE NUMBER: 12
fun case_12(a: ((Float) -> Int?)?, b: Float?, c: Boolean?) {
    if (true && a == null == true || b == null == true) {

    } else {
        val x = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!><!DEBUG_INFO_EXPRESSION_TYPE("\(\(kotlin.Float\) -> kotlin.Int?\)? & \(kotlin.Float\) -> kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Float & kotlin.Float?"), DEBUG_INFO_SMARTCAST!>b<!>)<!>
        if (x == null == true || (c != null && !<!DEBUG_INFO_SMARTCAST!>c<!>)) {

        } else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>
        }
    }
}

// TESTCASE NUMBER: 13
fun case_13(b: Boolean, c: Boolean, d: Boolean) {
    val a = if (b) {
        object {
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

    if ((a == null || <!DEBUG_INFO_SMARTCAST!>a<!>.B19 == null || <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19 == null || <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19 == null || <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x == null || b || c || !d) && true) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?"), DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>.B19<!>.C19<!>.D19<!>.x<!>
    }
}

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28329
 */
enum class A14(val c: Int?) {
    A(1),
    B(5),
    D(null)
}

fun case_14(z: Boolean?) {
    if (true && true && true && true && A14.B.c != null || z != null || <!ALWAYS_NULL!>z<!>!! && true && true) {

    } else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>A14.B.c<!>
    }
}

// TESTCASE NUMBER: 15
object A15 {
    object B15 {
        object C15 {
            object D15 {
                object E15 {
                    object F15 {
                        object G15 {
                            object H15 {
                                object I15 {
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

fun case_15(a: A15.B15.C15.D15.E15.F15.G15.H15.I15?) {
    val g = false

    if (true && a != null || g || !g || true || !true) {

    } else {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("A15.B15.C15.D15.E15.F15.G15.H15.I15? & kotlin.Nothing?")!>a<!>
    }
}

// TESTCASE NUMBER: 16
class A16 {
    val x: othertypes.B<!REDUNDANT_NULLABLE!>?<!>
    init {
        x = othertypes.B()
    }
}

fun case_16(b: Boolean, c: Boolean?) {
    val a = A16()

    if (a.x != null && false && false && false && false && false && false) {
        if ( <!SENSELESS_COMPARISON!>a.x == null<!> || false) {
        } else {
            if ( <!SENSELESS_COMPARISON!>a.x === null<!> && true) {
            } else {
                if (<!SENSELESS_COMPARISON!>a.x == null<!> || !b) {
                } else {
                    if (<!SENSELESS_COMPARISON!>a.x == null<!> || b || !b) {
                    } else {
                        if (<!SENSELESS_COMPARISON!>a.x == null<!> || c == null || !<!DEBUG_INFO_SMARTCAST!>c<!>) {
                        } else {
                            if (<!SENSELESS_COMPARISON!>a.x === null<!> || <!DEBUG_INFO_SMARTCAST!>c<!>) {
                            } else {
                                if (<!SENSELESS_COMPARISON!>a.x == null<!> || b || b || b || b || b || b || b || b || b || b || b) {
                                } else {
                                    if (<!SENSELESS_COMPARISON!>a.x == null<!> || !b || !b || !b || !b || !b || !b || !b || !b || !b) {
                                    } else {
                                        if (<!SENSELESS_COMPARISON!>a.x === null<!> || <!SENSELESS_COMPARISON!>null == null<!>) {
                                        } else {
                                            if (<!SENSELESS_COMPARISON!>a.x == null<!> || null!!) {
                                            } else {
                                                if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                } else {
                                                    if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                    } else {
                                                        if (<!SENSELESS_COMPARISON!>a.x === null<!>) {
                                                        } else {
                                                            if (<!SENSELESS_COMPARISON!>a.x == null<!>) {
                                                            } else {
                                                                if (<!SENSELESS_COMPARISON!>a.x === null<!>) {
                                                                } else {
                                                                    <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.B /* = kotlin.String */ & othertypes.B? /* = kotlin.String? */"), DEBUG_INFO_SMARTCAST!>a.x<!>
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// TESTCASE NUMBER: 17
fun case_17(a: Int?, b: Int = if (a != null) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!> else 0) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>
}

// TESTCASE NUMBER: 18
fun case_18(a: Int?, b: Int = if (false || a != null || false) <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>a<!> else 0) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>b<!>
}
