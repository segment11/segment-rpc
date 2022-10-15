import org.segment.rpc.client.RpcClient

beans {
    xmlns rpc: 'https://github.com/segment11/segment-rpc/tree/main/schema/segment-rpc'
    rpc.'segment-rpc-provider'('base-package': 'com.segment.rpc.spring')

    rpc.'segment-rpc-caller'(interface: 'com.segment.rpc.spring.Bean', client: 'client', context: '/rpc')

    client(RpcClient) {
        it.destroyMethod = 'stop'
    }
}