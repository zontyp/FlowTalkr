package cron

import engine.Engine
import engine.ExecutionScope
import kotlinx.coroutines.launch
import org.quartz.Job
import org.quartz.JobExecutionContext
import workflow.Workflow
import com.fasterxml.jackson.databind.ObjectMapper
import workflow.TriggerDefinition

//calls engine.run
class WorkflowCronJob : Job {

    override fun execute(context: JobExecutionContext) {
        val workflow = context.jobDetail.jobDataMap["workflow"] as Workflow
        val trigger = context.trigger

        val cronTrigger = workflow.triggers
            .filterIsInstance<TriggerDefinition.Cron>()
            .first { "${workflow.id}:${it.id}" == trigger.key.name }
        val engine = Engine()
        val mapper = ObjectMapper()
        println(
            "Cron fired: workflow=${workflow.id}, trigger=${context.trigger.key.name}"
        )

        ExecutionScope.scope.launch {
            engine.run(
                workflow = workflow,
                startNode = cronTrigger.startNode,
                initialInput = mapper.createObjectNode()
            )
        }
    }
}
