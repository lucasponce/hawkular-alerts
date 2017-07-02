angular.module( 'patternfly.charts' ).controller( 'ChartCtrl', function( $scope ) {
  $scope.used = 950;
  $scope.total = 1000;
  $scope.available =  $scope.total - $scope.used;
 
  $scope.chartConfig = patternfly.c3ChartDefaults().getDefaultDonutConfig('MHz Used');
  $scope.chartConfig.data = {
    type: "donut",
    columns: [
      ["Used", $scope.used],
      ["Available", $scope.total - $scope.used]
    ],
    groups: [
      ["used", "available"]
    ],
    order: null
  };
 
  $scope.getChart = function (chart) {
    $scope.chart = chart;
  }
 
  $scope.focusUsed = function () {
    $scope.chart.focus("Used");
  }
 
  $scope.updateAvailable = function (val) {
    $scope.available =  $scope.total - $scope.used;
  }
 
  $scope.submitform = function (val) {
    console.log("submitform");
    $scope.used = val;
    $scope.updateAvailable();
    $scope.chartConfig.data.columns = [["Used",$scope.used],["Available",$scope.available]];
  };
});
