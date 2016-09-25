/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.alerts.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import static org.hawkular.alerts.rest.HawkularAlertsApp.TENANT_HEADER_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.hawkular.alerts.api.exception.NotFoundException;
import org.hawkular.alerts.api.json.GroupConditionsInfo;
import org.hawkular.alerts.api.json.GroupMemberInfo;
import org.hawkular.alerts.api.json.UnorphanMemberInfo;
import org.hawkular.alerts.api.model.condition.Condition;
import org.hawkular.alerts.api.model.dampening.Dampening;
import org.hawkular.alerts.api.model.paging.Page;
import org.hawkular.alerts.api.model.paging.Pager;
import org.hawkular.alerts.api.model.trigger.FullTrigger;
import org.hawkular.alerts.api.model.trigger.Mode;
import org.hawkular.alerts.api.model.trigger.Trigger;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.hawkular.alerts.api.services.TriggersCriteria;
import org.hawkular.alerts.rest.ResponseUtil.ApiError;
import org.jboss.logging.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST endpoint for triggers
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Path("/triggers")
@Api(value = "/triggers")
public class TriggersHandler {
    private static final Logger log = Logger.getLogger(TriggersHandler.class);

    @HeaderParam(TENANT_HEADER_NAME)
    String tenantId;

    @EJB
    DefinitionsService definitions;

    public TriggersHandler() {
        log.debug("Creating instance.");
    }

