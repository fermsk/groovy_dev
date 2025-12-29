import grails.converters.JSON
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskController {

    TaskServiceImpl taskService

    def index() {
        List tasks = taskService.findAll()
        render tasks as JSON
    }

    def show(Long id) {
        Task task = taskService.findById(id)
        if (task) {
            render task as JSON
        } else {
            render status: 404, text: "Task not found"
        }
    }

    def save() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            LocalDateTime startTime = LocalDateTime.parse(params.startTime, formatter)
            LocalDateTime endTime = LocalDateTime.parse(params.endTime, formatter)

            Task task = taskService.save(params.name, startTime, endTime)
            if (task.hasErrors()) {
                render status: 400, text: task.errors.toString()
            } else {
                render task as JSON
            }
        } catch (Exception e) {
            render status: 400, text: "Invalid input: ${e.message}"
        }
    }

    def delete(Long id) {
        taskService.delete(id)
        render status: 204
    }

    def addAction() {
        try {
            Long taskId = params.taskId as Long
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            LocalDateTime startTime = LocalDateTime.parse(params.startTime, formatter)
            LocalDateTime endTime = LocalDateTime.parse(params.endTime, formatter)

            boolean result = taskService.addActionToTask(taskId, params.name, startTime, endTime)
            if (result) {
                render status: 200, text: "Action added successfully"
            } else {
                render status: 400, text: "Failed to add action - check time constraints and overlaps"
            }
        } catch (Exception e) {
            render status: 400, text: "Invalid input: ${e.message}"
        }
    }

    def removeAction() {
        try {
            Long taskId = params.taskId as Long
            Long actionId = params.actionId as Long

            boolean result = taskService.removeActionFromTask(taskId, actionId)
            if (result) {
                render status: 200, text: "Action removed successfully"
            } else {
                render status: 400, text: "Failed to remove action"
            }
        } catch (Exception e) {
            render status: 400, text: "Invalid input: ${e.message}"
        }
    }
}