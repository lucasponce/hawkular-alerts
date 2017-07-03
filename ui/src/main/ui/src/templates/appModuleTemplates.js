angular.module('hwk.appModule').run(['$templateCache', function($templateCache) {
  'use strict';

  $templateCache.put('src/actions/actions.html',
    "<div class=\"container-fluid\"><div class=\"row\"><div class=\"col-md-12\"><h1>Actions</h1><hr><div class=\"list-group list-view-pf\"><div class=\"list-group-item list-view-pf-stacked\"><div class=\"list-view-pf-actions\"><button class=\"btn btn-default\">Action</button><div class=\"dropdown pull-right dropdown-kebab-pf\"><button class=\"btn btn-link dropdown-toggle\" type=\"button\" id=\"dropdownKebabRight15\" data-toggle=\"dropdown\" aria-haspopup=\"true\" aria-expanded=\"true\"><span class=\"fa fa-ellipsis-v\"></span></button><ul class=\"dropdown-menu dropdown-menu-right\" aria-labelledby=\"dropdownKebabRight15\"><li><a href=\"#\">Action</a></li><li><a href=\"#\">Another action</a></li><li><a href=\"#\">Something else here</a></li><li role=\"separator\" class=\"divider\"></li><li><a href=\"#\">Separated link</a></li></ul></div></div><div class=\"list-view-pf-main-info\"><div class=\"list-view-pf-left\"><span class=\"fa fa-plane list-view-pf-icon-lg\"></span></div><div class=\"list-view-pf-body\"><div class=\"list-view-pf-description\"><div class=\"list-group-item-heading\">Event One <small>Feb 23, 2015 12:32 am</small></div><div class=\"list-group-item-text\">The following snippet of text is <a href=\"#\">rendered as link text</a>.</div></div><div class=\"list-view-pf-additional-info\"><div class=\"list-view-pf-additional-info-item list-view-pf-additional-info-item-donut-chart\"><span id=\"donut-chart-1\"></span></div><div class=\"list-view-pf-additional-info-item list-view-pf-additional-info-item-donut-chart\"><span id=\"donut-chart-2\"></span></div><div class=\"list-view-pf-additional-info-item list-view-pf-additional-info-item-donut-chart\"><span id=\"donut-chart-3\"></span></div><div class=\"list-view-pf-additional-info-item list-view-pf-additional-info-item-donut-chart\"><span id=\"donut-chart-4\"></span></div></div></div></div></div></div></div></div></div><!-- /container -->"
  );


  $templateCache.put('src/app/controllers/heading.html',
    "<span class=\"heading-class\">{{notificationGroup.heading}}</span> <span class=\"panel-counter sub-heading\">{{notificationGroup.unreadCount}} New {{notificationGroup.heading}}</span>"
  );


  $templateCache.put('src/dashboard/dashboard.html',
    "<div class=\"container-fluid container-cards-pf\"><div class=\"row row-cards-pf\"><!-- Important:  if you need to nest additional .row within a .row.row-cards-pf, do *not* use .row-cards-pf on the nested .row  --><div class=\"col-sm-6 col-md-3\"><div class=\"card-pf card-pf-accented card-pf-aggregate-status\"><h2 class=\"card-pf-title\"><span class=\"fa fa-shield\"></span><span class=\"card-pf-aggregate-status-count\">0</span> Ipsum</h2><div class=\"card-pf-body\"><p class=\"card-pf-aggregate-status-notifications\"><span class=\"card-pf-aggregate-status-notification\"><a href=\"#\" class=\"add\" data-toggle=\"tooltip\" data-placement=\"top\" title=\"Add Ipsum\"><span class=\"pficon pficon-add-circle-o\"></span></a></span></p></div></div></div><div class=\"col-sm-6 col-md-3\"><div class=\"card-pf card-pf-accented card-pf-aggregate-status\"><h2 class=\"card-pf-title\"><a href=\"#\"><span class=\"fa fa-shield\"></span><span class=\"card-pf-aggregate-status-count\">20</span> Amet</a></h2><div class=\"card-pf-body\"><p class=\"card-pf-aggregate-status-notifications\"><span class=\"card-pf-aggregate-status-notification\"><a href=\"#\"><span class=\"pficon pficon-error-circle-o\"></span>4</a></span> <span class=\"card-pf-aggregate-status-notification\"><a href=\"#\"><span class=\"pficon pficon-warning-triangle-o\"></span>1</a></span></p></div></div></div><div class=\"col-sm-6 col-md-3\"><div class=\"card-pf card-pf-accented card-pf-aggregate-status\"><h2 class=\"card-pf-title\"><a href=\"#\"><span class=\"fa fa-shield\"></span><span class=\"card-pf-aggregate-status-count\">9</span> Adipiscing</a></h2><div class=\"card-pf-body\"><p class=\"card-pf-aggregate-status-notifications\"><span class=\"card-pf-aggregate-status-notification\"><span class=\"pficon pficon-ok\"></span></span></p></div></div></div><div class=\"col-sm-6 col-md-3\"><div class=\"card-pf card-pf-accented card-pf-aggregate-status\"><h2 class=\"card-pf-title\"><a href=\"#\"><span class=\"fa fa-shield\"></span><span class=\"card-pf-aggregate-status-count\">12</span> Lorem</a></h2><div class=\"card-pf-body\"><p class=\"card-pf-aggregate-status-notifications\"><a href=\"#\"><span class=\"card-pf-aggregate-status-notification\"><span class=\"pficon pficon-error-circle-o\"></span>1</span></a></p></div></div></div></div><!-- /row --><div class=\"row row-cards-pf\"><!-- Important:  if you need to nest additional .row within a .row.row-cards-pf, do *not* use .row-cards-pf on the nested .row  --><div class=\"col-sm-6 col-md-3\"><div class=\"card-pf card-pf-accented card-pf-aggregate-status card-pf-aggregate-status-mini\"><h2 class=\"card-pf-title\"><span class=\"fa fa-rebel\"></span> <span class=\"card-pf-aggregate-status-count\">0</span> Ipsum</h2><div class=\"card-pf-body\"><p class=\"card-pf-aggregate-status-notifications\"><span class=\"card-pf-aggregate-status-notification\"><a href=\"#\" class=\"add\" data-toggle=\"tooltip\" data-placement=\"top\" title=\"Add Ipsum\"><span class=\"pficon pficon-add-circle-o\"></span></a></span></p></div></div></div><div class=\"col-sm-6 col-md-3\"><div class=\"card-pf card-pf-accented card-pf-aggregate-status card-pf-aggregate-status-mini\"><h2 class=\"card-pf-title\"><span class=\"fa fa-paper-plane\"></span> <a href=\"#\"><span class=\"card-pf-aggregate-status-count\">20</span> Amet</a></h2><div class=\"card-pf-body\"><p class=\"card-pf-aggregate-status-notifications\"><span class=\"card-pf-aggregate-status-notification\"><a href=\"#\"><span class=\"pficon pficon-error-circle-o\"></span>4</a></span></p></div></div></div><div class=\"col-sm-6 col-md-3\"><div class=\"card-pf card-pf-accented card-pf-aggregate-status card-pf-aggregate-status-mini\"><h2 class=\"card-pf-title\"><span class=\"pficon pficon-cluster\"></span> <a href=\"#\"><span class=\"card-pf-aggregate-status-count\">9</span> Adipiscing</a></h2><div class=\"card-pf-body\"><p class=\"card-pf-aggregate-status-notifications\"><span class=\"card-pf-aggregate-status-notification\"><span class=\"pficon pficon-ok\"></span></span></p></div></div></div><div class=\"col-sm-6 col-md-3\"><div class=\"card-pf card-pf-accented card-pf-aggregate-status card-pf-aggregate-status-mini\"><h2 class=\"card-pf-title\"><span class=\"pficon pficon-image\"></span> <a href=\"#\"><span class=\"card-pf-aggregate-status-count\">12</span> Lorem</a></h2><div class=\"card-pf-body\"><p class=\"card-pf-aggregate-status-notifications\"><a href=\"#\"><span class=\"card-pf-aggregate-status-notification\"><span class=\"pficon pficon-error-circle-o\"></span>1</span></a></p></div></div></div></div><div class=\"row row-cards-pf\"><div class=\"col-xs-12 col-sm-12 col-md-4\"><div class=\"card-pf\"><div class=\"card-pf-heading\"><h2 class=\"card-pf-title\">Top Utilized Clusters</h2></div><div class=\"card-pf-body\"><div class=\"progress-description\">RHOS6-Controller</div><div class=\"progress progress-label-top-right\"><div class=\"progress-bar progress-bar-danger\" role=\"progressbar\" aria-valuenow=\"95\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 95%\" data-toggle=\"tooltip\" title=\"95% Used\"><span><strong>190.0 of 200.0 GB</strong> Used</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"5\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 5%\" data-toggle=\"tooltip\" title=\"5% Available\"><span class=\"sr-only\">5% Available</span></div></div><div class=\"progress-description\">CFMEQE-Cluster</div><div class=\"progress progress-label-top-right\"><div class=\"progress-bar progress-bar-success\" role=\"progressbar\" aria-valuenow=\"50\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 50%\" data-toggle=\"tooltip\" title=\"50% Used\"><span><strong>100.0 of 200.0 GB</strong> Used</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"50\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 50%\" data-toggle=\"tooltip\" title=\"50% Available\"><span class=\"sr-only\">50% Available</span></div></div><div class=\"progress-description\">RHOS-Undercloud</div><div class=\"progress progress-label-top-right\"><div class=\"progress-bar progress-bar-warning\" role=\"progressbar\" aria-valuenow=\"70\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 70%\" data-toggle=\"tooltip\" title=\"70% Used\"><span><strong>140.0 of 200.0 GB</strong> Used</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"30\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 30%\" data-toggle=\"tooltip\" title=\"30% Available\"><span class=\"sr-only\">30% Available</span></div></div><div class=\"progress-description\">RHEL6-Controller</div><div class=\"progress progress-label-top-right\"><div class=\"progress-bar progress-bar-warning\" role=\"progressbar\" aria-valuenow=\"76.5\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 76.5%\" data-toggle=\"tooltip\" title=\"76.5% Used\"><span><strong>153.0 of 200.0 GB</strong> Used</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"23.5\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 23.5%\" data-toggle=\"tooltip\" title=\"23.5% Available\"><span class=\"sr-only\">23.5% Available</span></div></div></div></div></div><div class=\"col-xs-12 col-sm-6 col-md-4\"><div class=\"card-pf\"><div class=\"card-pf-heading\"><h2 class=\"card-pf-title\">Quotas</h2></div><div class=\"card-pf-body\"><div class=\"progress-container progress-description-left progress-label-right\"><div class=\"progress-description\">CPU</div><div class=\"progress\"><div class=\"progress-bar\" role=\"progressbar\" aria-valuenow=\"25\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 25%\" data-toggle=\"tooltip\" title=\"25% Used\"><span><strong>115 of 460</strong> MHz</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"75\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 75%\" data-toggle=\"tooltip\" title=\"75% Available\"><span class=\"sr-only\">75% Available</span></div></div></div><div class=\"progress-container progress-description-left progress-label-right\"><div class=\"progress-description\">Memory</div><div class=\"progress\"><div class=\"progress-bar\" role=\"progressbar\" aria-valuenow=\"50\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 50%\" data-toggle=\"tooltip\" title=\"8 GB Used\"><span><strong>8 of 16</strong> GB</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"50\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 50%\" data-toggle=\"tooltip\" title=\"8 GB Available\"><span class=\"sr-only\">50% Available</span></div></div></div><div class=\"progress-container progress-description-left progress-label-right\"><div class=\"progress-description\">Pods</div><div class=\"progress\"><div class=\"progress-bar\" role=\"progressbar\" aria-valuenow=\"62.5\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 62.5%\" data-toggle=\"tooltip\" title=\"62.5% Used\"><span><strong>5 of 8</strong> Total</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"37.5\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 37.5%\" data-toggle=\"tooltip\" title=\"37.5% Available\"><span class=\"sr-only\">37.5% Available</span></div></div></div><div class=\"progress-container progress-description-left progress-label-right\"><div class=\"progress-description\">Services</div><div class=\"progress\"><div class=\"progress-bar\" role=\"progressbar\" aria-valuenow=\"100\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 100%\" data-toggle=\"tooltip\" title=\"100% Used\"><span><strong>2 of 2</strong> Total</span></div></div></div></div></div></div><div class=\"col-xs-12 col-sm-6 col-md-4\"><div class=\"card-pf\"><div class=\"card-pf-heading\"><h2 class=\"card-pf-title\">Quotas</h2></div><div class=\"card-pf-body\"><div class=\"progress-container progress-description-left\"><div class=\"progress-description\">CPU</div><div class=\"progress\"><div class=\"progress-bar\" role=\"progressbar\" aria-valuenow=\"25\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 25%\" data-toggle=\"tooltip\" title=\"25% Used\"><span class=\"sr-only\">25% Used</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"75\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 75%\" data-toggle=\"tooltip\" title=\"75% Available\"><span class=\"sr-only\">75% Available</span></div></div></div><div class=\"progress-container progress-description-left\"><div class=\"progress-description\">Memory</div><div class=\"progress\"><div class=\"progress-bar\" role=\"progressbar\" aria-valuenow=\"50\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 50%\" data-toggle=\"tooltip\" title=\"8 GB Used\"><span class=\"sr-only\">50% Used</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"50\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 50%\" data-toggle=\"tooltip\" title=\"8 GB Available\"><span class=\"sr-only\">50% Available</span></div></div></div><div class=\"progress-container progress-description-left\"><div class=\"progress-description\">Pods</div><div class=\"progress\"><div class=\"progress-bar\" role=\"progressbar\" aria-valuenow=\"62.5\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 62.5%\" data-toggle=\"tooltip\" title=\"62.5% Used\"><span class=\"sr-only\">62.5% Used</span></div><div class=\"progress-bar progress-bar-remaining\" role=\"progressbar\" aria-valuenow=\"37.5\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 37.5%\" data-toggle=\"tooltip\" title=\"37.5% Available\"><span class=\"sr-only\">37.5% Available</span></div></div></div><div class=\"progress-container progress-description-left\"><div class=\"progress-description\">Services</div><div class=\"progress\"><div class=\"progress-bar\" role=\"progressbar\" aria-valuenow=\"100\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 100%\" data-toggle=\"tooltip\" title=\"100% Used\"><span class=\"sr-only\">100% Used</span></div></div></div></div></div></div></div><!-- /row --><div class=\"row row-cards-pf\"><!-- Important:  if you need to nest additional .row within a .row.row-cards-pf, do *not* use .row-cards-pf on the nested .row  --><div class=\"col-md-12\"><div class=\"card-pf card-pf-utilization\"><div class=\"card-pf-heading\"><p class=\"card-pf-heading-details\">Last 30 days</p><h2 class=\"card-pf-title\">Utilization</h2></div><div class=\"card-pf-body\"><div class=\"row\"><div class=\"col-xs-12 col-sm-4 col-md-4\"><h3 class=\"card-pf-subtitle\">CPU</h3><p class=\"card-pf-utilization-details\"><span class=\"card-pf-utilization-card-details-count\">50</span> <span class=\"card-pf-utilization-card-details-description\"><span class=\"card-pf-utilization-card-details-line-1\">Available</span> <span class=\"card-pf-utilization-card-details-line-2\">of 1000 MHz</span></span></p><div id=\"chart-pf-donut-1\"></div><div class=\"chart-pf-sparkline\" id=\"chart-pf-sparkline-1\"></div><script>var donutConfig = $().c3ChartDefaults().getDefaultDonutConfig('A');\n" +
    "                donutConfig.bindto = '#chart-pf-donut-1';\n" +
    "                donutConfig.color =  {\n" +
    "                  pattern: [\"#cc0000\",\"#D1D1D1\"]\n" +
    "                };\n" +
    "                donutConfig.data = {\n" +
    "                  type: \"donut\",\n" +
    "                  columns: [\n" +
    "                    [\"Used\", 95],\n" +
    "                    [\"Available\", 5]\n" +
    "                  ],\n" +
    "                  groups: [\n" +
    "                    [\"used\", \"available\"]\n" +
    "                  ],\n" +
    "                  order: null\n" +
    "                };\n" +
    "                donutConfig.tooltip = {\n" +
    "                  contents: function (d) {\n" +
    "                    return '<span class=\"donut-tooltip-pf\" style=\"white-space: nowrap;\">' +\n" +
    "                      Math.round(d[0].ratio * 100) + '%' + ' MHz ' + d[0].name +\n" +
    "                      '</span>';\n" +
    "                  }\n" +
    "                };\n" +
    "\n" +
    "                var chart1 = c3.generate(donutConfig);\n" +
    "                var donutChartTitle = d3.select(\"#chart-pf-donut-1\").select('text.c3-chart-arcs-title');\n" +
    "                donutChartTitle.text(\"\");\n" +
    "                donutChartTitle.insert('tspan').text(\"950\").classed('donut-title-big-pf', true).attr('dy', 0).attr('x', 0);\n" +
    "                donutChartTitle.insert('tspan').text(\"MHz Used\").classed('donut-title-small-pf', true).attr('dy', 20).attr('x', 0);\n" +
    "\n" +
    "                var sparklineConfig = $().c3ChartDefaults().getDefaultSparklineConfig();\n" +
    "                sparklineConfig.bindto = '#chart-pf-sparkline-1';\n" +
    "                sparklineConfig.data = {\n" +
    "                  columns: [\n" +
    "                    ['%', 10, 50, 28, 20, 31, 27, 60, 36, 52, 55, 62, 68, 69, 88, 74, 88, 95],\n" +
    "                  ],\n" +
    "                  type: 'area'\n" +
    "                };\n" +
    "                var chart2 = c3.generate(sparklineConfig);</script></div><div class=\"col-xs-12 col-sm-4 col-md-4\"><h3 class=\"card-pf-subtitle\">Memory</h3><p class=\"card-pf-utilization-details\"><span class=\"card-pf-utilization-card-details-count\">256</span> <span class=\"card-pf-utilization-card-details-description\"><span class=\"card-pf-utilization-card-details-line-1\">Available</span> <span class=\"card-pf-utilization-card-details-line-2\">of 432 GB</span></span></p><div id=\"chart-pf-donut-2\"></div><div class=\"chart-pf-sparkline\" id=\"chart-pf-sparkline-2\"></div><script>var donutConfig = $().c3ChartDefaults().getDefaultDonutConfig('A');\n" +
    "                donutConfig.bindto = '#chart-pf-donut-2';\n" +
    "                donutConfig.color =  {\n" +
    "                  pattern: [\"#3f9c35\",\"#D1D1D1\"]\n" +
    "                };\n" +
    "                donutConfig.data = {\n" +
    "                  type: \"donut\",\n" +
    "                  columns: [\n" +
    "                    [\"Used\", 41],\n" +
    "                    [\"Available\", 59]\n" +
    "                  ],\n" +
    "                  groups: [\n" +
    "                    [\"used\", \"available\"]\n" +
    "                  ],\n" +
    "                  order: null\n" +
    "                };\n" +
    "                donutConfig.tooltip = {\n" +
    "                  contents: function (d) {\n" +
    "                    return '<span class=\"donut-tooltip-pf\" style=\"white-space: nowrap;\">' +\n" +
    "                      Math.round(d[0].ratio * 100) + '%' + ' GB ' + d[0].name +\n" +
    "                      '</span>';\n" +
    "                  }\n" +
    "                };\n" +
    "\n" +
    "                var chart3 = c3.generate(donutConfig);\n" +
    "                var donutChartTitle = d3.select(\"#chart-pf-donut-2\").select('text.c3-chart-arcs-title');\n" +
    "                donutChartTitle.text(\"\");\n" +
    "                donutChartTitle.insert('tspan').text(\"176\").classed('donut-title-big-pf', true).attr('dy', 0).attr('x', 0);\n" +
    "                donutChartTitle.insert('tspan').text(\"GB Used\").classed('donut-title-small-pf', true).attr('dy', 20).attr('x', 0);\n" +
    "\n" +
    "                var sparklineConfig = $().c3ChartDefaults().getDefaultSparklineConfig();\n" +
    "                sparklineConfig.bindto = '#chart-pf-sparkline-2';\n" +
    "                sparklineConfig.data = {\n" +
    "                  columns: [\n" +
    "                    ['%', 35, 36, 20, 30, 31, 22, 44, 36, 40, 41, 55, 52, 48, 48, 50, 40, 41],\n" +
    "                  ],\n" +
    "                  type: 'area'\n" +
    "                };\n" +
    "                var chart4 = c3.generate(sparklineConfig);</script></div><div class=\"col-xs-12 col-sm-4 col-md-4\"><h3 class=\"card-pf-subtitle\">Network</h3><p class=\"card-pf-utilization-details\"><span class=\"card-pf-utilization-card-details-count\">200</span> <span class=\"card-pf-utilization-card-details-description\"><span class=\"card-pf-utilization-card-details-line-1\">Available</span> <span class=\"card-pf-utilization-card-details-line-2\">of 1300 Gbps</span></span></p><div id=\"chart-pf-donut-3\"></div><div class=\"chart-pf-sparkline\" id=\"chart-pf-sparkline-3\"></div><script>var donutConfig = $().c3ChartDefaults().getDefaultDonutConfig('A');\n" +
    "                donutConfig.bindto = '#chart-pf-donut-3';\n" +
    "                donutConfig.color =  {\n" +
    "                  pattern: [\"#EC7A08\",\"#D1D1D1\"]\n" +
    "                };\n" +
    "                donutConfig.data = {\n" +
    "                  type: \"donut\",\n" +
    "                  columns: [\n" +
    "                    [\"Used\", 85],\n" +
    "                    [\"Available\", 15]\n" +
    "                  ],\n" +
    "                  groups: [\n" +
    "                    [\"used\", \"available\"]\n" +
    "                  ],\n" +
    "                  order: null\n" +
    "                };\n" +
    "                donutConfig.tooltip = {\n" +
    "                  contents: function (d) {\n" +
    "                    return '<span class=\"donut-tooltip-pf\" style=\"white-space: nowrap;\">' +\n" +
    "                      Math.round(d[0].ratio * 100) + '%' + ' Gbps ' + d[0].name +\n" +
    "                      '</span>';\n" +
    "                  }\n" +
    "                };\n" +
    "\n" +
    "                var chart5 = c3.generate(donutConfig);\n" +
    "                var donutChartTitle = d3.select(\"#chart-pf-donut-3\").select('text.c3-chart-arcs-title');\n" +
    "                donutChartTitle.text(\"\");\n" +
    "                donutChartTitle.insert('tspan').text(\"1100\").classed('donut-title-big-pf', true).attr('dy', 0).attr('x', 0);\n" +
    "                donutChartTitle.insert('tspan').text(\"Gbps Used\").classed('donut-title-small-pf', true).attr('dy', 20).attr('x', 0);\n" +
    "\n" +
    "                var sparklineConfig = $().c3ChartDefaults().getDefaultSparklineConfig();\n" +
    "                sparklineConfig.bindto = '#chart-pf-sparkline-3';\n" +
    "                sparklineConfig.data = {\n" +
    "                  columns: [\n" +
    "                    ['%', 60, 55, 70, 44, 31, 67, 54, 46, 58, 75, 62, 68, 69, 88, 74, 88, 85],\n" +
    "                  ],\n" +
    "                  type: 'area'\n" +
    "                };\n" +
    "                var chart6 = c3.generate(sparklineConfig);</script></div></div></div></div></div></div><!-- /row --></div><!-- /container -->"
  );


  $templateCache.put('src/triggers/triggers.html',
    "<div class=\"container-fluid\"><div class=\"row\"><div class=\"col-md-12\"><h1>Triggers</h1><table class=\"datatable table table-striped table-bordered\"><thead><tr><th>Id<th>Name<th>Description<th>Actions<th>Enabled<tbody><tr><td>hello-world-trigger<td>Hello World Trigger<td>A mandatory Hello World Trigger<td><i class=\"fa fa-envelope-o\"></i> notify-to-admins<td><i class=\"fa fa-check\"></i></table></div></div></div><!-- /container -->"
  );

}]);
