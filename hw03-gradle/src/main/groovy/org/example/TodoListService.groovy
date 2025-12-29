package org.example

import grails.gorm.transactions.Transactional

@Transactional
class TodoListService {

    def addTask(Task task) {
        // Проверка пересечений
        def overlaps = Task.findAllByStartTimeLessThanAndEndTimeGreaterThan(task.endTime, task.startTime)
        if (overlaps.size() == 0) {
            task.save(flush: true)
            return true
        }
        return false
    }

    def removeTask(String id) {
        def task = Task.get(id)
        if (task) {
            task.delete(flush: true)
            return true
        }
        return false
    }

    def editTaskActions(String taskId, List actions) {
        def task = Task.get(taskId)
        if (task) {
            task.actions.clear()
            actions.each { act ->
                task.addToActions(act)
            }
            task.save(flush: true)
            return true
        }
        return false
    }

    def getTasksForDate(Date date) {
        def startOfDay = date.clearTime()
        def endOfDay = startOfDay + 1
        return Task.findAllByStartTimeBetween(startOfDay, endOfDay) + Task.findAllByEndTimeBetween(startOfDay, endOfDay)
    }

    def getTaskCountForDate(Date date) {
        getTasksForDate(date).size()
    }

    def getBusyPeriodsForDate(Date date) {
        def tasks = getTasksForDate(date)
        return tasks.collect { task -> new Period(start: task.startTime, end: task.endTime) }
    }
}
