import java.time.LocalDateTime
import java.time.LocalDate

class TodoList {
    Set<Task> tasks = []

    boolean addTask(Task task) {
        // Check if task time period is available
        boolean hasOverlap = tasks.any { existingTask ->
            !(task.endTime <= existingTask.startTime ||
                    task.startTime >= existingTask.endTime)
        }

        if (!hasOverlap) {
            tasks.add(task)
            return true
        }
        return false
    }

    boolean removeTask(Task task) {
        tasks.remove(task)
    }

    def editTaskActions(Task task, List<Action> newActions) {
        if (tasks.contains(task)) {
            task.actions.clear()
            newActions.each { action ->
                task.addAction(action)
            }
            return true
        }
        return false
    }

    int getTaskCountForDate(LocalDate date) {
        tasks.count { task ->
            task.startTime.toLocalDate() == date ||
                    task.endTime.toLocalDate() == date
        }
    }

    List<Period> getBusyPeriodsForDate(LocalDate date) {
        def busyPeriods = []
        tasks.findAll { task ->
            task.startTime.toLocalDate() == date ||
                    task.endTime.toLocalDate() == date
        }.each { task ->
            busyPeriods << new Period(task.startTime, task.endTime)
        }
        return busyPeriods
    }

    List<Task> getTasksForDate(LocalDate date) {
        tasks.findAll { task ->
            task.startTime.toLocalDate() == date ||
                    task.endTime.toLocalDate() == date
        }
    }

    void checkAndNotifyEvents() {
        LocalDateTime now = LocalDateTime.now()
        tasks.each { task ->
            task.actions.each { action ->
                if (action.event.dateTime <= now) {
                    println "EVENT: ${action.event.message} (Task: ${task.name})"
                }
            }
        }
    }
}