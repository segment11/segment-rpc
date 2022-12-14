// localstorage
(function (global) {
    var TIME_SUF = '__SAVED_TIME';

    var LocalStore = function (isSession, refreshInterval) {
        this.storage = isSession ? window.sessionStorage : window.localStorage;
        // default 1 hour
        this.refreshInterval = refreshInterval || 1000 * 60 * 60;
        if (!this.storage)
            console.log('Web local storage api not support!');
    };

    var extend = {
        clear: function () {
            if (!this.storage)
                return;

            this.storage.clear();
        },

        get: function (key) {
            if (!this.storage)
                return;

            var str = this.storage[key];
            if (!str || 'undefined' == str || 'null' == str)
                return null;

            // check if expired
            var timeSaved = this.storage[key + TIME_SUF];
            if (!timeSaved)
                return null;

            if ((new Date().getTime() - parseInt(timeSaved)) > this.refreshInterval) {
                this.storage.removeItem(key);
                this.storage.removeItem(key + TIME_SUF);
                return null;
            }

            return JSON.parse(str);
        },

        set: function (key, val) {
            if (!this.storage)
                return;

            this.storage[key] = JSON.stringify(val);
            this.storage[key + TIME_SUF] = '' + new Date().getTime();
        },

        remove: function (key) {
            if (!this.storage)
                return;

            this.storage.removeItem(key);
            this.storage.removeItem(key + TIME_SUF);
        }
    };

    for (key in extend) {
        LocalStore.prototype[key] = extend[key];
    }

    global.LocalStore = LocalStore;
})(window);

$(function () {
    // render menu by role on server side
    var menus = [
        {
            title: 'Segment RPC', icon: 'icon-list', list: [
                { title: 'Zookeeper Registry', page: 'that_zk' }
            ]
        }
    ];

    var sidebarMenu = Consts.format($('#menu-tpl').html(), { list: menus });
    $('#sidebar').html(sidebarMenu);

    $('#sidebar').append('<i class="foot icon icon-caret-left"></i>');

    var isHide = false;
    $('#sidebar .foot').click(function (e) {
        if (!isHide) {
            $('#sidebar .menu').hide();
            $('#sidebar').css('width', '1%');
            $('#main').css('padding-left', '2%');
            $('#sidebar .foot').addClass('icon-caret-right');
            isHide = true;
        } else {
            $('#main').css('padding-left', '15%');
            $('#sidebar').css('width', '15%');
            $('#sidebar .menu').show();
            $('#sidebar .foot').removeClass('icon-caret-right');
            isHide = false;
        }
    });

    // sidebar events
    $('.menu-title').hover(function (e) {
        var m = $(e.target);
        if (!m.is('.menu-title')) {
            m = m.closest('.menu-title');
        }

        m.addClass('hover');
    }, function () {
        $('.menu-title').removeClass('hover');
    });

    $('.menu-title').click(function (e) {
        e.stopPropagation();
        var m = $(e.target);
        if (!m.is('.menu-title')) {
            m = m.closest('.menu-title');
        }

        var plusIcon = m.find('.menu-title-text .icon');

        var ul = m.parent().find('ul');
        var isShow = ul.data('isShow');
        if (!isShow) {
            ul.data('isShow', 1);
            ul.stop().slideDown('fast');
            plusIcon.addClass('icon-minus');
        } else {
            ul.data('isShow', 0);
            ul.stop().slideUp('fast');
            plusIcon.removeClass('icon-minus');
        }
    });

    $.get('/dashboard/login/user', function (data) {
        if (data.name) {
            $('.user-name').text(data.name);
        }
    });

    // nav bar events
    $('#link-logout').click(function (e) {
        document.location.href = '/dashboard/logout';
    });
});

