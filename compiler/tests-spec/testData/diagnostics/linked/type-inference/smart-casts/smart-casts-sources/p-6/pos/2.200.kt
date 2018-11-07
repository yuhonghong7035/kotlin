// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: type-inference, smart-casts, smart-casts-sources -> paragraph 6 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Nullability condition smartcast source using if expression and anonymous object types.
 * UNEXPECTED BEHAVIOUR
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    val y = when (x) {
        is Number -> object {}
        else -> null
    }

    if (y != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("case_1.<no name provided> & case_1.<no name provided>?")!>y<!>
    }
}
