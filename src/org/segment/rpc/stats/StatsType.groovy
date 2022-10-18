package org.segment.rpc.stats

import groovy.transform.CompileStatic

@CompileStatic
enum StatsType {
    ENCODE_LENGTH('encode_length'),
    DECODE_LENGTH('decode_length'),
    QUEUE_SIZE('queue_size'),
    REJECT_NUMBER('reject_number'),
    RESP_500('resp_500'),
    RESP_404('resp_404')

    String name

    StatsType(String name) {
        this.name = name
    }
}