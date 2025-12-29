package org.example

import grails.rest.RestfulController

class TaskController extends RestfulController<Task> {
    static responseFormats = ['json']
    def todoListService

    TaskController() {
        super(Task)
    }

    @Override
    def save() {
        def task = new Task(request.JSON)
        if (todoListService.addTask(task)) {
            respond task, [status: 201]
        } else {
            respond([error: 'Overlap detected or invalid data'], status: 400)
        }
    }

    @Override
    def delete() {
        def id = params.id
        if (todoListService.removeTask(id)) {
            render status: 204
        } else {
            render status: 404
        }
    }

    def editActions() {
        def taskId = params.id
        def actionsData = request.JSON.actions
        if (todoListService.editTaskActions(taskId, actionsData)) {
            respond([status: 'ok'])
        } else {
            respond([error: 'Task not found'], status: 404)
        }
    }

    def getTasksByDate(String dateStr) {
        Date date = Date.parse('yyyy-MM-dd', dateStr)
        respond todoListService.getTasksForDate(date)
    }

    def getCountByDate(String dateStr) {
        Date date = Date.parse('yyyy-MM-dd', dateStr)
        respond([count: todoListService.getTaskCountForDate(date)])
    }

    def getBusyPeriods(String dateStr) {
        Date date = Date.parse('yyyy-MM-dd', dateStr)
        respond todoListService.getBusyPeriodsForDate(date)
    }
}