(function (win) {
    var LocalStore = win.LocalStore;

    var Page = {};
    win.Page = Page;

    Page.pageLoaded = [];
    Page.renderContent = function (page, params) {
        $('#content-page').remove();

        var queryStr = '';
        if (params) {
            var paramsList = [];
            for (key in params) {
                paramsList.push(key + '=' + params[key]);
            }
            queryStr = paramsList.join('&');
        }

        $.ajax({
            type: 'get',
            cache: false,
            url: 'pages/' + page + '.html',
            data: queryStr,
            dataType: 'text',
            success: function (content) {
                var htmlInner = '<div id="content-page">' + content + '</div>';
                $('#main').html(htmlInner);

                angular.bootstrap($('#content-page'), ['module_' + page]);
                $.dialog.unloading();
            },

            error: function () {
                $.dialog.unloading();
            }
        });
    };

    Page.open = function (page, params) {
        $.dialog.loading();
        if (this.pageLoaded.contains(page)) {
            this.renderContent(page, params);
        } else {
            var that = this;
            $LAB.script('pages/' + page + '.js?_=' + new Date().getTime()).wait(function () {
                // $LAB.script('pages/' + page + '.js').wait(function () {
                that.pageLoaded.push(page);
                that.renderContent(page, params);
            });
        }
        $('.vtip').remove();
    };

    Page.go = function (hash, params) {
        var store = new LocalStore(true);
        if (params) {
            store.set('page-params', params);
        } else {
            store.remove('page-params');
        }
        document.location.hash = '#' + hash;
    };

    Page.params = function () {
        var store = new LocalStore(true);
        var r = store.get('page-params') || {};
//        store.remove('page-params');
        return r;
    };

    Page.fixCenter = function (dialog, fixDelay) {
        // setTimeout -> locate center after $digest -> dom rebuild
        // donot use $timeout as need not $digest again
        if (dialog == null) {
            return;
        }
        setTimeout(function () {
            var wrap = dialog.DOM.wrap[0];
            var left = ($(window).width() - wrap.offsetWidth) / 2;
            var top = ($(window).height() - wrap.offsetHeight) / 2 - 20;
            dialog.position(left, top);
        }, fixDelay || 200);
    };

    var intervalFunc = {};
    Page.registerIntervalFunc = function (hash, fnName) {
        if (intervalFunc[hash]) {
            return false;
        } else {
            intervalFunc[hash] = fnName;
            return true;
        }
    };

    var intervalId = setInterval(function () {
        var hash = document.location.hash;
        if (!hash) {
            return;
        }
        var fnName = intervalFunc[hash];
        if (!fnName) {
            return;
        }

        var ctrl = document.querySelector('[ng-controller=MainCtrl]');
        if (!ctrl) {
            return;
        }
        var scope = angular.element(ctrl).scope();
        if (!scope) {
            return;
        }

        scope.$apply(function () {
            console.log('begin call - ' + fnName);
            scope.$eval(fnName + '()');
        });
    }, 1000 * 5);
})(this);

(function () {
    var routes = {
        '/page/:page': function (page) {
            page = page.replace(/\_/, '/');
            Page.open(page);
        }
    };

    var router = Router(routes);
    router.init();
})();

