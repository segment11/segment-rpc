package ctrl

import org.segment.rpc.common.Utils
import org.segment.web.common.Conf
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.post('/login') { req, resp ->
    String user = req.param('user')
    String password = req.param('password')
    assert user && password

    def c = Conf.instance
    if (user != c.get('admin.user') || password != c.get('admin.password')) {
        resp.redirect('/index.html?error=1')
    }

    def u = [name: user]
    req.session('user', u)
    resp.redirect('/admin/index.html#/page/that_zk')
}

h.get('/login/user') { req, resp ->
    req.session('user')
}

h.get('/logout') { req, resp ->
    req.removeSession('user')
    resp.redirect('/index.html')
}

h.before('/**') { req, resp ->
    def uri = req.uri()

    if (uri.endsWith('/login') || uri.endsWith('/logout')) {
        return
    }

    // check login
    def u = req.session('user')
    if (!u) {
        resp.halt(403, 'need login')
    }
}

h.exceptionHandler { req, resp, t ->
    log.error('', t)
    resp.status = 500
    resp.outputStream << Utils.getStackTraceString(t)
}
