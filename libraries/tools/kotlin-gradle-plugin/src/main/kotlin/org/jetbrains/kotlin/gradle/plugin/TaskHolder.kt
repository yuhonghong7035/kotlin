package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task
import org.gradle.api.Project


/**
 * Reference to a org.gradle.api.Task necessary in order to support flexible creation of tasks.
 * For gradle versions < 4.9 tasks are created meanwhile for gradle with version >= 4.9 tasks are registered
 */
open class TaskHolder<T: Task>(val task: T?, val project: Project?) {

    /**
     * Returns Task itself if task was created or TaskProvider<Task> if task was registered.
     */
    open fun getTaskOrProvider(): Any = task!!

    /**
     * Returns instance of task. If task created using lazy api, it will be instantiated
     */
    open fun doGetTask() : T {
        return task!!
    }

    override fun toString(): String {
        return "TaskHolder instance: [className: ${javaClass.name}, holded task: '${doGetTask().name}']"
    }

    /**
     * TODO description
     */
    open fun configure(action: (T) -> (Unit)) {
        with(task!!, action)
    }

}
