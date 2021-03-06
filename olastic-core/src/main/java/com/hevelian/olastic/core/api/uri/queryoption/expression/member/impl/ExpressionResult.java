package com.hevelian.olastic.core.api.uri.queryoption.expression.member.impl;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import org.apache.olingo.server.api.ODataApplicationException;
import org.elasticsearch.index.query.QueryBuilder;

import com.hevelian.olastic.core.api.uri.queryoption.expression.member.ExpressionMember;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the result of expression.
 * 
 * @author Taras Kohut
 */
@AllArgsConstructor
@Getter
public class ExpressionResult extends BaseMember {

    private final QueryBuilder queryBuilder;

    @Override
    public ExpressionResult and(ExpressionMember expressionMember)
            throws ODataApplicationException {
        return new ExpressionResult(boolQuery().must(queryBuilder)
                .must(((ExpressionResult) expressionMember).getQueryBuilder()));
    }

    @Override
    public ExpressionResult or(ExpressionMember expressionMember) throws ODataApplicationException {
        return new ExpressionResult(boolQuery().should(queryBuilder)
                .should(((ExpressionResult) expressionMember).getQueryBuilder()));
    }

    @Override
    public ExpressionResult not() throws ODataApplicationException {
        return new ExpressionResult(boolQuery().mustNot(queryBuilder));
    }
}
