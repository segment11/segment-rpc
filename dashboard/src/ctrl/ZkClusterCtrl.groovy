package ctrl

import model.ZkClusterDTO
import org.segment.rpc.common.Conf
import org.segment.rpc.server.registry.RemoteUrl
import org.segment.rpc.server.registry.zookeeper.ZookeeperRegistry
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.group('/zk') {
    h.get('/list') { req, resp ->
        new ZkClusterDTO().where('1=1').loadList()
    }.get('/list/simple') { req, resp ->
        new ZkClusterDTO().where('1=1').queryFields('id,name,des').loadList()
    }.delete('/delete/:id') { req, resp ->
        def id = req.param(':id')
        assert id
        new ZkClusterDTO(id: id as int).delete()
        [flag: true]
    }.post('/update') { req, resp ->
        def one = req.bodyAs(ZkClusterDTO)
        assert one.name && one.connectString && one.prefix
        one.updatedDate = new Date()
        if (one.id) {
            one.update()
            return [id: one.id]
        } else {
            def id = one.add()
            return [id: id]
        }
    }.get('/overview') { req, resp ->
        def id = req.param('id')
        assert id

        ZkClusterDTO one = new ZkClusterDTO(id: id as int).one()

        Conf.instance.put('zookeeper.connect.string', one.connectString)
        def registry = ZookeeperRegistry.instance
        registry.clearLocal()
        registry.refreshToLocal()

        def list = registry.cachedLocalList
        def r = list.groupBy {
            it.context
        }
        [r: r, contextList: r.keySet().collect { [value: it] }]
    }.post('/switch') { req, resp ->
        RemoteUrl url = req.bodyAs(RemoteUrl)
        url.ready = !url.ready
        url.updatedTime = new Date()

        def registry = ZookeeperRegistry.instance
        registry.register(url)
        [flag: true, ready: url.ready]
    }.post('/weight/update') { req, resp ->
        RemoteUrl url = req.bodyAs(RemoteUrl)
        url.updatedTime = new Date()

        def registry = ZookeeperRegistry.instance
        registry.register(url)
        [flag: true, weight: url.weight]
    }
}