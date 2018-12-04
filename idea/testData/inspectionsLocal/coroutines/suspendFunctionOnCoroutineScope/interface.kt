import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

interface MyCoroutineScope : CoroutineScope {
    suspend fun <caret>foo()
}