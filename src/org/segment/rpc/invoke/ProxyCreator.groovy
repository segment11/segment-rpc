package org.segment.rpc.invoke

import groovy.transform.CompileStatic
import org.segment.rpc.client.RpcClient
import org.segment.rpc.server.handler.Req

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

@CompileStatic
class ProxyCreator {
    final static String HANDLER_URI = '/_method_invoke'

    private RpcClient client

    private String context

    ProxyCreator(RpcClient client, String context) {
        this.client = client
        this.context = context
    }

    public <T> T create(Class<?> interfaceClass) {
        Class<?>[] classes = [interfaceClass]
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                classes,
                new InvocationHandler() {
                    @Override
                    Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        def meta = new MethodMeta()
                        meta.clazz = method.declaringClass.name
                        meta.method = method.name
                        meta.paramTypes = method.parameterTypes
                        meta.args = args

                        def req = new Req(context + HANDLER_URI, meta)
                        req.isMethodInvoke = true
                        def resp = client.sendSync(req)
                        if (!resp.ok()) {
                            throw new RpcMethodInvokeException('invoke error - ' + resp.status + ':' + resp.message)
                        }
                        resp.body
                    }
                }
        )
    }
}
