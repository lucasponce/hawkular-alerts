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
package org.hawkular.alerts.api.model.condition;

import org.hawkular.alerts.api.doc.DocModel;
import org.hawkular.alerts.api.doc.DocModelProperty;
import org.hawkular.alerts.api.model.trigger.Mode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A string comparison condition.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@DocModel(description = "A string comparison condition.")
public class StringCondition extends Condition {

    private static final long serialVersionUID = 1L;

    public enum Operator {
        EQUAL, NOT_EQUAL, STARTS_WITH, ENDS_WITH, CONTAINS, MATCH
    }

    @JsonInclude(Include.NON_NULL)
    private String dataId;

    @DocModelProperty(description = "String operator.",
            position = 0,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private Operator operator;

    @DocModelProperty(description = "Pattern to be used with the string operator.",
            position = 1,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private String pattern;

    @DocModelProperty(description = "Flag to indicate whether pattern should ignore case in the string operator expression.",
            position = 2,
            required = false,
            defaultValue = "false")
    @JsonInclude
    private boolean ignoreCase;

    public StringCondition() {
        /*
            Default constructor is needed for JSON libraries in JAX-RS context.
         */
        this("", "", 1, 1, null, null, null, false);
    }

    public StringCondition(String tenantId, String triggerId,
            String dataId, Operator operator, String pattern, boolean ignoreCase) {
        this(tenantId, triggerId, Mode.FIRING, 1, 1, dataId, operator, pattern, ignoreCase);
    }

    /**
     * This constructor requires the tenantId be assigned prior to persistence. It can be used when
     * creating triggers via Rest, as the tenant will be assigned automatically.
     */
    public StringCondition(String triggerId, Mode triggerMode,
            String dataId, Operator operator, String pattern, boolean ignoreCase) {
        this("", triggerId, triggerMode, 1, 1, dataId, operator, pattern, ignoreCase);
    }

    public StringCondition(String tenantId, String triggerId, Mode triggerMode,
            String dataId, Operator operator, String pattern, boolean ignoreCase) {
        this(tenantId, triggerId, triggerMode, 1, 1, dataId, operator, pattern, ignoreCase);
    }

    public StringCondition(String tenantId, String triggerId, int conditionSetSize, int conditionSetIndex,
            String dataId, Operator operator, String pattern, boolean ignoreCase) {
        this(tenantId, triggerId, Mode.FIRING, conditionSetSize, conditionSetIndex, dataId, operator, pattern,
                ignoreCase);
    }

    /**
     * This constructor requires the tenantId be assigned prior to persistence. It can be used when
     * creating triggers via Rest, as the tenant will be assigned automatically.
     */
    public StringCondition(String triggerId, Mode triggerMode, int conditionSetSize,
            int conditionSetIndex, String dataId, Operator operator, String pattern,
            boolean ignoreCase) {
        this("", triggerId, triggerMode, conditionSetSize, conditionSetIndex, dataId, operator, pattern,
                ignoreCase);
    }

    public StringCondition(String tenantId, String triggerId, Mode triggerMode, int conditionSetSize,
            int conditionSetIndex, String dataId, Operator operator, String pattern,
            boolean ignoreCase) {
        super(tenantId, triggerId, triggerMode, conditionSetSize, conditionSetIndex, Type.STRING);
        this.dataId = dataId;
        this.operator = operator;
        this.pattern = pattern;
        this.ignoreCase = ignoreCase;
        updateDisplayString();
    }

    public StringCondition(StringCondition condition) {
        super(condition);

        this.dataId = condition.getDataId();
        this.ignoreCase = condition.isIgnoreCase();
        this.operator = condition.getOperator();
        this.pattern = condition.getPattern();
    }

    @Override
    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean match(String value) {

        if (ignoreCase && operator != Operator.MATCH) {
            pattern = pattern.toLowerCase();
            value = value.toLowerCase();
        }
        switch (operator) {
            case EQUAL:
                return value.equals(pattern);
            case NOT_EQUAL:
                return !value.equals(pattern);
            case ENDS_WITH:
                return value.endsWith(pattern);
            case STARTS_WITH:
                return value.startsWith(pattern);
            case CONTAINS:
                return value.contains(pattern);
            case MATCH:
                return value.matches(ignoreCase ? ("(?i)" + pattern) : pattern);
            default:
                throw new IllegalStateException("Unknown operator: " + operator.name());
        }
    }

    @Override
    public void updateDisplayString() {
        String operator = null == this.operator ? null : this.operator.name();
        String s = String.format("%s %s [%s]%s", this.dataId, operator, this.pattern,
                (this.ignoreCase ? " Ignoring Case" : ""));
        setDisplayString(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        StringCondition that = (StringCondition) o;

        if (ignoreCase != that.ignoreCase)
            return false;
        if (dataId != null ? !dataId.equals(that.dataId) : that.dataId != null)
            return false;
        if (operator != that.operator)
            return false;
        if (pattern != null ? !pattern.equals(that.pattern) : that.pattern != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dataId != null ? dataId.hashCode() : 0);
        result = 31 * result + (operator != null ? operator.hashCode() : 0);
        result = 31 * result + (pattern != null ? pattern.hashCode() : 0);
        result = 31 * result + (ignoreCase ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StringCondition [triggerId='" + triggerId + "', " +
                "triggerMode=" + triggerMode + ", " +
                "dataId=" + (dataId == null ? null : '\'' + dataId + '\'') + ", " +
                "operator=" + (operator == null ? null : '\'' + operator.toString() + '\'') + ", " +
                "pattern=" + (pattern == null ? null : '\'' + pattern + '\'') + ", " +
                "ignoreCase=" + ignoreCase + "]";
    }

}
