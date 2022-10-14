package org.segment.rpc.server.codec

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
    Stats encoderStats = new Stats()
    Stats decoderStats = new Stats()

    private ScheduledExecutorService scheduler

    private Gauge e1
    private Gauge d1

    private RemoteUrl remoteUrl

    void init(RemoteUrl remoteUrl) {
        this.remoteUrl = remoteUrl
        e1 = Gauge.build().name('encoder_bytes_length_last_1min').
                help('encoder_bytes_length_last_1min').
                labelNames('address').register()

        d1 = Gauge.build().name('decoder_bytes_length_last_1min').
                help('decoder_bytes_length_last_1min').
                labelNames('address').register()

        scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleWithFixedDelay({
            e1.labels(address()).set(encoderStats.getLatestMin())
            d1.labels(address()).set(decoderStats.getLatestMin())
        }, 0, 5, TimeUnit.SECONDS)
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
