package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

class TaskProviderHolder<T: Task> : TaskHolder<T> {
    val provider : TaskProvider<T>
    val name: String

    constructor(provider: TaskProvider<T>, name: String) : super(null, null) {
        this.provider = provider
        this.name = name
    }

    override fun getTaskOrProvider(): Any = provider

    //TODO improve
    override fun doGetTask() : T {
        println("TPH: Invoked provider.getTask " + provider.get().name)
        for (it in Thread.currentThread().getStackTrace()) {
            println(it)
        }
        return provider.get();
    }

    override fun configure(action: (T) -> (Unit)) {
        provider.configure {
            with(it, action)
        }
    }

    override fun toString(): String {
        return "TaskProviderHolder instane: [className: ${javaClass.name}, holded task name: '${name}']"
    }
}
