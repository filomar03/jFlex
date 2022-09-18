package com.filomar.interpreter;

import java.util.List;

abstract class Stmt {
	interface Visitor<R> {
		R visitFunDclStmt(FunDcl stmt);
		R visitVarDclStmt(VarDcl stmt);
		R visitBlockStmt(Block stmt);
		R visitBreakStmt(Break stmt);
		R visitIfStmt(If stmt);
		R visitPrintStmt(Print stmt);
		R visitReturnStmt(Return stmt);
		R visitWhileStmt(While stmt);
		R visitExpressionStmt(Expression stmt);
	}

	abstract <R> R accept(Visitor<R> visitor);

	static class FunDcl extends Stmt {
		final Token identifier;
		final List<Token> parameters;
		final List<Stmt> body;

		FunDcl(Token identifier, List<Token> parameters, List<Stmt> body) {
			this.identifier = identifier;
			this.parameters = parameters;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunDclStmt(this);
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

	static class Break extends Stmt {

		Break() {
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBreakStmt(this);
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

	static class Return extends Stmt {
		final Expr value;

		Return(Expr value) {
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStmt(this);
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
	}

}
