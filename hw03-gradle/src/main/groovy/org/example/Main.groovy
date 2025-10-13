import java.time.LocalDateTime
import java.time.LocalDate

def todoList = new TodoList()

// Create a task
def task1 = new Task(
        "Study Programming",
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(2)
)

// Create actions for the task
def action1 = new Action(
        "Read documentation",
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(1)
)

def action2 = new Action(
        "Practice coding",
        LocalDateTime.now().plusHours(1),
        LocalDateTime.now().plusHours(2)
)

// Add actions to task
task1.addAction(action1)
task1.addAction(action2)

// Add task to todo list
todoList.addTask(task1)

// Display tasks for today
def today = LocalDate.now()
println "Tasks for today:"
todoList.getTasksForDate(today).each { task ->
    println "- ${task.name}"
    task.actions.each { action ->
        println "  * ${action.description}"
    }
}

// Check for events
todoList.checkAndNotifyEvents()