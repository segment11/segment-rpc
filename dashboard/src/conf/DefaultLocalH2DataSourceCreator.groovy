package conf

import groovy.transform.CompileStatic
import org.segment.d.Ds
import org.segment.web.common.Conf

@CompileStatic
class DefaultLocalH2DataSourceCreator {
    Ds create() {
        def c = Conf.instance

        def dbParams = [:]
        c.params.findAll { it.key.startsWith('db.') }.each { k, v ->
            dbParams[k[3..-1]] = v
        }
        if (dbParams.url) {
            Ds.register('other', dbParams.driver as String) { String ip, int port, String db ->
                dbParams.url
            }
            int i = c.getInt('db.minPoolSize', 5)
            int j = c.getInt('db.maxPoolSize', 10)
            return Ds.dbType('other').connectWithPool('', 0, '',
                    dbParams.user.toString(), dbParams.password.toString(), i, j)
        }

        String dbDataDir = c.getString('dbDataDir', '~/var/segment-rpc-dashboard/data')
        Ds.h2Local(dbDataDir)
    }
}
