package com.iorga.iraj.util;

import org.apache.commons.lang3.StringUtils;

import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.expr.ComparableExpressionBase;

public class QueryDSLUtils {
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<?>> OrderSpecifier<T> parseOrderSpecifier(final String orderByPath, final String orderByDirection, final Expression<?> baseExpression) {
		final String[] orderByPaths = orderByPath.split("\\.");
		Expression<?> listExpression = baseExpression;

		for (final String orderByPathElement : orderByPaths) {
			// get that public field part
			try {
				listExpression = (Expression<?>) listExpression.getClass().getField(orderByPathElement).get(listExpression);
			} catch (final Exception e) {
				throw new IllegalStateException("Cannot get " + listExpression + "." + orderByPathElement, e);
			}
		}

		if (StringUtils.equalsIgnoreCase(orderByDirection, "DESC")) {
			return ((ComparableExpressionBase<T>)listExpression).desc();
		} else {
			return ((ComparableExpressionBase<T>)listExpression).asc();
		}
	}
}
