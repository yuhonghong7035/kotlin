package tests._checkIntersectionType

class In<in T>

/*
 * Example of use to the check of an intersection type:
 *
 * if (a is T1) {
 *     if (a is T2) {
 *         checkEqualTypesExact(a, intersect(In<T1>(), In<T2>()))
 *     }
 * }
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <T> checkEqualTypesExact(a: @kotlin.internal.Exact T, b: @kotlin.internal.Exact In<T>) {}

fun <T> intersect(vararg args: T): T = throw Exception()
