package com.filomar.interpreter;

abstract class Stmt {
	interface Visitor<R> {
		R visitExpressionStmt(Expression stmt);
		R visitPrintStmt(Print stmt);
	}

	abstract <R> R accept(Visitor<R> visitor);

	static class Expression extends Stmt {
		final Expr expr;

		Expression(Expr expr) {
			this.expr = expr;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}
	}

	static class Print extends Stmt {
		final Expr value;

		Print(Expr expr) {
			this.value = expr;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}
	}

}
