package com.filomar.interpreter;

import java.util.List;

abstract class Stmt {
	interface Visitor<R> {
		R visitVarDclStmt(VarDcl stmt);
		R visitBranchingStmt(Branching stmt);
		R visitPrintStmt(Print stmt);
		R visitBlockStmt(Block stmt);
		R visitExpressionStmt(Expression stmt);
	}

	abstract <R> R accept(Visitor<R> visitor);

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

	static class Branching extends Stmt {
		final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch;

		Branching(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBranchingStmt(this);
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

}
