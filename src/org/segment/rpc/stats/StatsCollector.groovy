package org.segment.rpc.stats

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.prometheus.client.Collector
import org.segment.rpc.server.registry.RemoteUrl

@CompileStatic
@Singleton
@Slf4j
class StatsCollector extends Collector {
    private RemoteUrl remoteUrl

    void init(RemoteUrl remoteUrl) {
        this.remoteUrl = remoteUrl
    }

    @Override
    List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> list = []

        List<MetricFamilySamples.Sample> samples = []
        def mfs = new MetricFamilySamples('rpc_server', Collector.Type.COUNTER,
                'Rpc server stats.', samples)
        list.add(mfs)

        def counter = CounterInMinute.instance

        List<String> labels = ['address', 'context']
        List<String> labelValues = [remoteUrl.toString(), remoteUrl.context]

        samples.add(new MetricFamilySamples.Sample('encode_length_1min', labels, labelValues,
                counter.getCounter(StatsType.ENCODE_LENGTH) as double))
        samples.add(new MetricFamilySamples.Sample('decode_length_1min', labels, labelValues,
                counter.getCounter(StatsType.DECODE_LENGTH) as double))

        samples.add(new MetricFamilySamples.Sample('resp404_1min', labels, labelValues,
                counter.getCounter(StatsType.RESP_404) as double))
        samples.add(new MetricFamilySamples.Sample('resp500_1min', labels, labelValues,
                counter.getCounter(StatsType.RESP_500) as double))
        samples.add(new MetricFamilySamples.Sample('executor_reject_1min', labels, labelValues,
                counter.getCounter(StatsType.REJECT_NUMBER) as double))

        samples.add(new MetricFamilySamples.Sample('executor_queue_size', labels, labelValues,
                counter.getOne(StatsType.QUEUE_SIZE) as double))

        counter.clearAllOldCounter()

        list
    }
}
