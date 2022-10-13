package org.segment.rpc.invoke

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.beanutils.MethodUtils
import org.segment.rpc.server.handler.AbstractHandler
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp

@CompileStatic
@Slf4j
class MethodInvokeHandler extends AbstractHandler {
    // todo
    //  use abstract factory pattern to get bean better
    BeanGetByInterface getImplByInterface = NameMappingLoaderByInterface.instance

    @Override
    Resp hi(Req req) {
        MethodMeta meta = req.body as MethodMeta
        try {
            Object obj = getImplByInterface.get(meta.clazz)
            if (obj == null) {
                log.error('method invoke error, object not found {}', meta.clazz)
                return Resp.fail('object instance not found')
            }
            Object r = MethodUtils.invokeMethod(obj, meta.method, meta.args, meta.paramTypes)
            Resp.one(r)
        } catch (Exception e) {
            log.error('method invoke error - ' + meta.toString(), e)
            Resp.fail(e.message)
        }
    }
}
