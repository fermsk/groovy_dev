import java.time.LocalDateTime

class Task {
    String name
    LocalDateTime startTime
    LocalDateTime endTime
    Set<Action> actions = []

    Task(String name, LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name
        this.startTime = startTime
        this.endTime = endTime
    }

    boolean addAction(Action action) {
        // Check if action fits within task time bounds
        if (action.startTime >= startTime && action.endTime <= endTime) {
            // Check if action overlaps with existing actions
            boolean hasOverlap = actions.any { existingAction ->
                !(action.endTime <= existingAction.startTime ||
                        action.startTime >= existingAction.endTime)
            }
            if (!hasOverlap) {
                actions.add(action)
                return true
            }
        }
        return false
    }

    boolean removeAction(Action action) {
        actions.remove(action)
    }
}