import conf.DefaultLocalH2DataSourceCreator
import org.segment.d.D
import org.segment.d.MySQLDialect
import org.segment.rpc.common.ConsoleReader
import org.segment.rpc.common.ZkClientHolder
import org.segment.rpc.manage.RpcClientHolder
import org.segment.web.RouteRefreshLoader
import org.segment.web.RouteServer
import org.segment.web.common.CachedGroovyClassLoader
import org.segment.web.common.Conf
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

import java.sql.Types

def log = LoggerFactory.getLogger(this.getClass())

// project work directory set
def c = Conf.instance.resetWorkDir().loadArgs(args)
log.info c.toString()
def srcDirPath = c.projectPath('/src')
def resourceDirPath = c.projectPath('/resources')

// DB
def ds = new DefaultLocalH2DataSourceCreator().create().cacheAs()
def d = new D(ds, new MySQLDialect())
// check if need create table first
def tableNameList = d.query('show tables', String).collect { it.toUpperCase() }
if (!tableNameList.contains('ZK_CLUSTER')) {
    new File(c.projectPath('/init_h2.sql')).text.split(';').each {
        try {
            d.exe(it)
            log.info('done created table - ZK_CLUSTER')
        } catch (Exception e) {
            log.error('create table fail', e)
        }
    }
}
D.classTypeBySqlType[Types.TINYINT] = Integer
D.classTypeBySqlType[Types.SMALLINT] = Integer

// groovy class loader init
def loader = CachedGroovyClassLoader.instance
loader.init(this.getClass().classLoader, srcDirPath + ':' + resourceDirPath)

ChainHandler.instance.context('/dashboard')

def server = RouteServer.instance
server.isStartMetricServer = false
server.loader = RouteRefreshLoader.create(loader.gcl).
        addClasspath(srcDirPath).
        addClasspath(resourceDirPath).
        addDir(c.projectPath('/src/ctrl'))
server.webRoot = c.projectPath('/www')

def reader = ConsoleReader.instance
reader.quitHandler = {
    println 'stop...'
    RpcClientHolder.instance.stop()
    ZkClientHolder.instance.disconnect()
    server.stop()
}
reader.read()

server.start(8888)