    @GET
    @Path("/")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get triggers with optional filtering.",
            response = Trigger.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully fetched list of triggers."),
            @ApiResponse(code = 400, message = "Bad request/Invalid Parameters.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    public Response findTriggers(
            @ApiParam(required = false, value = "Filter out triggers for unspecified triggerIds, " +
                    "comma separated list of trigger IDs.")
            @QueryParam("triggerIds")
            final String triggerIds,
            @ApiParam(required = false, value = "Filter out triggers for unspecified tags, comma separated list of "
                    + "tags, each tag of format \'name|value\'. Specify \'*\' for value to match all values.")
            @QueryParam("tags")
            final String tags,
            @ApiParam(required = false, value = "Return only thin triggers. Currently Ignored.")
            @QueryParam("thin")
            final Boolean thin,
            @Context final UriInfo uri) {
        Pager pager = RequestUtil.extractPaging(uri);
        try {
            TriggersCriteria criteria = buildCriteria(triggerIds, tags, thin);
            Page<Trigger> triggerPage = definitions.getTriggers(tenantId, criteria, pager);
            if (log.isDebugEnabled()) {
                log.debug("Triggers: " + triggerPage);
            }
            if (isEmpty(triggerPage)) {
                return ResponseUtil.ok(triggerPage);
            }
            return ResponseUtil.paginatedOk(triggerPage, uri);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    private TriggersCriteria buildCriteria(String triggerIds, String tags, Boolean thin) {
        TriggersCriteria criteria = new TriggersCriteria();
        if (!isEmpty(triggerIds)) {
            criteria.setTriggerIds(Arrays.asList(triggerIds.split(",")));
        }
        if (!isEmpty(tags)) {
            String[] tagTokens = tags.split(",");
            Map<String, String> tagsMap = new HashMap<>(tagTokens.length);
            for (String tagToken : tagTokens) {
                String[] fields = tagToken.split("\\|");
                if (fields.length == 2) {
                    tagsMap.put(fields[0], fields[1]);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Invalid Tag Criteria " + Arrays.toString(fields));
                    }
                    throw new IllegalArgumentException( "Invalid Tag Criteria " + Arrays.toString(fields) );
                }
            }
            criteria.setTags(tagsMap);
        }
        if (null != thin) {
            criteria.setThin(thin.booleanValue());
        }

        return criteria;
    }

    @GET
    @Path("/groups/{groupId}/members")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Find all Group Member Trigger Definitions.",
            notes = "Pagination is not yet implemented.",
            response = Trigger.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully fetched list of triggers."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    public Response findGroupMembers(
            @ApiParam(value = "Group TriggerId.", required = true)
            @PathParam("groupId")
            final String groupId,
            @ApiParam(value = "include Orphan members? No if omitted.", required = false)
            @QueryParam("includeOrphans")
            final boolean includeOrphans) {
        try {
            Collection<Trigger> members = definitions.getMemberTriggers(tenantId, groupId, includeOrphans);
            if (log.isDebugEnabled()) {
                log.debug("Member Triggers: " + members);
            }
            return ResponseUtil.ok(members);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @POST
    @Path("/")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new trigger.",
            notes = "Return created trigger.",
            response = Trigger.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger created."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters", response = ApiError.class)
    })
    public Response createTrigger(
            @ApiParam(value = "Trigger definition to be created.", name = "trigger", required = true)
            final Trigger trigger) {
        try {
            if (null != trigger) {
                if (isEmpty(trigger.getId())) {
                    trigger.setId(Trigger.generateId());
                } else if (definitions.getTrigger(tenantId, trigger.getId()) != null) {
                    return ResponseUtil.badRequest("Trigger with ID [" + trigger.getId() + "] exists.");
                }
                definitions.addTrigger(tenantId, trigger);
                log.debug("Trigger: " + trigger.toString());
                return ResponseUtil.ok(trigger);
            } else {
                return ResponseUtil.badRequest("Trigger is null");
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @POST
    @Path("/trigger")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new full trigger (trigger, dampenings and conditions).",
            notes = "Return created full trigger.",
            response = FullTrigger.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, FullTrigger created."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class)
    })
    public Response createFullTrigger(
            @ApiParam(value = "FullTrigger (trigger, dampenings, conditions) to be created.",
                    name = "fullTrigger", required = true)
            final FullTrigger fullTrigger) {
        if (fullTrigger == null || fullTrigger.getTrigger() == null) {
            return ResponseUtil.badRequest("Trigger is empty ");
        }
        try {
            Trigger trigger = fullTrigger.getTrigger();
            trigger.setTenantId(tenantId);
            if (isEmpty(trigger.getId())) {
                trigger.setId(Trigger.generateId());
            } else if (definitions.getTrigger(tenantId, trigger.getId()) != null) {
                return ResponseUtil.badRequest("Trigger with ID [" + trigger.getId() + "] exists.");
            }
            definitions.addTrigger(tenantId, trigger);
            log.debug("Trigger: " + trigger.toString());
            for (Dampening dampening : fullTrigger.getDampenings()) {
                dampening.setTenantId(tenantId);
                dampening.setTriggerId(trigger.getId());
                boolean exist = (definitions.getDampening(tenantId, dampening.getDampeningId()) != null);
                if (exist) {
                    definitions.removeDampening(tenantId, dampening.getDampeningId());
                }
                definitions.addDampening(tenantId, dampening);
                log.debug("Dampening: " + dampening.toString());
            }
            fullTrigger.getConditions().stream().forEach(c -> {
                c.setTenantId(tenantId);
                c.setTriggerId(trigger.getId());
            });
            List<Condition> firingConditions = fullTrigger.getConditions().stream()
                    .filter(c -> c.getTriggerMode() == Mode.FIRING)
                    .collect(Collectors.toList());
            if (firingConditions != null && !firingConditions.isEmpty()) {
                definitions.setConditions(tenantId, trigger.getId(), Mode.FIRING, firingConditions);
                log.debug("Conditions: " + firingConditions);
            }
            List<Condition> autoResolveConditions = fullTrigger.getConditions().stream()
                    .filter(c -> c.getTriggerMode() == Mode.AUTORESOLVE)
                    .collect(Collectors.toList());
            if (autoResolveConditions != null && !autoResolveConditions.isEmpty()) {
                definitions.setConditions(tenantId, trigger.getId(), Mode.AUTORESOLVE, autoResolveConditions);
                log.debug("Conditions:" + autoResolveConditions);
            }
            return ResponseUtil.ok(fullTrigger);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @POST
    @Path("/groups")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new group trigger.",
            notes = "Returns created group trigger.",
            response = Trigger.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Group Trigger Created."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class)
    })
    public Response createGroupTrigger(
            @ApiParam(value = "Trigger definition to be created.", name = "groupTrigger", required = true)
            final Trigger groupTrigger) {
        try {
            if (null != groupTrigger) {
                if (isEmpty(groupTrigger.getId())) {
                    groupTrigger.setId(Trigger.generateId());
                } else if (definitions.getTrigger(tenantId, groupTrigger.getId()) != null) {
                    return ResponseUtil.badRequest("Trigger with ID [" + groupTrigger.getId() + "] exists.");
                }
                definitions.addGroupTrigger(tenantId, groupTrigger);
                log.debug("Group Trigger: " + groupTrigger.toString());
                return ResponseUtil.ok(groupTrigger);
            } else {
                return ResponseUtil.badRequest("Trigger is null");
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @POST
    @Path("/groups/members")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new member trigger for a parent trigger.",
            notes = "Returns Member Trigger created if operation finished correctly.",
            response = Trigger.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Member Trigger Created."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 404, message = "Group trigger not found.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class)
    })
    public Response createGroupMember(
            @ApiParam(value = "Group member trigger to be created.", name = "groupMember", required = true)
            final GroupMemberInfo groupMember) {
        try {
            if (null == groupMember) {
                return ResponseUtil.badRequest("MemberTrigger is null");
            }
            String groupId = groupMember.getGroupId();
            if (isEmpty(groupId)) {
                return ResponseUtil.badRequest("MemberTrigger groupId is null");
            }
            Trigger child = definitions.addMemberTrigger(tenantId, groupId, groupMember.getMemberId(),
                    groupMember.getMemberName(),
                    groupMember.getMemberDescription(),
                    groupMember.getMemberContext(),
                    groupMember.getMemberTags(),
                    groupMember.getDataIdMap());
            if (log.isDebugEnabled()) {
                log.debug("Child Trigger: " + child.toString());
            }
            return ResponseUtil.ok(child);

        } catch (NotFoundException e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.notFound(e.getMessage());
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/{triggerId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get an existing trigger definition.",
            response = Trigger.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger found."),
            @ApiResponse(code = 404, message = "Trigger not found.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    public Response getTrigger(
            @ApiParam(value = "Trigger definition id to be retrieved.", required = true)
            @PathParam("triggerId")
            final String triggerId) {
        try {
            Trigger found = definitions.getTrigger(tenantId, triggerId);
            if (found != null) {
                log.debug("Trigger: " + found);
                return ResponseUtil.ok(found);
            } else {
                return ResponseUtil.notFound("triggerId: " + triggerId + " not found");
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/trigger/{triggerId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get an existing full trigger definition (trigger, dampenings and conditions).",
            response = FullTrigger.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, FullTrigger found."),
            @ApiResponse(code = 404, message = "Trigger not found.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    public Response getFullTrigger(
            @ApiParam(value = "Full Trigger definition id to be retrieved.", required = true)
            @PathParam("triggerId")
            final String triggerId) {
        try {
            Trigger found = definitions.getTrigger(tenantId, triggerId);
            if (found != null) {
                log.debug("Trigger: " + found);
                List<Dampening> dampenings = new ArrayList<>(definitions.getTriggerDampenings(tenantId, found.getId(),
                        null));
                List<Condition> conditions = new ArrayList<>(definitions.getTriggerConditions(tenantId, found.getId(),
                        null));
                FullTrigger fullTrigger = new FullTrigger(found, dampenings, conditions);
                return ResponseUtil.ok(fullTrigger);
            } else {
                return ResponseUtil.notFound("triggerId: " + triggerId + " not found");
            }

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @PUT
    @Path("/{triggerId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update an existing trigger definition.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger updated."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class),
            @ApiResponse(code = 404, message = "Trigger doesn't exist.", response = ApiError.class)
    })
    public Response updateTrigger(
            @ApiParam(value = "Trigger definition id to be updated.", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Updated trigger definition.", name = "trigger", required = true)
            final Trigger trigger) {
        try {
            if (trigger != null && !isEmpty(triggerId)) {
                trigger.setId(triggerId);
            }
            definitions.updateTrigger(tenantId, trigger);
            if (log.isDebugEnabled()) {
                log.debug("Trigger: " + trigger);
            }
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            return ResponseUtil.notFound("Trigger " + triggerId + " doesn't exist for update");

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @PUT
    @Path("/groups/{groupId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update an existing group trigger definition and its member definitions.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Group Trigger updated."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class),
            @ApiResponse(code = 404, message = "Trigger doesn't exist.", response = ApiError.class)
    })
    public Response updateGroupTrigger(
            @ApiParam(value = "Group Trigger id to be updated.", required = true)
            @PathParam("groupId")
            final String groupId,
            @ApiParam(value = "Updated group trigger definition.", name = "groupTrigger", required = true)
            final Trigger groupTrigger) {
        try {
            if (groupTrigger != null && !isEmpty(groupId)) {
                groupTrigger.setId(groupId);
            }
            definitions.updateGroupTrigger(tenantId, groupTrigger);
            if (log.isDebugEnabled()) {
                log.debug("Trigger: " + groupTrigger);
            }
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            return ResponseUtil.notFound("Trigger " + groupId + " doesn't exist for update");

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @POST
    @Path("/groups/members/{memberId}/orphan")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Make a non-orphan member trigger into an orphan.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger updated."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 404, message = "Trigger doesn't exist/Invalid Parameters.", response = ApiError.class)
    })
    public Response orphanMemberTrigger(
            @ApiParam(value = "Member Trigger id to be made an orphan.", required = true)
            @PathParam("memberId")
            final String memberId) {
        try {
            Trigger child = definitions.orphanMemberTrigger(tenantId, memberId);
            if (log.isDebugEnabled()) {
                log.debug("Orphan Member Trigger: " + child);
            }
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            return ResponseUtil.notFound("Member Trigger " + memberId + " doesn't exist for update");

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @POST
    @Path("/groups/members/{memberId}/unorphan")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Make a non-orphan member trigger into an orphan.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger updated."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class),
            @ApiResponse(code = 404, message = "Trigger doesn't exist.", response = ApiError.class)
    })
    public Response unorphanMemberTrigger(
            @ApiParam(value = "Member Trigger id to be made an orphan.", required = true)
            @PathParam("memberId")
            final String memberId,
            @ApiParam(required = true, name = "memberTrigger",
                    value = "Only context and dataIdMap are used when changing back to a non-orphan.")
            final UnorphanMemberInfo unorphanMemberInfo) {
        try {
            if (null == unorphanMemberInfo) {
                return ResponseUtil.badRequest("MemberTrigger is null");
            }
            Trigger child = definitions.unorphanMemberTrigger(tenantId, memberId,
                    unorphanMemberInfo.getMemberContext(),
                    unorphanMemberInfo.getMemberTags(),
                    unorphanMemberInfo.getDataIdMap());
            if (log.isDebugEnabled()) {
                log.debug("Member Trigger: " + child);
            }
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            return ResponseUtil.notFound("Member Trigger " + memberId + " doesn't exist for update");

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @DELETE
    @Path("/{triggerId}")
    @ApiOperation(value = "Delete an existing standard or group member trigger definition." +
            " This can not be used to delete a group trigger definition.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Trigger deleted."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 404, message = "Trigger not found.", response = ApiError.class)
    })
    public Response deleteTrigger(
            @ApiParam(value = "Trigger definition id to be deleted.", required = true) @PathParam("triggerId")
            final String triggerId) {
        try {
            definitions.removeTrigger(tenantId, triggerId);
            if (log.isDebugEnabled()) {
                log.debug("TriggerId: " + triggerId);
            }
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            return ResponseUtil.notFound("Trigger " + triggerId + " doesn't exist for delete");

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @DELETE
    @Path("/groups/{groupId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Delete a group trigger.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Group Trigger Removed."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 404, message = "Group Trigger not found.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class)
    })
    public Response deleteGroupTrigger(
            @ApiParam(required = true, value = "Group Trigger id.")
            @PathParam("groupId")
            final String groupId,
            @ApiParam(required = true, value = "Convert the non-orphan member triggers to standard triggers.")
            @QueryParam("keepNonOrphans")
            final boolean keepNonOrphans,
            @ApiParam(required = true, value = "Convert the orphan member triggers to standard triggers.")
            @QueryParam("keepOrphans")
            final boolean keepOrphans) {
        try {
            definitions.removeGroupTrigger(tenantId, groupId, keepNonOrphans, keepOrphans);
            if (log.isDebugEnabled()) {
                log.debug("Remove Group Trigger: " + tenantId + "/" + groupId);
            }
            return ResponseUtil.ok();

        } catch (NotFoundException e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.notFound(e.getMessage());
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/{triggerId}/dampenings")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get all Dampenings for a Trigger (1 Dampening per mode).",
            response = Dampening.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully fetched list of dampenings."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    public Response getTriggerDampenings(
            @ApiParam(value = "Trigger definition id to be retrieved.", required = true)
            @PathParam("triggerId")
            final String triggerId) {
        try {
            Collection<Dampening> dampenings = definitions.getTriggerDampenings(tenantId, triggerId, null);
            log.debug("Dampenings: " + dampenings);
            return ResponseUtil.ok(dampenings);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/{triggerId}/dampenings/mode/{triggerMode}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get dampening using triggerId and triggerMode.",
            response = Dampening.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully fetched list of dampenings."),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    public Response getTriggerModeDampenings(
            @ApiParam(value = "Trigger definition id to be retrieved.", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Trigger mode", required = true)
            @PathParam("triggerMode")
            final Mode triggerMode) {
        try {
            Collection<Dampening> dampenings = definitions.getTriggerDampenings(tenantId, triggerId,
                    triggerMode);
            if (log.isDebugEnabled()) {
                log.debug("Dampenings: " + dampenings);
            }
            return ResponseUtil.ok(dampenings);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/{triggerId}/dampenings/{dampeningId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get an existing dampening.",
            response = Dampening.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening Found."),
            @ApiResponse(code = 404, message = "No Dampening Found.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    public Response getDampening(
            @ApiParam(value = "Trigger definition id to be retrieved.", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Dampening id", required = true)
            @PathParam("dampeningId")
            final String dampeningId) {
        try {
            Dampening found = definitions.getDampening(tenantId, dampeningId);
            log.debug("Dampening: " + found);
            if (found == null) {
                return ResponseUtil.notFound("No dampening found for triggerId: " + triggerId + " and dampeningId:" +
                        dampeningId);
            }
            return ResponseUtil.ok(found);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @POST
    @Path("/{triggerId}/dampenings")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new dampening.",
            notes = "Return Dampening created.",
            response = Dampening.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening created."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class)
    })
    public Response createDampening(
            @ApiParam(value = "Trigger definition id attached to dampening.", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Dampening definition to be created.", required = true)
            final Dampening dampening) {
        try {
            dampening.setTenantId(tenantId);
            dampening.setTriggerId(triggerId);
            boolean exists = (definitions.getDampening(tenantId, dampening.getDampeningId()) != null);
            if (!exists) {
                // make sure we have the best chance of clean data..
                Dampening d = getCleanDampening(dampening);
                definitions.addDampening(tenantId, d);
                if (log.isDebugEnabled()) {
                    log.debug("Dampening: " + d);
                }
                return ResponseUtil.ok(d);
            } else {
                return ResponseUtil.badRequest("Existing dampening for dampeningId: " + dampening.getDampeningId());
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @POST
    @Path("/groups/{groupId}/dampenings")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Create a new group dampening.",
            notes = " Return group Dampening created.",
            response = Dampening.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening created."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters", response = ApiError.class)
    })
    public Response createGroupDampening(
            @ApiParam(value = "Group Trigger definition id attached to dampening.", required = true)
            @PathParam("groupId")
            final String groupId,
            @ApiParam(value = "Dampening definition to be created.", required = true)
            final Dampening dampening) {
        try {
            dampening.setTriggerId(groupId);
            boolean exists = (definitions.getDampening(tenantId, dampening.getDampeningId()) != null);
            if (!exists) {
                // make sure we have the best chance of clean data..
                Dampening d = getCleanDampening(dampening);
                definitions.addGroupDampening(tenantId, d);
                if (log.isDebugEnabled()) {
                    log.debug("Dampening: " + d);
                }
                return ResponseUtil.ok(d);
            } else {
                return ResponseUtil.badRequest("Existing dampening for dampeningId: " + dampening.getDampeningId());
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    private Dampening getCleanDampening(Dampening dampening) throws Exception {
        switch (dampening.getType()) {
            case STRICT:
                return Dampening.forStrict(dampening.getTenantId(), dampening.getTriggerId(),
                        dampening.getTriggerMode(),
                        dampening.getEvalTrueSetting());

            case STRICT_TIME:
                return Dampening.forStrictTime(dampening.getTenantId(), dampening.getTriggerId(),
                        dampening.getTriggerMode(),
                        dampening.getEvalTimeSetting());

            case STRICT_TIMEOUT:
                return Dampening.forStrictTimeout(dampening.getTenantId(), dampening.getTriggerId(),
                        dampening.getTriggerMode(),
                        dampening.getEvalTimeSetting());
            case RELAXED_COUNT:
                return Dampening.forRelaxedCount(dampening.getTenantId(), dampening.getTriggerId(),
                        dampening.getTriggerMode(),
                        dampening.getEvalTrueSetting(),
                        dampening.getEvalTotalSetting());
            case RELAXED_TIME:
                return Dampening.forRelaxedTime(dampening.getTenantId(), dampening.getTriggerId(),
                        dampening.getTriggerMode(),
                        dampening.getEvalTrueSetting(), dampening.getEvalTimeSetting());

            default:
                throw new Exception("Unhandled Dampening Type: " + dampening.toString());
        }
    }

    @PUT
    @Path("/{triggerId}/dampenings/{dampeningId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update an existing dampening definition.",
            notes = "Note that the trigger mode can not be changed. Return Dampening updated.",
            response = Dampening.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening Updated."),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class),
            @ApiResponse(code = 404, message = "No Dampening Found.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error", response = ApiError.class)
    })
    public Response updateDampening(
            @ApiParam(value = "Trigger definition id to be retrieved.", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Dampening id.", required = true)
            @PathParam("dampeningId")
            final String dampeningId,
            @ApiParam(value = "Updated dampening definition", required = true)
            final Dampening dampening) {
        try {
            boolean exists = (definitions.getDampening(tenantId, dampeningId) != null);
            if (exists) {
                // make sure we have the best chance of clean data..
                Dampening d = getCleanDampening(dampening);
                definitions.updateDampening(tenantId, d);
                if (log.isDebugEnabled()) {
                    log.debug("Dampening: " + d);
                }
                return ResponseUtil.ok(d);
            } else {
                return ResponseUtil.notFound("No dampening found for dampeningId: " + dampeningId);
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @PUT
    @Path("/groups/{groupId}/dampenings/{dampeningId}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(value = "Update an existing group dampening definition.",
            notes = "Note that the trigger mode can not be changed. Return Dampening updated.",
            response = Dampening.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening Updated."),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters.", response = ApiError.class),
            @ApiResponse(code = 404, message = "No Dampening Found.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error", response = ApiError.class)
    })
    public Response updateGroupDampening(
            @ApiParam(value = "Trigger definition id to be retrieved.", required = true)
            @PathParam("groupId")
            final String groupId,
            @ApiParam(value = "Dampening id.", required = true)
            @PathParam("dampeningId")
            final String dampeningId,
            @ApiParam(value = "Updated dampening definition.", required = true)
            final Dampening dampening) {
        try {
            boolean exists = (definitions.getDampening(tenantId, dampeningId) != null);
            if (exists) {
                // make sure we have the best chance of clean data..
                Dampening d = getCleanDampening(dampening);
                definitions.updateGroupDampening(tenantId, d);
                if (log.isDebugEnabled()) {
                    log.debug("Group Dampening: " + d);
                }
                return ResponseUtil.ok(d);
            } else {
                return ResponseUtil.notFound("No dampening found for dampeningId: " + dampeningId);
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @DELETE
    @Path("/{triggerId}/dampenings/{dampeningId}")
    @ApiOperation(value = "Delete an existing dampening definition.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening deleted."),
            @ApiResponse(code = 404, message = "No Dampening found.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error", response = ApiError.class)
    })
    public Response deleteDampening(
            @ApiParam(value = "Trigger definition id to be deleted.", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "Dampening id for dampening definition to be deleted.", required = true)
            @PathParam("dampeningId")
            final String dampeningId) {
        try {
            boolean exists = (definitions.getDampening(tenantId, dampeningId) != null);
            if (exists) {
                definitions.removeDampening(tenantId, dampeningId);
                log.debug("DampeningId: " + dampeningId);
                return ResponseUtil.ok();
            } else {
                return ResponseUtil.notFound("Dampening not found for dampeningId: " + dampeningId);
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @DELETE
    @Path("/groups/{groupId}/dampenings/{dampeningId}")
    @ApiOperation(value = "Delete an existing group dampening definition.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Dampening deleted."),
            @ApiResponse(code = 404, message = "No Dampening found.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    public Response deleteGroupDampening(
            @ApiParam(value = "Trigger definition id to be retrieved.", required = true)
            @PathParam("groupId")
            final String groupId,
            @ApiParam(value = "Dampening id for dampening definition to be deleted.", required = true)
            @PathParam("dampeningId")
            final String dampeningId) {
        try {
            boolean exists = (definitions.getDampening(tenantId, dampeningId) != null);
            if (exists) {
                definitions.removeGroupDampening(tenantId, dampeningId);
                if (log.isDebugEnabled()) {
                    log.debug("Group DampeningId: " + dampeningId);
                }
                return ResponseUtil.ok();
            } else {
                return ResponseUtil.notFound("Dampening not found for dampeningId: " + dampeningId);
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/{triggerId}/conditions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get all conditions for a specific trigger.",
            response = Condition.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully fetched list of conditions."),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    public Response getTriggerConditions(
            @ApiParam(value = "Trigger definition id to be retrieved.", required = true)
            @PathParam("triggerId")
            final String triggerId) {
        try {
            Collection<Condition> conditions = definitions.getTriggerConditions(tenantId, triggerId, null);
            log.debug("Conditions: " + conditions);
            return ResponseUtil.ok(conditions);
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return ResponseUtil.internalError(e);
        }
    }

    @PUT
    @Path("/{triggerId}/conditions/{triggerMode}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Set the conditions for the trigger. ",
            notes = "This replaces any existing conditions. Returns the new conditions.",
            response = Condition.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Condition Set created."),
            @ApiResponse(code = 404, message = "No trigger found.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error.", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters", response = ApiError.class)
    })
    public Response setConditions(
            @ApiParam(value = "The relevant Trigger.", required = true)
            @PathParam("triggerId")
            final String triggerId,
            @ApiParam(value = "FIRING or AUTORESOLVE (not case sensitive).", required = true)
            @PathParam("triggerMode")
            final String triggerMode,
            @ApiParam(value = "Collection of Conditions to set.", required = true)
            final Collection<Condition> conditions) {
        try {
            Mode mode = Mode.valueOf(triggerMode.toUpperCase());
            if (!isEmpty(conditions)) {
                for (Condition condition : conditions) {
                    condition.setTriggerId(triggerId);
                    condition.setTriggerMode(mode);
                }
            }
            Collection<Condition> updatedConditions = definitions.setConditions(tenantId, triggerId, mode, conditions);
            if (log.isDebugEnabled()) {
                log.debug("Conditions: " + updatedConditions);
            }
            return ResponseUtil.ok(updatedConditions);

        } catch (NotFoundException e) {
            return ResponseUtil.notFound(e.getMessage());
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    @PUT
    @Path("/groups/{groupId}/conditions/{triggerMode}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Set the conditions for the group trigger.",
            notes = "This replaces any existing conditions on the group and member conditions. " +
                    "Return the new group conditions.",
            response = Condition.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Group Condition Set created."),
            @ApiResponse(code = 404, message = "No trigger found.", response = ApiError.class),
            @ApiResponse(code = 500, message = "Internal server error", response = ApiError.class),
            @ApiResponse(code = 400, message = "Bad Request/Invalid Parameters", response = ApiError.class)
    })
    public Response setGroupConditions(
            @ApiParam(value = "The relevant Group Trigger.", required = true)
            @PathParam("groupId")
            final String groupId,
            @ApiParam(value = "FIRING or AUTORESOLVE (not case sensitive).", required = true)
            @PathParam("triggerMode")
            final String triggerMode,
            @ApiParam(value = "Collection of Conditions to set and Map with tokens per dataId on members.")
            final GroupConditionsInfo groupConditionsInfo) {
        try {
            Mode mode = Mode.valueOf(triggerMode.toUpperCase());
            for (Condition condition : groupConditionsInfo.getConditions()) {
                if (condition == null) {
                    return ResponseUtil.badRequest("GroupConditionsInfo must have non null conditions: " +
                            groupConditionsInfo);
                }
                condition.setTriggerId(groupId);
                condition.setTriggerMode(mode);
            }

            Collection<Condition> conditions = definitions.setGroupConditions(tenantId, groupId, mode,
                    groupConditionsInfo.getConditions(),
                    groupConditionsInfo.getDataIdMemberMap());

            if (log.isDebugEnabled()) {
                log.debug("Conditions: " + conditions);
            }
            return ResponseUtil.ok(conditions);

        } catch (IllegalArgumentException e) {
            return ResponseUtil.badRequest("Bad trigger mode: " + triggerMode);
        } catch (NotFoundException e) {
            return ResponseUtil.notFound(e.getMessage());
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                return ResponseUtil.badRequest("Bad arguments: " + e.getMessage());
            }
            return ResponseUtil.internalError(e);
        }
    }

    private boolean isEmpty(String s) {
        return null == s || s.trim().isEmpty();
    }

    private boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

}
