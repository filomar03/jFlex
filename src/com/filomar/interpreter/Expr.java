package com.filomar.interpreter;

import java.util.List;

abstract class Expr {
	interface Visitor<R> {
		R visitAssignExpr(Assign expr);
		R visitSetExpr(Set expr);
		R visitLogicalExpr(Logical expr);
		R visitBinaryExpr(Binary expr);
		R visitUnaryExpr(Unary expr);
		R visitCallExpr(Call expr);
		R visitGetExpr(Get expr);
		R visitFunctionExpr(Function expr);
		R visitLiteralExpr(Literal expr);
		R visitVariableExpr(Variable expr);
		R visitGroupingExpr(Grouping expr);
	}

	abstract <R> R accept(Visitor<R> visitor);

	static class Assign extends Expr {
		final Token target;
		final Expr expression;

		Assign(Token target, Expr expression) {
			this.target = target;
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitAssignExpr(this);
		}

	}

	static class Set extends Expr {
		final Expr instance;
		final Token property;
		final Expr value;

		Set(Expr instance, Token property, Expr value) {
			this.instance = instance;
			this.property = property;
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSetExpr(this);
		}

	}

	static class Logical extends Expr {
		final Expr left;
		final Token operator;
		final Expr right;

		Logical(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLogicalExpr(this);
		}

	}

	static class Binary extends Expr {
		final Expr left;
		final Token operator;
		final Expr right;

		Binary(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}

	}

	static class Unary extends Expr {
		final Token operator;
		final Expr expression;

		Unary(Token operator, Expr expression) {
			this.operator = operator;
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}

	}

	static class Call extends Expr {
		final Expr callee;
		final Token locationReference;
		final List<Expr> arguments;

		Call(Expr callee, Token locationReference, List<Expr> arguments) {
			this.callee = callee;
			this.locationReference = locationReference;
			this.arguments = arguments;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitCallExpr(this);
		}

	}

	static class Get extends Expr {
		final Expr instance;
		final Token property;

		Get(Expr instance, Token property) {
			this.instance = instance;
			this.property = property;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitGetExpr(this);
		}

	}

	static class Function extends Expr {
		final List<Token> parameters;
		final List<Stmt> body;

		Function(List<Token> parameters, List<Stmt> body) {
			this.parameters = parameters;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionExpr(this);
		}

	}

	static class Literal extends Expr {
		final Object value;

		Literal(Object value) {
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}

	}

	static class Variable extends Expr {
		final Token identifier;

		Variable(Token identifier) {
			this.identifier = identifier;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVariableExpr(this);
		}

	}

	static class Grouping extends Expr {
		final Expr expression;

		Grouping(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitGroupingExpr(this);
		}

	}

}
