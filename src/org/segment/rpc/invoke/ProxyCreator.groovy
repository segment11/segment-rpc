package org.segment.rpc.invoke

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers
import org.segment.rpc.client.RpcClient
import org.segment.rpc.server.handler.Req

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

@CompileStatic
class ProxyCreator {
    final static String HANDLER_URI = '/_method_invoke'

    private RpcClient client

    private String context

    // local cache
    private static Map<Key, Object> map = new HashMap<>()

    ProxyCreator(RpcClient client, String context) {
        this.client = client
        this.context = context
    }

    public synchronized <T> T create(Class<T> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException('must be interface')
        }
        Key key = new Key(client.uuid, context, interfaceClass.name)
        def r = map.get(key)
        if (r != null) {
            return (T) r
        }

        ElementMatcher.Junction by = ElementMatchers.isDeclaredBy(interfaceClass)
        ElementMatcher.Junction notStatic = ElementMatchers.not(ElementMatchers.isStatic())
        ElementMatcher.Junction notObject = ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))
        def matcher = (by & notStatic) & notObject
        def newOne = new ByteBuddy()
                .subclass(interfaceClass)
                .name(interfaceClass.name + '_ProxyRpcCaller')
                .method(matcher)
                .intercept(InvocationHandlerAdapter.of(new MethodInvocationHandler(client, context)))
                .make()
                .load(interfaceClass.classLoader)
                .getLoaded().newInstance()

        def oldOne = map.putIfAbsent(key, newOne)
        if (oldOne != null) {
            return (T) oldOne
        }
        newOne
    }

    @CompileStatic
    private static class MethodInvocationHandler implements InvocationHandler {
        private RpcClient client

        private String context

        MethodInvocationHandler(RpcClient client, String context) {
            this.client = client
            this.context = context
        }

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

    @CompileStatic
    @TupleConstructor
    @EqualsAndHashCode
    private static class Key {
        String clientUuid
        String context
        String interfaceClassName
    }
}
