package org.segment.rpc.invoke

import groovy.transform.CompileStatic
import org.segment.rpc.client.RpcClient
import org.segment.rpc.server.handler.Req

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

@CompileStatic
class ProxyCreator {
    final static String HANDLER_URI = '/_method_invoke'

    private RpcClient client

    private String context

    // local cache
    private static Map<Key, Object> map = new ConcurrentHashMap<>()

    ProxyCreator(RpcClient client, String context) {
        this.client = client
        this.context = context
    }

    Object create(Class<?> interfaceClass) {
        def key = new Key(client.uuid, context, interfaceClass.name)
        def r = map.get(key)
        if (r != null) {
            return r
        }

        Class<?>[] classes = [interfaceClass]
        def newOne = Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                classes,
                new InvocationHandler() {
                    @Override
                    Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ('toString' == method.name) {
                            return "ProxyRpcCaller_" + this.class.name
                        }

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
        map.put(key, newOne)
        newOne
    }

    @CompileStatic
    private static class Key {
        String clientUuid
        String context
        String interfaceClassName

        Key(String clientUuid, String context, String interfaceClassName) {
            this.clientUuid = clientUuid
            this.context = context
            this.interfaceClassName = interfaceClassName
        }

        @Override
        boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Key)) {
                return false
            }

            def that = (Key) obj
            this.clientUuid == that.clientUuid && this.context == that.context &&
                    this.interfaceClassName == that.interfaceClassName
        }

        @Override
        int hashCode() {
            (clientUuid + ',' + context + ',' + interfaceClassName).hashCode()
        }
    }
}
