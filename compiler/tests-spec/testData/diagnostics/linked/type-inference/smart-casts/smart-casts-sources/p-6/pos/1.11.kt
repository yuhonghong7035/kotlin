// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 6 -> sentence 1
 * NUMBER: 11
 * DESCRIPTION: Nullability condition, if, generic types
 */

// TESTCASE NUMBER: 1
fun <T> case_1(x: T) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T")!>x<!>
    }
}

// TESTCASE NUMBER: 2
fun <T> case_2(x: T?) {
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T!! & T?")!>x<!>
    }
}
