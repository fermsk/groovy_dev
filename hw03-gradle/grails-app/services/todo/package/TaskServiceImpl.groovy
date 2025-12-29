import grails.gorm.transactions.Transactional
import java.time.LocalDateTime

@Transactional
class TaskServiceImpl implements TaskService {

    @Override
    Task save(String name, LocalDateTime startTime, LocalDateTime endTime) {
        Task task = new Task(
                name: name,
                startTime: startTime,
                endTime: endTime
        )
        task.save(flush: true)
        return task
    }

    @Override
    void delete(Serializable id) {
        Task task = Task.findById(id)
        if (task) {
            task.delete(flush: true)
        }
    }

    @Override
    Task findById(Serializable id) {
        return Task.findById(id)
    }

    @Override
    List findAll() {
        return Task.list()
    }

    @Override
    Number count() {
        return Task.count()
    }

    boolean addActionToTask(Long taskId, String actionName, LocalDateTime actionStartTime, LocalDateTime actionEndTime) {
        Task task = Task.findById(taskId)
        if (!task) {
            return false
        }

        // Check if action fits within task time bounds
        if (actionStartTime >= task.startTime && actionEndTime <= task.endTime) {
            // Check if action overlaps with existing actions
            boolean hasOverlap = task.actions?.any { existingAction ->
                !(actionEndTime <= existingAction.startTime ||
                        actionStartTime >= existingAction.endTime)
            } ?: false

            if (!hasOverlap) {
                Action action = new Action(
                        name: actionName,
                        startTime: actionStartTime,
                        endTime: actionEndTime,
                        task: task
                )
                task.addToActions(action)
                return task.save(flush: true) != null
            }
        }
        return false
    }

    boolean removeActionFromTask(Long taskId, Long actionId) {
        Task task = Task.findById(taskId)
        Action action = Action.findById(actionId)

        if (task && action && task.actions.contains(action)) {
            task.removeFromActions(action)
            action.delete(flush: true)
            return task.save(flush: true) != null
        }
        return false
    }
}