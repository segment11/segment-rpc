package ctrl

import model.ZkClusterDTO
import org.segment.rpc.manage.RpcClientHolder
import org.segment.rpc.server.handler.Req
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.group('/remote') {
    h.get('/overview') { req, resp ->
        def id = req.param('id')
        def context = req.param('context')
        assert id && context

        ZkClusterDTO one = new ZkClusterDTO(id: id as int).one()

        def client = RpcClientHolder.instance.create(one.connectString, one.prefix)
        def r = new Req(context + '/remote/overview')
        def rpcResp = client.sendSync(r)
        Map body = rpcResp.body
        body.remove('clientChannelInfo')
        body
    }
}
