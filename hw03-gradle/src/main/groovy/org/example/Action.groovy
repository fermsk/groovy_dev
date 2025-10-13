class Action {
    String description
    LocalDateTime startTime
    LocalDateTime endTime
    Event event

    Action(String description, LocalDateTime startTime, LocalDateTime endTime) {
        this.description = description
        this.startTime = startTime
        this.endTime = endTime
        this.event = new Event("${description} is due", startTime)
    }
}