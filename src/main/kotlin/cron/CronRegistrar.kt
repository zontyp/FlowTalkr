package cron

import engine.ExecutionScope
import engine.Engine
import kotlinx.coroutines.launch
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import workflow.Workflow
import workflow.TriggerDefinition
import com.fasterxml.jackson.databind.ObjectMapper

object CronRegistrar {

    private const val JOB_GROUP = "workflow-jobs"
    private const val TRIGGER_GROUP = "workflow-cron-triggers"

    fun register(workflows: List<Workflow>) {
        val scheduler = StdSchedulerFactory().scheduler
        scheduler.start()

        workflows.forEach { workflow ->
            registerWorkflowJob(scheduler, workflow)
            registerWorkflowCronTriggers(scheduler, workflow)
        }
    }

    private fun registerWorkflowJob(
        scheduler: Scheduler,
        workflow: Workflow
    ) {
        val jobKey = JobKey(workflow.id, JOB_GROUP)

        if (scheduler.checkExists(jobKey)) {
            return
        }

        val jobDetail = JobBuilder.newJob(WorkflowCronJob::class.java)
            .withIdentity(jobKey)
            .usingJobData(
                JobDataMap(
                    mapOf(
                        "workflow" to workflow
                    )
                )
            )
            .storeDurably() // important: job exists without triggers
            .build()

        scheduler.addJob(jobDetail, false)
    }

    private fun registerWorkflowCronTriggers(
        scheduler: Scheduler,
        workflow: Workflow
    ) {
        workflow.triggers
            .filterIsInstance<TriggerDefinition.Cron>()
            .forEach { cron ->
                println("Registering cron for workflow=${workflow.id}, trigger=${cron.id}, expr=${cron.expression}")

                val triggerKey = TriggerKey(
                    "${workflow.id}:${cron.id}",
                    TRIGGER_GROUP
                )

                if (scheduler.checkExists(triggerKey)) {
                    return@forEach
                }

                val trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(JobKey(workflow.id, JOB_GROUP))
                    .withSchedule(
                        CronScheduleBuilder.cronSchedule(cron.expression)
                    )
                    .build()

                scheduler.scheduleJob(trigger)
            }
    }
}
