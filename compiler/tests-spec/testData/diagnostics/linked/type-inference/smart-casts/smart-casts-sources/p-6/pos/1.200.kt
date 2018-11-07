// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Nullability condition smartcast source using if expression and simple builtin and custom types.
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
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28159
 */
fun case_2(x: Nothing?) {
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!><!ALWAYS_NULL!>x<!>!!<!>
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?"), UNREACHABLE_CODE!>x<!>
}

// TESTCASE NUMBER: 3
fun case_3(x: Number?) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>
}

// TESTCASE NUMBER: 4
fun case_4(x: Char?) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char & kotlin.Char?")!>x<!>
}

// TESTCASE NUMBER: 5
fun case_5(x: Unit?) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>x<!>
}

// TESTCASE NUMBER: 6
fun case_6(x: A?) {
    <!DEBUG_INFO_EXPRESSION_TYPE("A?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("A")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("A & A?")!>x<!>
}

// TESTCASE NUMBER: 7
fun case_7(x: B?) {
    <!DEBUG_INFO_EXPRESSION_TYPE("B?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("B")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("B & B?")!>x<!>
}

// TESTCASE NUMBER: 8
fun case_8(x: C) {
    <!DEBUG_INFO_EXPRESSION_TYPE("C /* = kotlin.String? */")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("C /* = kotlin.String */")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("C /* = kotlin.String */ & C /* = kotlin.String? */")!>x<!>
}

// TESTCASE NUMBER: 9
fun case_9(x: C<!REDUNDANT_NULLABLE!>?<!>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("C? /* = kotlin.String? */")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("C /* = kotlin.String */")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("C /* = kotlin.String */ & C? /* = kotlin.String? */")!>x<!>
}

// TESTCASE NUMBER: 10
fun case_10(x: D?) {
    <!DEBUG_INFO_EXPRESSION_TYPE("D? /* = kotlin.Float? */")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("D /* = kotlin.Float */")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("D /* = kotlin.Float */ & D? /* = kotlin.Float? */")!>x<!>
}

// TESTCASE NUMBER: 11
fun case_11(x: E<!REDUNDANT_NULLABLE!>?<!>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("E? /* = kotlin.String? */")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("E /* = kotlin.String */")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("E /* = kotlin.String */ & E? /* = kotlin.String? */")!>x<!>
}

// TESTCASE NUMBER: 12
fun case_12(x: E) {
    <!DEBUG_INFO_EXPRESSION_TYPE("E /* = kotlin.String? */")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("E /* = kotlin.String */")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("E /* = kotlin.String */ & E /* = kotlin.String? */")!>x<!>
}

// TESTCASE NUMBER: 13
fun case_13(x: othertypes.A?) {
    <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.A?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.A")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.A & othertypes.A?")!>x<!>
}

// TESTCASE NUMBER: 14
fun case_14(x: othertypes.B<!REDUNDANT_NULLABLE!>?<!>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.B? /* = kotlin.String? */")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.B /* = kotlin.String */")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("othertypes.B /* = kotlin.String */ & othertypes.B? /* = kotlin.String? */")!>x<!>
}

// TESTCASE NUMBER: 15
fun case_15(x: F) {
    <!DEBUG_INFO_EXPRESSION_TYPE("F /* = kotlin.String? */")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("F /* = kotlin.String */")!>x!!<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("F /* = kotlin.String */ & F /* = kotlin.String? */")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("F /* = kotlin.String */")!>x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>
    <!DEBUG_INFO_EXPRESSION_TYPE("F /* = kotlin.String */ & F /* = kotlin.String? */")!>x<!>
}

/*
 * TESTCASE NUMBER: 16
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28159
 */
fun case_16(x: G) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!><!ALWAYS_NULL!>x<!>!!<!>
    <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("G /* = kotlin.Nothing? */"), UNREACHABLE_CODE!>x<!>
}



// TESTCASE NUMBER: 9
data class A9<T, K, L>(
    val x: T,
    val y: K,
    val z: L
)

fun case_9(a: A9<Int?, Int, Any>) {
    var (x, y, _) = a

    if (<!OI;IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>x === y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    }

    if (<!OI;IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>x == z<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    }
}
