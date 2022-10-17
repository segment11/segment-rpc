package org.segment.rpc.stats

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.prometheus.client.Gauge
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@CompileStatic
@Singleton
@Slf4j
class StatsHolder {
    private ScheduledExecutorService scheduler

    private Gauge encodeLengthLastMin
    private Gauge decodeLengthLastMin

    private Gauge resp404
    private Gauge resp500

    private Gauge rejectNumber

    private RemoteUrl remoteUrl

    private CounterInMinute counter = CounterInMinute.instance

    void init(RemoteUrl remoteUrl) {
        this.remoteUrl = remoteUrl
        encodeLengthLastMin = Gauge.build().name('encoder_bytes_length_last_1min').
                help('encoder_bytes_length_last_1min').
                labelNames('address').register()

        decodeLengthLastMin = Gauge.build().name('decoder_bytes_length_last_1min').
                help('decoder_bytes_length_last_1min').
                labelNames('address').register()

        resp404 = Gauge.build().name('resp_404').
                help('resp_404').
                labelNames('address').register()

        resp500 = Gauge.build().name('resp_500').
                help('resp_500').
                labelNames('address').register()

        rejectNumber = Gauge.build().name('reject_number').
                help('handler_executor_pool_reject_number').labelNames('address').register()

        scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleWithFixedDelay({
            encodeLengthLastMin.labels(address()).set(counter.get(StatsType.ENCODE_LENGTH))
            decodeLengthLastMin.labels(address()).set(counter.get(StatsType.DECODE_LENGTH))

            resp404.labels(address()).set(counter.get(StatsType.RESP_404))
            resp500.labels(address()).set(counter.get(StatsType.RESP_500))

            rejectNumber.labels(address()).set(counter.get(StatsType.REJECT_NUMBER))
        }, 0, 10, TimeUnit.SECONDS)
    }

    private String address() {
        remoteUrl ? remoteUrl.toString() : ''
    }

    void stop() {
        if (scheduler) {
            scheduler.shutdown()
            log.info 'refresh-stats-to-gauge shutdown'
        }
    }
}
