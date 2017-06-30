package org.hawkular.alerter.prometheus.rest;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.hawkular.alerts.api.json.JsonUtil.fromJson;
import static org.hawkular.alerts.api.json.JsonUtil.toJson;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.api.services.AlertsService;
import org.hawkular.alerts.engine.StandaloneAlerts;
import org.hawkular.commons.log.MsgLogger;
import org.hawkular.commons.log.MsgLogging;
import org.hawkular.handlers.RestEndpoint;
import org.hawkular.handlers.RestHandler;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@RestEndpoint(path = "/")
public class PrometheusHandler implements RestHandler {
    public final static String ACCEPT = "Accept";
    public final static String CONTENT_TYPE = "Content-Type";
    public final static String APPLICATION_JSON = "application/json";
    private static final MsgLogger log = MsgLogging.getMsgLogger(PrometheusHandler.class);

    AlertsService alerts;

    public PrometheusHandler() {
        alerts = StandaloneAlerts.getAlertsService();
    }

    @Override
    public void initRoutes(String baseUrl, Router router) {
        router.get(baseUrl + "/status").handler(this::status);
        router.post(baseUrl + "/notification").handler(this::notification);
    }

    void status(RoutingContext routing) {
        routing.vertx()
                .executeBlocking(future -> future.complete("Status"),
                        res -> response(routing, OK.code(), res.result()));
    }

    void notification(RoutingContext routing) {
        routing.vertx()
                .executeBlocking(future -> {
                    String json = routing.getBodyAsString();
                    Notification notification = fromJson(json, Notification.class);
                    String tenantId = notification.getGroupLabels().getOrDefault("tenant", "prometheus");
                    String dataId = notification.getGroupLabels().get("dataId");
                    String category = "prometheus";
                    String text = notification.getCommonAnnotations().get("description");
                    text = null != text ? text : notification.getCommonAnnotations().get("summary");
                    text = null != text ? text : notification.getGroupLabels().getOrDefault("alertname", "notification");
                    Map<String, String> context = new HashMap<>();
                    context.put("alertname", notification.getGroupLabels().getOrDefault("alertname", "unknown"));
                    context.put("json", json);
                    Event event = new Event(tenantId, UUID.randomUUID().toString(), dataId, category, text, context);
                    log.debugf("Adding Prometheus Notification Event %s", event);
                    try {
                        alerts.addEvents(Collections.singleton(event));
                        future.complete("Success");
                    } catch (Exception e) {
                        future.fail(e);
                    }
                }, res -> {
                    if (res.succeeded()) {
                        response(routing, OK.code(), res.result());
                    } else {
                        response(routing, BAD_REQUEST.code(), res.cause().getMessage());
                    }
                });
    }

    public static void response(RoutingContext routing, int code, Object o) {
        routing.response()
                .putHeader(ACCEPT, APPLICATION_JSON)
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(code)
                .end(toJson(o));
    }
}
