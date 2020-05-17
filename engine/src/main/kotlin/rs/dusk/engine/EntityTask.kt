package rs.dusk.engine

import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.*
import rs.dusk.engine.entity.list.PooledMapList
import rs.dusk.engine.model.entity.index.Indexed
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author Greg Hibberd <greg@greghibberd.com>
 * @since April 25, 2020
 */
abstract class EntityTask<T : Indexed> : EngineTask {
    val defers: Deque<Deferred<Unit>> = LinkedList()
    private val logger = InlineLogger()

    abstract val entities: PooledMapList<T>

    abstract fun runAsync(entity: T)

    override fun run() = runBlocking {
        entities.forEach {
            defers.add(scope.async { runAsync(it) })
        }
        val took = measureTimeMillis {
            while (defers.isNotEmpty()) {
                defers.poll().await()
            }
        }
        if (took > 0) {
            logger.info { "${this@EntityTask::class.simpleName} took ${took}ms" }
        }
    }

    companion object {
        private val scope = CoroutineScope(newSingleThreadContext("UpdateTasks"))
    }
}