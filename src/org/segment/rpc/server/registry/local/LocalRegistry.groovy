package org.segment.rpc.server.registry.local

import groovy.sql.Sql
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.h2.tools.Server
import org.segment.rpc.common.Conf
import org.segment.rpc.common.NamedThreadFactory
import org.segment.rpc.server.handler.Req
import org.segment.rpc.server.registry.Registry
import org.segment.rpc.server.registry.RemoteUrl

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@CompileStatic
@Singleton
@Slf4j
class LocalRegistry implements Registry {
    private ScheduledExecutorService scheduler

    private List<RemoteUrl> cachedLocalList = new CopyOnWriteArrayList<RemoteUrl>()

    private Server server

    private Sql sql

    boolean isNeedStartH2Server = false

    boolean isNeedClearTableFirst = false

    private void refreshToLocal() {
        List<RemoteUrl> getList = []
        sql.query('select * from t') { rs ->
            while (rs.next()) {
                def one = new RemoteUrl(rs.getString('host'), rs.getInt('port'))
                one.context = rs.getString('context')
                one.updatedTime = rs.getDate('update_time')
                getList << one
            }
        }

        log.info 'get list from registry {}', getList.collect { it.getStringWithContext() }.toString()
        log.info 'local list {}', cachedLocalList.collect { it.getStringWithContext() }.toString()

        // do merge list to local
        for (one in getList) {
            def localOne = cachedLocalList.find { it == one }
            if (localOne) {
                localOne.updatedTime = one.updatedTime
            } else {
                cachedLocalList << one
            }
        }
        cachedLocalList.removeAll {
            !(it in getList)
        }
        log.info 'done merge to local cache'
    }

    @Override
    void init() {
        if (isNeedStartH2Server) {
            server = Server.createTcpServer('-tcp', '-tcpAllowOthers', '-tcpPort', '8082', '-ifNotExists').start()
        }

        sql = Sql.newInstance('jdbc:h2:tcp://localhost:8082/~/segment-rpc-local-registry',
                'sa', 'sa', 'org.h2.Driver')
        log.info 'create h2 database connection'

        String ddl = '''
create table if not exists t(
    context varchar, 
    host varchar,
    port int, 
    update_time date
)
'''
        sql.execute(ddl)
        log.info 'done create table'

        if (isNeedClearTableFirst) {
            sql.executeUpdate('delete from t')
            log.info 'done clear table'
        }

        refreshToLocal()

        // use interval, simple
        scheduler = Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory('refresh-registry-to-local-from-local-file'))

        final int interval = Conf.instance.getInt('client.refresh.registry.interval.ms', 10 * 1000)

        def now = new Date()
        int sec = now.seconds
        long delaySeconds = interval - (sec % interval)

        scheduler.scheduleWithFixedDelay({
            try {
                refreshToLocal()
            } catch (Exception e) {
                log.error('refresh-registry-to-local-from-local-file error', e)
            }
        }, delaySeconds, interval, TimeUnit.SECONDS)
    }

    @Override
    void register(RemoteUrl url) {
        def row = sql.firstRow('select context from t where host = ? and port = ?', [url.host as Object, url.port])
        if (!row) {
            String addSql = 'insert into t values(?,?,?,?)'
            sql.executeUpdate(addSql, [url.context as Object, url.host, url.port, url.updatedTime])
            log.info 'registry done add new one {}', url.getStringWithContext()
        } else {
            log.info 'skip as already exists - {}' + row.toString()
        }
    }

    @Override
    List<RemoteUrl> discover(Req req) {
        def context = req.context()
        if (context == null) {
            return []
        }
        cachedLocalList.findAll { context == it.context }
    }

    @Override
    void shutdown() {
        if (scheduler) {
            scheduler.shutdown()
            log.info 'refresh-registry-to-local-from-local shutdown'
        }
        if (sql) {
            sql.close()
            log.info 'close h2 database connection'
        }
        if (server) {
            server.stop()
            log.info 'stop h2 server'
        }
    }
}
