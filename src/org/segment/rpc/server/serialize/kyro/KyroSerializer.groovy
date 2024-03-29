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
    def <T> T read(InputStream is, Class<T> clz) {
        def kryo = pool.borrow()
        def input = new Input(is)
        try {
            return kryo.readObject(input, clz)
        } finally {
            input.close()
            pool.release(kryo)
        }
    }

    @Override
    int write(Object obj, OutputStream os) {
        def kryo = pool.borrow()
        def output = new Output(os)
        try {
            kryo.writeObject(output, obj)
            output.flush()
            output.close()
            return output.total()
        } finally {
            pool.release(kryo)
        }
    }
}
