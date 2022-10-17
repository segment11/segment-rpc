var md = angular.module('module_that/overview', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {statsKeyword: ''};
    $scope.ctrl = {};

    var tmp = {};

    var Page = window.Page;
    var params = Page.params();

    // zk define row id
    var id = params.id;
    $scope.tmp.name = params.name;
    $scope.tmp.des = params.des;

    $http.get('/dashboard/zk/overview', { params: { id: id } }).success(function (data) {
        $scope.tmp.contextList = data.contextList;
        tmp.r = data.r;

        if(data.contextList.length){
            $scope.tmp.context = data.contextList[0].value;
            $scope.onContextChoose();
        }
    });

    $scope.onContextChoose = function () {
        if (!$scope.tmp.context) {
            $scope.list = [];
            $scope.uriList = [];
            $scope.methodList = [];
            return;
        }
        $scope.list = tmp.r[$scope.tmp.context];

        var p = { id: id, context: $scope.tmp.context };
        setTimeout(function(){
            $http.get('/dashboard/remote/overview', { params: p }).success(function (data) {
                $scope.uriList = data.uriList;
                $scope.methodList = data.methodList;
            });
        }, 1000);
    };

    // filter
    $scope.queryLl = function(){
        var keyword = $scope.tmp.keyword;
        if(!keyword){
            $scope.onContextChoose();
            return;
        }

        $scope.list = _.filter($scope.list, function(it){
            return it.host.contains(keyword);
        });
        $scope.uriList = _.filter($scope.uriList, function(it){
            return it.contains(keyword);
        });
        $scope.methodList = _.filter($scope.methodList, function(it){
            return it.clazz.contains(keyword) || it.method.contains(keyword);
        });
    };

    $scope.showMetrics = function(one){
        $http.post('/dashboard/remote/stats?id=' + id, one).success(function(data){
            tmp.statsList = data.statsList;
            $scope.statsList = tmp.statsList;

            $scope.tmp.statsDialogTitle = 'Server Address - ' + one.host + ':' + one.port;
            $scope.ctrl.isShowStats = true;
        });
    };

    $scope.$watch('tmp.statsKeyword', function(val){
        if(!$scope.statsList){
            $scope.statsList = tmp.statsList;
        }else{
            $scope.statsList = _.filter(tmp.statsList, function(one){
                return one.key.contains(val);
            });
        }
    });

    $scope.changeReady = function(one){
		uiTips.confirm('Sure Switch Ready - ' + one.host + ':' + one.port +
		    ' from ' + one.ready + ' to ' + !one.ready + ' ?', function(){
			$http.post('/dashboard/zk/switch?id=' + id, one).success(function(data){
				if(data.flag){
				    uiTips.alert('Done - Now Ready : ' + data.ready);
				    one.ready = !one.ready;
				}
			});
		}, null);
    };

    $scope.updateParams = function(one){
        tmp.editOne = one;
        var paramList = [];
        if(!one.params){
            one.params = {};
        }
        _.each(_.keys(one.params), function(key){
            paramList.push({key: key, value: one.params[key]});
        });
        $scope.tmp.paramList = paramList;
        $scope.tmp.paramsDialogTitle = 'Server Address - ' + one.host + ':' + one.port;
        $scope.ctrl.isShowParamsUpdate = true;
    };

    $scope.saveParams = function(){
        var x = {};
        _.each($scope.tmp.paramList, function(it){
            x[it.key] = it.value;
        });
        tmp.editOne.params = x;
        $http.post('/dashboard/zk/update?id=' + id, tmp.editOne).success(function(data){
            if(data.flag){
                uiTips.alert('Update Ok');
                $scope.ctrl.isShowParamsUpdate = false;
            }
        });
    };

    $scope.updateWeight = function(one){
        uiTips.prompt('Update weight for - ' + one.host + ':' + one.port, function (val) {
            if(!(/^[\-\+]?([0-9]+)$/.test(val))){
                uiTips.alert('not a number - ' + val);
                return;
            }
            one.weight = parseInt(val);
            if(one.weight < 0 || one.weight > 10) {
                uiTips.alert('weight number must >= 0 and <= 10 - ' + val);
                return;
            }
			$http.post('/dashboard/zk/update?id=' + id, one).success(function(data){
				if(data.flag){
				    uiTips.alert('Done - Now Weight : ' + one.weight);
				}
			});
        }, one.weight);
    };
});