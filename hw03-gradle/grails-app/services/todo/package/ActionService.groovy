import grails.gorm.services.Service

@Service(Action)
interface ActionService {
    Action save(Map args)
    void delete(Serializable id)
    Action findById(Serializable id)
    List findAll()
}