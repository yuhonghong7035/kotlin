// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Empty 'when' with bound value.
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    }
    if (x is Any) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28159
 */
fun case_2(x: Nothing?) {
    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    }
    if (<!DEBUG_INFO_CONSTANT!>x<!> is Any) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    }
    if (<!DEBUG_INFO_CONSTANT!>x<!> is Nothing) {
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 */
fun case_3(x: Number?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Number?")!>x<!>
    }
    if (x is Any) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Number & kotlin.Number?")!>x<!>
    }
}
