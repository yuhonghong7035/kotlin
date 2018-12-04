import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

abstract class MyCoroutineScope : CoroutineScope {
    suspend abstract fun <caret>foo()
}