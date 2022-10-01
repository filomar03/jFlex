package com.filomar.interpreter;

import java.util.List;

abstract class Stmt {
	interface Visitor<R> {
		R visitFunctionDclStmt(FunctionDcl stmt);
		R visitVariableDclStmt(VariableDcl stmt);
		R visitBlockStmt(Block stmt);
		R visitBreakStmt(Break stmt);
		R visitIfStmt(If stmt);
		R visitPrintStmt(Print stmt);
		R visitReturnStmt(Return stmt);
		R visitWhileStmt(While stmt);
		R visitExpressionStmt(Expression stmt);
	}

	abstract <R> R accept(Visitor<R> visitor);

	static class FunctionDcl extends Stmt {
		final Token identifier;
		final Expr.Function function;

		FunctionDcl(Token identifier, Expr.Function function) {
			this.identifier = identifier;
			this.function = function;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionDclStmt(this);
		}

		@Override
		public String toString() {
			return Flex.debugAstPrinter().stringify(this);
		}
	}

	static class VariableDcl extends Stmt {
		final Token identifier;
		final Expr initializer;

		VariableDcl(Token identifier, Expr initializer) {
			this.identifier = identifier;
			this.initializer = initializer;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVariableDclStmt(this);
		}

		@Override
		public String toString() {
			return Flex.debugAstPrinter().stringify(this);
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

		@Override
		public String toString() {
			return Flex.debugAstPrinter().stringify(this);
		}
	}

	static class Break extends Stmt {

		Break() {
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBreakStmt(this);
		}

		@Override
		public String toString() {
			return Flex.debugAstPrinter().stringify(this);
		}
	}

	static class If extends Stmt {
		final Expr condition;
		final Stmt thenBranch;
		final Stmt elseBranch;

		If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}

		@Override
		public String toString() {
			return Flex.debugAstPrinter().stringify(this);
		}
	}

	static class Print extends Stmt {
		final Expr expression;

		Print(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}

		@Override
		public String toString() {
			return Flex.debugAstPrinter().stringify(this);
		}
	}

	static class Return extends Stmt {
		final Token keyword;
		final Expr expression;

		Return(Token keyword, Expr expression) {
			this.keyword = keyword;
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStmt(this);
		}

		@Override
		public String toString() {
			return Flex.debugAstPrinter().stringify(this);
		}
	}

	static class While extends Stmt {
		final Expr condition;
		final Stmt body;

		While(Expr condition, Stmt body) {
			this.condition = condition;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}

		@Override
		public String toString() {
			return Flex.debugAstPrinter().stringify(this);
		}
	}

	static class Expression extends Stmt {
		final Expr expression;

		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}

		@Override
		public String toString() {
			return Flex.debugAstPrinter().stringify(this);
		}
	}

}
