package com.cpptojavasourceconverter;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;

import com.cpptojavasourceconverter.models.ExpressionModels.*;
import com.cpptojavasourceconverter.TypeManager.TypeEnum;
import com.cpptojavasourceconverter.TypeManager.TypeType;

class InitializationManager
{
	private final TranslationUnitContext ctx;
	
	InitializationManager(TranslationUnitContext con)
	{
		ctx = con;
	}
	
	enum InitType
	{
		RAW,
		WRAPPED;
	}
	
	MExpression eval1Init(IASTInitializer initializer, IType typeRequired, IASTName name, InitType initType) throws DOMException 
	{
		if (initializer == null)
		{
			if (TypeManager.isBasicType(typeRequired) &&
				(name == null || !ctx.bitfieldMngr.isBitfield(name)))
			{
				return ctx.exprEvaluator.makeSimpleCreationExpression(typeRequired);
			}
			else if (TypeManager.isOneOf(typeRequired, TypeEnum.BASIC_ARRAY, TypeEnum.BASIC_POINTER, TypeEnum.OBJECT_POINTER))
			{
				return ctx.exprEvaluator.makeSimpleCreationExpression(typeRequired);
			}
			else if (TypeManager.isOneOf(typeRequired, TypeEnum.OBJECT))
			{
				// new FooBar();
				MNewExpressionObject expr = new MNewExpressionObject();

				expr.type = ctx.typeMngr.cppToJavaType(typeRequired, TypeType.IMPLEMENTATION);

				if (!(name.resolveBinding() instanceof IField))
				{
					// A plain object must be added to the stack
					// so we can call its destructor.
					return ctx.stackMngr.createAddItemCall(expr);
				}
				else
				{
					// Fields will be destroyed in the dtor so are
					// not added to the stack.
					return expr;
				}
			}
			else if (TypeManager.isOneOf(typeRequired, TypeEnum.OBJECT_ARRAY))
			{
				// TODO
				return null;
			}
			else
			{
				return null;
			}
		}
		else if (initializer instanceof IASTEqualsInitializer)
		{
			if (ctx.bitfieldMngr.isBitfield(name))
			{
				return ctx.exprEvaluator.eval1Expr((IASTExpression) ((IASTEqualsInitializer) initializer).getInitializerClause());
			}
			else if (TypeManager.isOneOf(typeRequired, TypeEnum.BASIC_POINTER, TypeEnum.OBJECT_POINTER))
			{
				MExpression expr = ctx.exprEvaluator.eval1Expr((IASTExpression) ((IASTEqualsInitializer) initializer).getInitializerClause());
				ctx.exprEvaluator.modifyLiteralToPtr(expr);
				return expr;
			}
			else if (TypeManager.isOneOf(typeRequired, TypeEnum.OBJECT))
			{
				// TODO: Is this needed?
				MExpression expr = ctx.exprEvaluator.eval1Expr((IASTExpression) ((IASTEqualsInitializer) initializer).getInitializerClause());				
				// A plain object must be added to the stack so we can call its destructor.
				return ctx.stackMngr.createAddItemCall(expr);
			}
			else
			{
				if (initType == InitType.WRAPPED)
					return ctx.exprEvaluator.wrapIfNeeded((IASTExpression) ((IASTEqualsInitializer) initializer).getInitializerClause(), typeRequired);
				else
					return ctx.exprEvaluator.eval1Expr((IASTExpression) ((IASTEqualsInitializer) initializer).getInitializerClause());
			}
		}
		else if (initializer instanceof ICPPASTConstructorInitializer)
		{
			ICPPASTConstructorInitializer inti = (ICPPASTConstructorInitializer) initializer;

			MMultiExpression multi = new MMultiExpression();
			
			for (IASTInitializerClause cls : inti.getArguments())
			{
				IASTExpression expr = (IASTExpression) cls;
				MExpression create;

				if (name != null && ctx.bitfieldMngr.isBitfield(name))
				{
					create = ctx.exprEvaluator.eval1Expr(expr);
				}
				else if (TypeManager.isOneOf(typeRequired, TypeEnum.BASIC_POINTER, TypeEnum.OBJECT_POINTER))
				{
					create = ctx.exprEvaluator.eval1Expr(expr);
					ctx.exprEvaluator.modifyLiteralToPtr(create);
				}
				else
				{
					create = ctx.exprEvaluator.wrapIfNeeded(expr, typeRequired);
				}
				multi.exprs.add(create);
			}

			return multi;
		}
		
		return null;
	}
}
