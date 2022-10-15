package ctrl

import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.group('/server') {
    h.get('/detail') { req, resp ->

    }
}
