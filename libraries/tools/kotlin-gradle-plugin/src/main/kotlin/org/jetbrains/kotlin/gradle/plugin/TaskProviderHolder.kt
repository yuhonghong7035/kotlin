package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypesIfNecessary

class TaskProviderHolder<T : Task> : TaskHolder<T> {
    val provider: TaskProvider<T>
    val name: String
    val configurator: TaskConfigurator<T>

    constructor(project: Project, name: String, type: Class<T>, initAction: (T) -> (Unit)) : super(null, null) {
        //TaskProviderHolder(project.tasks.register(name, type){ with(it, body)}, name)
        configurator = TaskConfigurator(initAction)
        this.provider = project.tasks.register(name, type, configurator)
        this.name = name
    }

    override fun getTaskOrProvider(): Any = provider

    override fun doGetTask(): T {
        return provider.get();
    }

    override fun configure(action: (T) -> (Unit)) {
        configurator.appendAction(action, provider)
    }

    override fun toString(): String {
        return "TaskProviderHolder instane: [className: ${javaClass.name}, holded task name: '${name}']"
    }
}

//TODO may be it is useless!!!
// make sure that configure works as follows
class TaskConfigurator<T : Task> : (T) -> (Unit) {
    private val actionsList = ArrayList<(T) -> (Unit)>()
    private var executed = false

    constructor(defaultAction: (T) -> (Unit)) {
        actionsList.add(defaultAction)
    }

    @Synchronized
    override fun invoke(p1: T) {
        executed = true
        for (action in actionsList) {
            action.invoke(p1)
        }
    }

    @Synchronized
    fun appendAction(action: (T) -> (Unit), provider: TaskProvider<T>) {
        if (executed) {
            provider.configure(action)
        } else {
            actionsList.add(action)
        }
    }
}
