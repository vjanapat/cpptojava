package com.cpptojavasourceconverter;

import java.util.Arrays;

import com.cpptojavasourceconverter.models.ExpressionModels.MExpression;
import com.cpptojavasourceconverter.models.ExpressionModels.MFieldReferenceExpression;
import com.cpptojavasourceconverter.models.ExpressionModels.MFieldReferenceExpressionPlain;
import com.cpptojavasourceconverter.models.ExpressionModels.MFunctionCallExpression;
import com.cpptojavasourceconverter.models.ExpressionModels.MIdentityExpression;
import com.cpptojavasourceconverter.models.ExpressionModels.MIdentityExpressionNumber;
import com.cpptojavasourceconverter.models.ExpressionModels.MIdentityExpressionPlain;
import com.cpptojavasourceconverter.models.ExpressionModels.MInfixExpression;
import com.cpptojavasourceconverter.models.ExpressionModels.MInfixExpressionPlain;
import com.cpptojavasourceconverter.models.ExpressionModels.MInfixExpressionPtrComparison;
import com.cpptojavasourceconverter.models.ExpressionModels.MLiteralExpression;
import com.cpptojavasourceconverter.models.StmtModels.MExprStmt;
import com.cpptojavasourceconverter.models.StmtModels.MStmt;

public class ModelCreation
{
	/**
	 * Results in obj1.obj2.method(args) 
	 */
	static MStmt createMethodCall(TranslationUnitContext ctx, String obj1, String obj2, String method, MExpression... args)
	{
		MFieldReferenceExpression fr1 = createFieldReference(obj1, obj2);
		MFieldReferenceExpression fr2 = createFieldReference(fr1, method);
		MFunctionCallExpression fcall = new MFunctionCallExpression();

		fcall.name = fr2;
		fcall.args = Arrays.asList(args);
	
		return 	createExprStmt(ctx, fcall);
	}
	
	/**
	 * Results in literal as expression.
	 */
	static MLiteralExpression createLiteral(String literal)
	{
		MLiteralExpression lit1 = new MLiteralExpression();		
		lit1.literal = literal;
		return lit1;
	}
	
	static MIdentityExpression createId(String name)
	{
		MIdentityExpressionPlain ident = new MIdentityExpressionPlain();
		ident.ident = name;
		return ident;
	}

	static MIdentityExpression createNumberId(String name)
	{
		MIdentityExpressionNumber ident = new MIdentityExpressionNumber();
		ident.ident = name;
		return ident;
	}

	
	/**
	 * Results in obj.field as expression.
	 */
	static MFieldReferenceExpression createFieldReference(String obj, String field)
	{
		MLiteralExpression lit1 = createLiteral(obj);
		return createFieldReference(lit1, field);
	}
	
	/**
	 * Results in obj.field as expression.  
	 */
	static MFieldReferenceExpression createFieldReference(MExpression obj, String field)
	{
		MFieldReferenceExpression fr = new MFieldReferenceExpressionPlain();
		fr.object = obj;
		fr.field = field;
		return fr;
	}
	
	/**
	 * Results in expr as statement.
	 */
	static MExprStmt createExprStmt(TranslationUnitContext ctx, MExpression expr)
	{
		MExprStmt es = ctx.stmtModels.new MExprStmt();
		es.expr = expr;
		return es;
	}

	/**
	 * Results in obj.method(args) as statement.
	 */
	static MStmt createMethodCall(TranslationUnitContext ctx, String obj, String method, MExpression... args)
	{
		MFieldReferenceExpression fr = createFieldReference(obj, method);
		MFunctionCallExpression fcall = new MFunctionCallExpression();
		
		fcall.name = fr;
		fcall.args = Arrays.asList(args);
		
		return createExprStmt(ctx, fcall);
	}

	/**
	 * Results in l op r as expression.
	 */
	static MExpression createInfixExpr(MExpression l, MExpression r, String op)
	{
		MInfixExpression infix = new MInfixExpressionPlain();
		infix.left = l;
		infix.right = r;
		infix.operator = op;
		return infix;
	}
	
	/**
	 * Results in left.lfield op right.rfield as expression.
	 */
	static MExpression createInfixExpr(String left, String lfield, String right, String rfield, String op)
	{
		MFieldReferenceExpression frl = createFieldReference(left, lfield);
		MFieldReferenceExpression frr = createFieldReference(right, rfield);
		return createInfixExpr(frl, frr, op);
	}
	
	static MExpression createInfixExprPtrComparison(MExpression l, MExpression r, String op)
	{
		MInfixExpressionPtrComparison infix = new MInfixExpressionPtrComparison();
		infix.left = l;
		infix.right = r;
		infix.operator = op;
		return infix;
	}

	/**
	 * Results in method(args) as expression.
	 */
	static MFunctionCallExpression createFuncCall(String method, MExpression...args)
	{
		MFunctionCallExpression expr = new MFunctionCallExpression();
		expr.name = createLiteral(method);
		expr.args.addAll(Arrays.asList(args));
		return expr;
	}
}
