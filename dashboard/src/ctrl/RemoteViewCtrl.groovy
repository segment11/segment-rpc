package ctrl

import com.github.kevinsawicki.http.HttpRequest
import model.ZkClusterDTO
import org.segment.rpc.manage.RpcClientHolder
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.RemoteUrl
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.group('/remote') {
    h.get('/overview') { req, resp ->
        def id = req.param('id')
        def context = req.param('context')
        assert id && context

        ZkClusterDTO one = new ZkClusterDTO(id: id as int).one()

        def clientWrapper = RpcClientHolder.instance.create(one.connectString, one.prefix)
        if (clientWrapper.isFirstCreated) {
            Thread.sleep(1000 * 2)
        }
        def r = new Req(context + '/remote/overview')
        def rpcResp = clientWrapper.client.sendSync(r)
        Map body = rpcResp.body
        body.remove('clientChannelInfo')
        body
    }.post('/stats') { req, resp ->
        def id = req.param('id')
        assert id

        ZkClusterDTO one = new ZkClusterDTO(id: id as int).one()
        RemoteUrl url = req.bodyAs(RemoteUrl)
        def metricGetUrl = url.host + ':' + url.metricExportPort

        final int timeout = 2
        def body = HttpRequest.get('http://' + metricGetUrl).connectTimeout(timeout * 1000).readTimeout(timeout * 1000).body()

        def statsList = []
        body.readLines().each {
            def line = it.trim()
            if (!line || line.startsWith('#')) {
                return
            }
            def arr = line.split(' ')
            def item = [key: arr[0], value: arr[1]]
            statsList << item
        }

        def r = [statsList: statsList]
        r
    }
}
