package com.cpptojavasourceconverter;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.*;

import com.cpptojavasourceconverter.SourceConverter.CompositeInfo;
import com.cpptojavasourceconverter.SourceConverter.FieldInfo;
import com.cpptojavasourceconverter.TypeManager.TypeType;
import com.cpptojavasourceconverter.TypeManager.TypeEnum;
import com.cpptojavasourceconverter.models.DeclarationModels.*;
import com.cpptojavasourceconverter.models.ExpressionModels.*;
import com.cpptojavasourceconverter.models.StmtModels.*;

class SpecialGenerator
{
	private final TranslationUnitContext ctx;
	
	SpecialGenerator(TranslationUnitContext con)
	{
		ctx = con;
	}
	
	/**
	 * Given a list of fields for a class, adds initialization statements
	 * to the constructor for each field as required.
	 * Initializers provided to this function are generated from C++ initializer
	 * lists, and implicit initializers for objects.
	 * Note: We must initialize in order that fields were declared.
	 */
	void generateCtorStatements(List<FieldInfo> fields, MCompoundStmt method) throws DOMException
	{
		int start = 0;
		for (FieldInfo fieldInfo : fields)
		{
			MyLogger.log(fieldInfo.field.getName());

			// Static fields can't be initialized in the constructor.
			if (fieldInfo.init != null && !fieldInfo.isStatic)
			{
				if (ctx.bitfieldMngr.isBitfield(fieldInfo.declarator.getName()))
				{
					// this.set__name(right.get__name);
					MInfixAssignmentWithBitfieldOnLeft infix = new MInfixAssignmentWithBitfieldOnLeft();
					MFieldReferenceExpressionBitfield lbf = new MFieldReferenceExpressionBitfield();
					
					lbf.object = ModelCreation.createLiteral("this");
					lbf.field = fieldInfo.field.getName();
					
					infix.left = lbf;
					infix.right = fieldInfo.init;
					
					method.statements.add(start++, ModelCreation.createExprStmt(ctx, infix));
				}
				else
				{
					// Use 'this.field' construct as we may be shadowing a param name.
					MFieldReferenceExpression frl = ModelCreation.createFieldReference("this", fieldInfo.field.getName());
					MExpression expr = ModelCreation.createInfixExpr(frl, fieldInfo.init, "=");
				
					// Add assignment statements to start of generated method...
					method.statements.add(start++, ModelCreation.createExprStmt(ctx, expr));
				}
			}
		}
	}

	/**
	 * Generate destruct calls for fields in reverse order of field declaration.
	 */
	void generateDtorStatements(List<FieldInfo> fields, MCompoundStmt method, boolean hasSuper) throws DOMException
	{
		for (int i = fields.size() - 1; i >= 0; i--)
		{
			MyLogger.log(fields.get(i).field.getName());

			if (fields.get(i).isStatic)
			{
				/* Do nothing. */
			}
			else if (TypeManager.isOneOf(fields.get(i).field.getType(), TypeEnum.OBJECT))
			{
				// Call this.field.destruct()
				MStmt stmt = ModelCreation.createMethodCall(ctx, "this", fields.get(i).field.getName(), "destruct");
				method.statements.add(stmt);
			}
			else if (TypeManager.isOneOf(fields.get(i).field.getType(), TypeEnum.OBJECT_ARRAY))
			{
				// Call DestructHelper.destruct(this.field)
				MStmt stmt = ModelCreation.createMethodCall(
						ctx,
						"DestructHelper",
						"destruct",
						ModelCreation.createFieldReference("this", fields.get(i).field.getName()));
				
				method.statements.add(stmt);
			}
		}
		
		if (hasSuper)
		{
			// Call super.destruct()
			MStmt stmt = ModelCreation.createMethodCall(ctx, "super", "destruct");
			method.statements.add(stmt);
		}
	}
	
