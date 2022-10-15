package model

import groovy.transform.CompileStatic
import org.segment.d.D
import org.segment.d.Ds
import org.segment.d.MySQLDialect
import org.segment.d.Record

@CompileStatic
class ZkClusterDTO extends Record {

    Integer id

    String name

    String des

    String connectString

    String prefix

    Date createdDate

    Date updatedDate

    @Override
    String pk() {
        'id'
    }

    @Override
    D useD() {
        new D(Ds.one(), new MySQLDialect())
    }
}