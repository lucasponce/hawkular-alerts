angular.module('hwk.dashboardModule').controller( 'hwk.dashboardController', ['$scope', '$rootScope', '$resource', '$window', '$interval', '$q', 'hwk.dashboardService',
  function ($scope, $rootScope, $resource, $window, $interval, $q, dashboardService) {
    'use strict';

    console.log("[Dashboard] Start: " + new Date());
    console.log("[Dashboard] $rootScope.selectedTenant " + $rootScope.selectedTenant);

    $scope.refresh = true;

    var ONE_HOUR = 60 * 60 * 1000,
      ONE_DAY = 24 * ONE_HOUR,
      ONE_WEEK = 7 * ONE_DAY,
      ONE_SECOND = 1000;

    var PING_INTERVAL = 2000;

    var selectedTenant = $rootScope.selectedTenant;

    var OPEN = 0, ACKNOWLEDGED = 1, RESOLVED = 2, EVENTS = 3;

    var dataTimeline = [
      {
        name: 'Open Alerts',
        data: []
      },
      {
        name: 'Acknowledged Alerts',
        data: []
      },
      {
        name: 'Resolved Alerts',
        data: []
      },
      {
        name: 'Events',
        data: []
      }
    ];

    var today = new Date();

    var onTimelineColor = function (eventData) {
      switch (eventData.name) {
      case 'Open Alerts':
        return '#c00';
      case 'Acknowledged Alerts':
        return '#ec7a08';
      case 'Resolved Alerts':
        return '#3f9c35';
      case 'Events':
        return '';
      }
      return '';
    };

    var onTimelineClick = function (eventTimeline) {
      $scope.eventTimeline = eventTimeline;
      console.log(eventTimeline);
      $scope.$apply();
    };

    var timeline;
    var element;
    var intervalRef;

    var updateDashboard = function () {
      console.log("[Dashboard] Updating data for " + selectedTenant + " at " + new Date());

      var promise1 = dashboardService.Alert(selectedTenant).query();
      var promise2 = dashboardService.Event(selectedTenant).query();

      $q.all([promise1.$promise, promise2.$promise]).then(function (result) {
        var updatedAlerts = result[0];
        var updatedEvents = result[1];

        dataTimeline[OPEN].data = [];
        dataTimeline[ACKNOWLEDGED].data = [];
        dataTimeline[RESOLVED].data = [];
        dataTimeline[EVENTS].data = [];

        var minDate = Number.MAX_VALUE, maxDate = 0;
        var i;
        for (i = 0; i < updatedAlerts.length; i++) {
          var status = updatedAlerts[i].status;
          var stime = updatedAlerts[i].lifecycle[updatedAlerts[i].lifecycle.length - 1].stime;
          if (stime < minDate) {
            minDate = stime;
          }
          if (stime > maxDate) {
            maxDate = stime;
          }
          switch (status) {
          case 'OPEN':
            dataTimeline[OPEN].data.push({
              date: new Date(stime),
              details: updatedAlerts[i]
            });
            break;
          case 'ACKNOWLEDGED':
            dataTimeline[ACKNOWLEDGED].data.push({
              date: new Date(stime),
              details: updatedAlerts[i]
            });
            break;
          case 'RESOLVED':
            dataTimeline[RESOLVED].data.push({
              date: new Date(stime),
              details: updatedAlerts[i]
            });
            break;
          }
        }

        for (i = 0; i < updatedEvents.length; i++) {
          var ctime = updatedEvents[i].ctime;
          if (ctime < minDate) {
            minDate = ctime;
          }
          if (ctime > maxDate) {
            maxDate = ctime;
          }
          dataTimeline[EVENTS].data.push({
            date: new Date(updatedEvents[i].ctime),
            details: updatedEvents[i]
          });
        }

        $scope.openAlerts = dataTimeline[OPEN].data;
        $scope.acknowledgedAlerts = dataTimeline[ACKNOWLEDGED].data;
        $scope.resolvedAlerts = dataTimeline[RESOLVED].data;
        $scope.events = dataTimeline[EVENTS].data;

        console.log('[Dashboard] Update timeline data ' + new Date());
        // console.log(JSON.stringify(dataTimeline));

      // [lponce] remove objects to re-draw
        d3.select('#pf-timeline').selectAll('div').remove();

        var startTimeline = minDate - ((maxDate - minDate) * 0.25);
        var endTimeline = maxDate + ((maxDate - minDate) * 0.25);

        timeline = d3.chart.timeline()
          .end(new Date(endTimeline))
          .start(new Date(startTimeline))
          .eventLineColor(onTimelineColor)
          .eventClick(onTimelineClick)
          .eventGrouping(ONE_SECOND);
        element = d3.select('#pf-timeline').append('div').datum(dataTimeline);
        timeline(element);
      });

    };

    // Init scope to show at least an empty chart
    $scope.openAlerts = dataTimeline[OPEN].data;
    $scope.acknowledgedAlerts = dataTimeline[ACKNOWLEDGED].data;
    $scope.resolvedAlerts = dataTimeline[RESOLVED].data;
    $scope.events = dataTimeline[EVENTS].data;

    // Initial dashboard if tenant is valid
    if (selectedTenant && selectedTenant.length > 0) {
      // Initial blank timeline
      timeline = d3.chart.timeline();
      element = d3.select('#pf-timeline').append('div').datum(dataTimeline);
      timeline(element);

      updateDashboard();
      if ($scope.refresh) {
        intervalRef = $interval(updateDashboard, PING_INTERVAL);
      }
    }

    // Watch for tenant changes
    var watchRef = $rootScope.$watch('selectedTenant', function (newTenant, oldTenant) {
      selectedTenant = newTenant;
      console.log('[Dashboard] New Tenant: ' + selectedTenant);
      if (intervalRef) {
        $interval.cancel(intervalRef);
      }
      // Create an $interval only if tenant is valid
      if (selectedTenant && selectedTenant.length > 0 && $scope.refresh) {
        intervalRef = $interval(updateDashboard, PING_INTERVAL);
      }
    });

    // When dashboard controler is destroyed, the $interval and $watch are removed.
    $scope.$on('$destroy', function() {
      $interval.cancel(intervalRef);
      watchRef();
    });

    angular.element($window).bind('resize', function () {
      if (selectedTenant && selectedTenant.length > 0) {
        timeline(element);
      }
    });

    $scope.updateRefresh = function () {
      if ($scope.refresh) {
        console.log('[Dashboard] Stopping refresh');
        $interval.cancel(intervalRef);
        $scope.refresh = false;
      } else {
        console.log('[Dashboard] Starting refresh');
        if (selectedTenant && selectedTenant.length > 0) {
          intervalRef = $interval(updateDashboard, PING_INTERVAL);
        }
        $scope.refresh = true;
      }
    };
  }
]);
