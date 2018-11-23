// WITH_RUNTIME
// FIX: Add '.await()' to function result (breaks use-sites!)

package kotlinx.coroutines

fun <caret>myFunction(context: CoroutineContext, switch: Int): Deferred<Int> {
    with (GlobalScope) {
        when (switch) {
            0 -> return async {
                val x = 123
                x * x
            }
            1 -> return async(context) { -1 }
            else -> return async(context, CoroutineStart.DEFAULT) { 9 }
        }
    }
}
