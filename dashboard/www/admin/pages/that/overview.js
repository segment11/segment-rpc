var md = angular.module('module_that/overview', ['base']);
md.controller('MainCtrl', function ($scope, $http, uiTips, uiValid) {
    $scope.tmp = {};
    $scope.ctrl = {};

    var tmp = {};

    var Page = window.Page;
    var params = Page.params();

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
        $http.get('/dashboard/remote/overview', { params: p }).success(function (data) {
            $scope.uriList = data.uriList;
            $scope.methodList = data.methodList;
        });
    };

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
			$http.post('/dashboard/zk/weight/update?id=' + id, one).success(function(data){
				if(data.flag){
				    uiTips.alert('Done - Now Weight : ' + data.weight);
				    one.weight = one.weight;
				}
			});
        }, one.weight);
    };
});