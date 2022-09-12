package com.filomar.interpreter;

import java.util.List;

abstract class Stmt {
	interface Visitor<R> {
		R visitBlockStmt(Block stmt);
		R visitExpressionStmt(Expression stmt);
		R visitBranchingStmt(Branching stmt);
		R visitVarDclStmt(VarDcl stmt);
		R visitPrintStmt(Print stmt);
	}

	abstract <R> R accept(Visitor<R> visitor);

	static class Block extends Stmt {
		final List<Stmt> statements;

		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}
	}

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

	static class Branching extends Stmt {
		final Expr condition;
		final Expr then;
		final Expr else;

		Branching(Expr condition, Expr then, Expr else) {
			this.condition = condition;
			this.then = then;
			this.else = else;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBranchingStmt(this);
		}
	}

	static class VarDcl extends Stmt {
		final Token identifier;
		final Expr initializer;

		VarDcl(Token identifier, Expr initializer) {
			this.identifier = identifier;
			this.initializer = initializer;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarDclStmt(this);
		}
	}

	static class Print extends Stmt {
		final Expr value;

		Print(Expr value) {
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}
	}

}
