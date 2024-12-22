package org.chuma.homecontroller.extensions.external.inverter;

import java.time.LocalTime;

import it.sauronsoftware.cron4j.InvalidPatternException;
import it.sauronsoftware.cron4j.SchedulingPattern;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Scheduler {
    private static final Scheduler instance = new Scheduler();
    static Logger log = LoggerFactory.getLogger(Scheduler.class.getName());
    private final it.sauronsoftware.cron4j.Scheduler scheduler = new it.sauronsoftware.cron4j.Scheduler();

    private Scheduler() {
        scheduler.start();
    }

    public static Scheduler getInstance() {
        return instance;
    }

    public String scheduleTask(LocalTime time, Runnable action) {
        log.debug("Scheduling task at {}", time);
        SchedulingPattern pattern = new SchedulingPattern(time.getMinute() + " " + time.getHour() + " * * *");
        return scheduler.schedule(pattern, new RunnableTask(action));
    }

    public void removeScheduledTasks(Iterable<String> ids) {
        for (String id : ids) {
            scheduler.deschedule(id);
        }
    }

    private static class RunnableTask extends Task {
        private final Runnable runnable;

        public RunnableTask(Runnable runnable) throws InvalidPatternException {
            this.runnable = runnable;
        }

        public void execute(TaskExecutionContext context) {
            log.debug("Executing task");
            runnable.run();
            log.debug("Task execution done.");
        }
    }
}
