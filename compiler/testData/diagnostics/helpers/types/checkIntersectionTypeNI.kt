package tests._checkIntersectionType

class Inv<T>(x: T)
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
fun <T> checkEqualTypesExact(a: T, b: Inv<T>) {}

fun <T> intersect(vararg args: In<T>): Inv<T> = throw Exception()