(function () {
    var md = angular.module('base', ['ng.ui']);
    md.run(['uiValid', function (uiValid) {
        uiValid.regPat('email', /^(\w)+(\.\w+)*@(\w)+((\.\w+)+)$/, 'eg. dingyong87@163.com');
        uiValid.regPat('url', /^((ht|f)tps?):\/\/([\w-]+(\.[\w-]+)*\/?)+(\?([\w\-\.,@?^=%&:\/~\+#]*)+)?$/, 'eg. http://a.com/b');
        uiValid.regPat('cronExp', function (val) {
            // todo
            return true;
        }, 'eg. * * * * * - refer cron4j, http://www.sauronsoftware.it/projects/cron4j/manual.php#p02');
        uiValid.regPat('uri', function (val) {
            return val.indexOf('/') == 0;
        }, 'begin with / eg. /health');
        uiValid.regPat('dir', function (val) {
            return val.indexOf('/') == 0;
        }, 'begin with / eg. /volume/dir');
        uiValid.regPat('ips', function (val) {
            return _.every(val.split(','), function (it) {
                var arr = it.split(/\./);
                if (arr.length != 4) {
                    return false;
                }
                return _.every(arr, function (x) {
                    return /^\d+$/.test(x) && parseInt(x) >= 0 && parseInt(x) <= 255;
                });
            });
        }, 'eg. 127.0.0.1,127.0.0.1');
        uiValid.regPat('endpoints', function (val) {
            return _.every(val.split(','), function (it) {
                if (it.indexOf('http://') != -1) {
                    it = it.replace('http://', '');
                }
                var arr2 = it.split(':');
                if (arr2.length != 2) {
                    return false;
                }
                var portStr = arr2[1];
                if (!/^\d+$/.test(portStr) || parseInt(portStr) <= 0 || parseInt(portStr) >= 65535) {
                    return false;
                }
                var arr = it.split(':')[0].split(/\./);
                if (arr.length != 4) {
                    return false;
                }
                return _.every(arr, function (x) {
                    return /^\d+$/.test(x) && parseInt(x) >= 0 && parseInt(x) <= 255;
                });
            });
        }, 'eg. 127.0.0.1:80,127.0.0.1:80');
        uiValid.regPat('schemeHostPort', function (val) {
            if (val.indexOf('http://') != 0) {
                return false;
            }
            var arr2 = val.split(':');
            if (arr2.length != 3) {
                return false;
            }
            var portStr = arr2[2];
            if (!/^\d+$/.test(portStr) || parseInt(portStr) <= 0 || parseInt(portStr) >= 65535) {
                return false;
            }
            return true;
        }, 'eg. http://127.0.0.1:80');
        uiValid.regPat('imageWithGroup', function (val) {
            if (val.indexOf('/') == -1) {
                return false;
            }
            var arr2 = val.split('/');
            if (arr2.length != 2) {
                return false;
            }
            return true;
        }, 'eg. library/nginx');
    }]);
    md.factory('responseLoginFilter', ['$q', 'uiTips', function ($q, uiTips) {
        var responseLoginFilter = {
            responseError: function (response) {
                uiTips.unloading();
                $('.vtip').remove();
                if (403 == response.status) {
                    setTimeout(function () {
                        document.location.href = '/index.html'
                    }, 500);
                } else if (500 == response.status) {
                    uiTips.alert(response.data);
                }
                return $q.reject(response);
            },
            response: function (response) {
                uiTips.unloading();
                $('.vtip').remove();
                return response;
            },
            // request: function (config) {
            //     uiTips.loading();
            //     return config;
            // }
        };
        return responseLoginFilter;
    }]);
    md.config(['$httpProvider', function ($httpProvider) {
        $httpProvider.interceptors.push('responseLoginFilter');
    }]);
    md.run(['conf', function (conf) {
        conf.uiChartPie = {
            type: 'pie',
            radius: '30%',
            label: {
                normal: {
                    show: true,
                    formatter: '{b}: {c}({d}%)'
                }
            }
        };
    }]);


    md.directive('uiMind', ['conf', 'uiLog', function (conf, log) {
        'use strict';
        return {
            restrict: 'A',
            link: function (scope, el, attrs, ctrl) {
                    var one = scope.$eval(attrs.uiMind) || {};
                    el[0].id = 'show-' + one.id;

                    var data = [{ id: 'root', isroot: true, topic: 'Router' }];
                    _.each(one.serverList, function(it, i){
                        data.push({ id: 'sub' + i, parentid: 'root', topic: it.url + ' __ weight:' + it.weight });
                    });

                    var mind = {
                        meta: {
                            name: 'router-show'
                        },
                        format: 'node_array',
                        data: data
                    };
                    var options = {
                        container: 'show-' + one.id,
                        editable: false,
                        theme: 'primary',
                    };
                    jsMind.show(options, mind);
            }
        };
    }]);

    md.directive('uiChartPie', ['conf', 'uiLog', function (conf, log) {
        'use strict';
        return {
            restrict: 'A',
            link: function (scope, el, attrs, ctrl) {
                var myChart = echarts.init(el[0]);

                var opts = scope.$eval(attrs.uiChartPie) || {};
                scope.$watch(opts.scopeDataName, function (val) {
                    if (!val) {
                        return;
                    }

                    var series = angular.copy(conf.uiChartPie);
                    series.data = val;
                    var chartOption = { title: { text: opts.title }, series: series };
                    log.i(chartOption);
                    myChart.setOption(chartOption);
                });
            }
        };
    }]);
    md.directive('uiChartLine', ['conf', 'uiLog', function (conf, log) {
        'use strict';
        return {
            restrict: 'A',
            link: function (scope, el, attrs, ctrl) {
                var myChart = echarts.init(el[0]);

                var opts = scope.$eval(attrs.uiChartLine) || {};
                scope.$watch(opts.scopeDataName, function (val) {
                    if (!val) {
                        return;
                    }

                    var series = [{
                        name: opts.name,
                        type: 'line',
                        smooth: true,
                        symbol: 'circle',
                        symbolSize: 6,
                        lineStyle: {
                            color: {
                                type: 'linear',
                                x: 0,
                                y: 0,
                                x2: 0,
                                y2: 1,
                                colorStops: [{
                                    offset: 0,
                                    color: '#6ae6dd'
                                }, {
                                    offset: 1,
                                    color: 'red'
                                }],
                                globalCoord: false
                            },
                            width: 2,
                            type: 'solid',
                        },
                        itemStyle: {
                            color: '#00E5DE'
                        },
                        areaStyle: {
                            color: '#004c5E'
                        },
                        data: val.data
                    }];

                    var chartOption = {
                        title: {
                            text: opts.title, textStyle: {
                                color: 'blue',
                            },
                            left: 'center'
                        }, series: series, xAxis: [{
                            type: 'category',
                            boundaryGap: true,
                            axisLine: {
                                show: true,
                                lineStyle: {
                                    color: '#17273B',
                                    width: 1,
                                    type: 'solid'
                                }
                            },
                            axisLabel: {
                                show: false,
                                fontSize: 12,
                                color: 'blue'
                            },
                            data: val.xData
                        }], yAxis: [{
                            type: 'value',
                            axisLine: {
                                show: true,
                                lineStyle: {
                                    color: '#17273B',
                                    width: 1,
                                    type: 'solid'
                                }
                            },
                            axisLabel: {
                                show: true,
                                color: 'blue',
                                fontSize: 12
                            },
                            splitNumber: 10,
                            splitLine: {
                                show: true,
                                lineStyle: {
                                    color: '#17273B',
                                    width: 1,
                                    type: 'solid'
                                }
                            }
                        }], grid: {
                            left: '4%',
                            right: '6%',
                            bottom: '4%',
                            top: 80,
                            containLabel: true
                        }, tooltip: {
                            trigger: 'axis',
                            position: 'top',
                            axisPointer: {
                                type: 'cross',
                                label: {
                                    backgroundColor: '#6a7985'
                                }
                            }
                        }, legend: {
                            show: false
                        }, color: ['#6a7985']
                    };
                    // log.i(chartOption);
                    myChart.setOption(chartOption);
                });
            }
        };
    }]);

    md.filter('timeAgo', function () {
        return function (val) {
            var returnText = "";
            var nowDate = new Date().getTime();
            var setDate = new Date(val).getTime();
            var times = Math.floor((nowDate - setDate) / 1000);
            if (times > 60 * 60 * 24 * 365) {
                returnText = Math.floor(times / (60 * 60 * 24 * 365)) + "year ago";
            } else if (times > 60 * 60 * 24 * 30) {
                returnText = Math.floor(times / (60 * 60 * 24 * 30)) + "month ago";
            } else if (times > 60 * 60 * 24) {
                returnText = Math.floor(times / (60 * 60 * 24)) + "day ago";
            } else if (times > 60 * 60) {
                returnText = Math.floor(times / (60 * 60)) + "hour ago";
            } else if (times > 60) {
                returnText = Math.floor(times / (60)) + "minute ago";
            } else if (times > 0) {
                returnText = Math.floor(times / 1) + "second ago";
            } else {
                returnText = "error - " + val;
            }
            return returnText;
        };
    });
})();