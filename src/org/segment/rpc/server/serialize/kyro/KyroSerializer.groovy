package org.segment.rpc.server.serialize.kyro

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.pool.KryoFactory
import com.esotericsoftware.kryo.pool.KryoPool
import groovy.transform.CompileStatic
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.handler.Resp
import org.segment.rpc.server.serialize.Serializer

@CompileStatic
class KyroSerializer implements Serializer {
    private static KryoPool pool

    private static KryoFactory factory

    static {
        factory = new KryoFactory() {
            Kryo create() {
                Kryo kryo = new Kryo()
                kryo.setReferences(false)
                kryo.register(Req.class)
                kryo.register(Resp.class)
                return kryo
            }
        }
        pool = new KryoPool.Builder(factory).build()
    }

    @Override
    def <T> T read(byte[] data, Class<T> clz) {
        def kryo = pool.borrow()
        def input = new Input(new ByteArrayInputStream(data))
        try {
            return kryo.readObject(input, clz)
        } finally {
            input.close()
            pool.release(kryo)
        }
    }

    @Override
    byte[] write(Object obj) {
        def kryo = pool.borrow()
        def os = new ByteArrayOutputStream()
        def output = new Output(os)
        try {
            kryo.writeObject(output, obj)
            output.flush()
            output.close()
            return os.toByteArray()
        } finally {
            pool.release(kryo)
        }
    }
}
