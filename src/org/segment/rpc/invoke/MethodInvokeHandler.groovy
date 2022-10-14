package org.segment.rpc.invoke

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.segment.rpc.server.handler.AbstractHandler
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.provider.DefaultProvider
import org.segment.rpc.server.provider.ServiceProvider

@CompileStatic
@Slf4j
class MethodInvokeHandler extends AbstractHandler {
    ServiceProvider provider = DefaultProvider.instance

    @Override
    Resp hi(Req req) {
        MethodMeta meta = req.body as MethodMeta
        def methodWrapper = provider.lookupMethod(meta)
        if (methodWrapper == null) {
            log.warn('method not found {}', meta.toString())
            return Resp.fail('method not found', Resp.Status.EMPTY)
        }
        try {
            Object r = methodWrapper.method.invoke(methodWrapper.target, meta.args)
            Resp.one(r)
        } catch (Exception e) {
            log.error('method invoke error - ' + meta.toString(), e)
            Resp.fail(e.message)
//            Resp.fail(e.message + Utils.getStackTraceString(e.cause))
        }
    }
}
