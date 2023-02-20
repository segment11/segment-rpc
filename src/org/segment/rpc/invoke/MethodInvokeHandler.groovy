package org.segment.rpc.invoke

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.prometheus.client.Gauge
import io.prometheus.client.Summary
import org.segment.rpc.common.Utils
import org.segment.rpc.server.handler.AbstractHandler
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.provider.DefaultProvider
import org.segment.rpc.server.provider.ServiceProvider
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
@Slf4j
class MethodInvokeHandler extends AbstractHandler {
    private RemoteUrl remoteUrl

    MethodInvokeHandler(RemoteUrl remoteUrl) {
        this.remoteUrl = remoteUrl
    }

    private ServiceProvider provider = DefaultProvider.instance

    private Summary cost = Summary.build().name('method_invoke_ms').
            help('rpc method invoke cost time').
            labelNames('address', 'context', 'interface', 'method').
            quantile(0.5.doubleValue(), 0.05.doubleValue()).
            quantile(0.9.doubleValue(), 0.01.doubleValue()).register()

    private AtomicInteger emptyNumber = new AtomicInteger(0)
    private AtomicInteger failedNumber = new AtomicInteger(0)

    private Gauge emptyNumberGauge = Gauge.build().name('method_not_found_number').
            help('method not found number').
            labelNames('address', 'context', 'interface', 'method').register()
    private Gauge failedNumberGauge = Gauge.build().name('method_invoke_failed_number').
            help('method invoke failed number').
            labelNames('address', 'context', 'interface', 'method').register()

    @Override
    Resp hi(Req req) {
        MethodMeta meta = req.body as MethodMeta
        def methodWrapper = provider.lookupMethod(meta)
        if (methodWrapper == null) {
            def number = emptyNumber.incrementAndGet()
            emptyNumberGauge.labels(remoteUrl.toString(), req.context(), meta.clazz, meta.method).set(number as double)

            log.warn('method not found {}', meta.toString())
            return Resp.fail('method not found', Resp.Status.NOT_FOUND)
        }

        def timer = cost.labels(remoteUrl.toString(), req.context(), meta.clazz, meta.method).startTimer()
        try {
            Object r = methodWrapper.method.invoke(methodWrapper.target, meta.args)
            Resp.one(r)
        } catch (Exception e) {
            def number = failedNumber.incrementAndGet()
            failedNumberGauge.labels(remoteUrl.toString(), req.context(), meta.clazz, meta.method).set(number as double)

            log.error('method invoke error - ' + meta.toString(), e)

            if (remoteUrl.isOn('server.fail.message.with.stacktrace')) {
                Resp.fail(e.message + '\r\n' + Utils.getStackTraceString(e.cause))
            } else {
                Resp.fail(e.message)
            }
        } finally {
            timer.observeDuration()
        }
    }
}
