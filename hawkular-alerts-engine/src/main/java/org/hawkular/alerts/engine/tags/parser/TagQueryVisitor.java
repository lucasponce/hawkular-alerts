// Generated from org/hawkular/alerts/engine/tags/parser/TagQuery.g4 by ANTLR 4.6
package org.hawkular.alerts.engine.tags.parser;

/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TagQueryParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TagQueryVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TagQueryParser#tagquery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTagquery(TagQueryParser.TagqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link TagQueryParser#object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject(TagQueryParser.ObjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link TagQueryParser#tagexp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTagexp(TagQueryParser.TagexpContext ctx);
	/**
	 * Visit a parse tree produced by {@link TagQueryParser#logical_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogical_operator(TagQueryParser.Logical_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link TagQueryParser#boolean_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolean_operator(TagQueryParser.Boolean_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link TagQueryParser#array_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_operator(TagQueryParser.Array_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link TagQueryParser#array}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray(TagQueryParser.ArrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link TagQueryParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(TagQueryParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link TagQueryParser#key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey(TagQueryParser.KeyContext ctx);
}