	CppFunction generateCopyCtor(CompositeInfo info, CppClass tyd, IASTDeclSpecifier declSpecifier) throws DOMException
	{
		CppFunction meth = ctx.declModels.new CppFunction();
		meth.retType = "";
		meth.name = tyd.name;
		meth.isCtor = true;
		
		MSimpleDecl var = ctx.declModels.new MSimpleDecl();
		var.type = tyd.name;
		var.name = "right";

		meth.args.add(var);
		meth.body = ctx.stmtModels.new MCompoundStmt();
		
		List<FieldInfo> fields = ctx.converter.collectFieldsForClass(declSpecifier);

		if (info.hasSuper)
		{
			// super(right);
			MStmt sup = ModelCreation.createExprStmt(
					ctx, ModelCreation.createFuncCall("super", ModelCreation.createLiteral("right")));
			
			meth.body.statements.add(sup);
		}

		for (FieldInfo fieldInfo : fields)
		{
			IType tp = fieldInfo.field.getType();
			String nm = fieldInfo.field.getName();
			
			MyLogger.log(nm);

			if (fieldInfo.isStatic)
			{
				/* Do nothing. */
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.OBJECT))
			{
				// this.field = right.field.copy();
				MStringExpression expr = new MStringExpression();
				expr.contents = "this." + nm + " = right." + nm + ".copy()";
				meth.body.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.OBJECT_ARRAY))
			{
				// this.field = CPP.copyArray(right.field);
				MStringExpression expr = new MStringExpression(); 
				expr.contents = "this." + nm + " = CPP.copyArray(right." + nm + ")";				
				meth.body.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.BASIC_ARRAY))
			{
				MStringExpression expr = new MStringExpression();
				
				if (ctx.exprEvaluator.getArraySizeExpressions(tp).size() > 1)
				{
					// TODO: Is this right? No!
					// this.field = CPP.copyMultiArray(right.field);
					expr.contents = "this." + nm + " = CPP.copyMultiArray(right." + nm +")";
				}
				else
				{
					// this.field = MIntegerMulti.create(right.field.deep().clone(), 0);
					expr.contents = "this." + nm + " = " +
							ctx.typeMngr.cppToJavaType(tp, TypeType.IMPLEMENTATION) + 
							".create(right." + nm + ".deep().clone(), 0)";
				}
				
				meth.body.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if (ctx.bitfieldMngr.isBitfield(fieldInfo.declarator.getName()))
			{
				// this.set__name(right.get__name());
				MStringExpression expr = new MStringExpression();
				expr.contents = "this.set__" + nm + "(right.get__" + nm + "())";
				meth.body.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if (TypeManager.isBasicType(tp))
			{
				// this.name = MInteger.valueOf(right.name.get());
				MStringExpression expr = new MStringExpression();

				expr.contents = "this." + nm + " = " + 
								ctx.typeMngr.cppToJavaType(tp, TypeType.IMPLEMENTATION) + 
								".valueOf(right." + nm + ".get())";

				meth.body.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.ENUMERATION))
			{
				// this.name = right.name
				MStringExpression expr = new MStringExpression();
				expr.contents = "this." + nm + " = right." + nm;
				meth.body.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.BASIC_POINTER, TypeEnum.OBJECT_POINTER))
			{
				// this.name = right.name.ptrCopy()
				MStringExpression expr = new MStringExpression();
				expr.contents = "this." + nm + " = right." + nm + ".ptrCopy()";
				meth.body.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else
			{
				// TODO: Function pointers, template types.
				MyLogger.logImportant("Unexpected type in copy ctor:" + nm);
				MyLogger.exitOnError();
			}
		}
		return meth;
	}
	
	CppAssign generateAssignMethod(CompositeInfo info, CppClass tyd, IASTDeclSpecifier declSpecifier) throws DOMException
	{
		CppAssign ass = ctx.declModels.new CppAssign();
		
		ass.type = tyd.name;
		ass.body = ctx.stmtModels.new MCompoundStmt();
		
		List<FieldInfo> fields = ctx.converter.collectFieldsForClass(declSpecifier);

		MCompoundStmt ifBlock = ctx.stmtModels.new MCompoundStmt();

		if (info.hasSuper)
		{
			MSuperAssignStmt sup = ctx.stmtModels.new MSuperAssignStmt();
			ifBlock.statements.add(sup);
		}

		for (FieldInfo fieldInfo : fields)
		{
			String nm = fieldInfo.field.getName();
			IType tp = fieldInfo.field.getType();
			
			MyLogger.log(nm);
			
			if (fieldInfo.isStatic)
			{
				/* Do nothing. */
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.OBJECT))
			{
				// this.name.opAssign(right.name);
				MStringExpression expr = new MStringExpression();
				expr.contents = "this." + nm + ".opAssign(right." + nm + ")"; 
				ifBlock.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.OBJECT_ARRAY))
			{
				// TODO
				MFieldReferenceExpression right = ModelCreation.createFieldReference("right", fieldInfo.field.getName());
				MFieldReferenceExpression left = ModelCreation.createFieldReference("this", fieldInfo.field.getName());
				ifBlock.statements.add(ModelCreation.createMethodCall(ctx, "CPP", "assignArray", left, right));
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.BASIC_ARRAY))
			{
				if (ctx.exprEvaluator.getArraySizeExpressions(fieldInfo.field.getType()).size() > 1)
				{
					// TODO
				}
				else
				{
					// System.arraycopy(right.field.deep(), 0, this.field.deep(), 0, this.field.deep().length)
					MStringExpression expr = new MStringExpression();
					expr.contents = "System.arraycopy(right." + nm + ".deep(), 0, this." + nm +
							".deep(), 0, this." + nm + ".deep().length)";
				
					ifBlock.statements.add(ModelCreation.createExprStmt(ctx, expr));
				}
			}
			else if (ctx.bitfieldMngr.isBitfield(fieldInfo.declarator.getName()))
			{
				// this.set__name(right.get__name());
				MStringExpression expr = new MStringExpression();
				expr.contents = "this.set__" + nm + "(right.get__" + nm + "())";
				ifBlock.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if(TypeManager.isBasicType(tp))
			{
				// this.name.set(right.name.get())
				MStringExpression expr = new MStringExpression();
				expr.contents = "this." + nm + ".set(right." + nm + ".get())";
				ifBlock.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.ENUMERATION))
			{
				// this.name = right.name
				MStringExpression expr = new MStringExpression();
				expr.contents = "this." + nm + " = right." + nm;
				ifBlock.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else if (TypeManager.isOneOf(tp, TypeEnum.BASIC_POINTER, TypeEnum.OBJECT_POINTER))
			{
				// this.name = right.name.ptrCopy()
				MStringExpression expr = new MStringExpression();
				expr.contents = "this." + nm + " = right." + nm + ".ptrCopy()";
				ifBlock.statements.add(ModelCreation.createExprStmt(ctx, expr));
			}
			else
			{
				// TODO: Function pointers, template types.
				MyLogger.logImportant("Unknown type in generated opAssign");
				MyLogger.exitOnError();
			}
		}

		if (!ifBlock.statements.isEmpty())
		{
			// if (right != this) { ... } 
			MExpression expr = ModelCreation.createInfixExpr(
					ModelCreation.createLiteral("right"),
					ModelCreation.createLiteral("this"),
					"!=");
			
			MIfStmt stmt = ctx.stmtModels.new MIfStmt();

			stmt.condition = expr;
			stmt.body = ifBlock;

			ass.body.statements.add(stmt);
		}

		MReturnStmt retu = ctx.stmtModels.new MReturnStmt();
		retu.expr = ModelCreation.createLiteral("this");
		
		ass.body.statements.add(retu);
		
		return ass;
	}
}
