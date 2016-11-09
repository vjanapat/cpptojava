package com.cpptojavasourceconverter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;

import com.cpptojavasourceconverter.InitializationManager.InitType;
import com.cpptojavasourceconverter.SourceConverter.CompositeInfo;
import com.cpptojavasourceconverter.SourceConverter.FieldInfo;
import com.cpptojavasourceconverter.TypeManager.NameType;
import com.cpptojavasourceconverter.TypeManager.TypeType;
import com.cpptojavasourceconverter.models.DeclarationModels.*;
import com.cpptojavasourceconverter.models.ExpressionModels.*;
import com.cpptojavasourceconverter.models.StmtModels.*;

class FunctionManager 
{
	private final TranslationUnitContext ctx;
	
	public FunctionManager(TranslationUnitContext con) {
		ctx = con;
	}
	
	/**
	 * Gets the type of the return value of a function.
	 * @return
	 */
	private String evalReturnType(IBinding funcBinding) throws DOMException
	{
		if (funcBinding instanceof IFunction)
		{
			IFunction func = (IFunction) funcBinding;
			IFunctionType funcType = func.getType();
			return ctx.typeMngr.cppToJavaType(funcType.getReturnType(), TypeType.INTERFACE);
		}

		MyLogger.logImportant("Unexpected binding for return type: " + funcBinding.getClass().getCanonicalName());
		MyLogger.exitOnError();
		return null;
	}
	
	/**
	 * Evaluates the parameters for a function. A SingleVariableDeclaration
	 * contains a type and a name. 
	 */
	private List<MSimpleDecl> evalParameters(IBinding funcBinding) throws DOMException
	{
		List<MSimpleDecl> ret = new ArrayList<MSimpleDecl>();

		if (funcBinding instanceof IFunction)
		{
			IFunction func = (IFunction) funcBinding;
			IParameter[] params = func.getParameters();

			int missingCount = 0;
			for (IParameter param : params)
			{	
				MSimpleDecl var = ctx.declModels.new MSimpleDecl();
				var.type = ctx.typeMngr.cppToJavaType(param.getType(), TypeType.INTERFACE);

				MyLogger.log("Found param: " + param.getName());

				// Remember C++ can have missing function argument names...
				if (param.getName() == null || param.getName().isEmpty())
					var.name = "missing" + missingCount++;
				else
					var.name = param.getName();

				ret.add(var);
			}
		}
		return ret;
	}
	
	/**
	 * Gets the default expressions for function arguments (null where default is not provided).
	 */
	private List<MExpression> getDefaultValues(IASTFunctionDeclarator func) throws DOMException
	{
		IASTStandardFunctionDeclarator declarator = (IASTStandardFunctionDeclarator) func;
		IASTParameterDeclaration[] params = declarator.getParameters();

		List<MExpression> exprs = new ArrayList<MExpression>();

		for (IASTParameterDeclaration param : params)
		{
			IASTDeclarator paramDeclarator = param.getDeclarator(); 

			IBinding binding = paramDeclarator.getName().resolveBinding();
			IType tp = ctx.converter.evalBindingReturnType(binding);
			
			if (paramDeclarator.getInitializer() != null)
			{
				exprs.add(ctx.initMngr.eval1Init(paramDeclarator.getInitializer(), tp, paramDeclarator.getName(), InitType.WRAPPED));
			}
			else
			{
				exprs.add(null);
			}
		}

		return exprs;
	}
	
	/**
	 * Evaluates a function definition and converts it to Java.
	 */
	void evalFunction(IASTDeclaration declaration) throws DOMException
	{
		IASTFunctionDefinition func = (IASTFunctionDefinition) declaration;
		IBinding funcBinding = func.getDeclarator().getName().resolveBinding();
		
		CppFunction method = (CppFunction) ctx.typeMngr.getDeclFromTypeName(ctx.converter.evalBindingReturnType(funcBinding), func.getDeclarator().getName()); 
				
		if (method == null)
		{
			method = ctx.declModels.new CppFunction();
			
			ctx.typeMngr.registerDecl(method, ctx.converter.evalBindingReturnType(funcBinding),
					func.getDeclarator().getName(), NameType.CAMEL_CASE,
					func.getDeclarator().getContainingFilename(), 
					func.getDeclarator().getFileLocation().getStartingLineNumber());
		}

		method.isStatic = ((IFunction) funcBinding).isStatic();
		method.retType = evalReturnType(funcBinding);

		if (funcBinding instanceof ICPPConstructor)
		{
			method.isCtor = true;
			method.retType = null;
		}
		else if (funcBinding instanceof ICPPMethod &&
				((ICPPMethod) funcBinding).isDestructor())
		{
			method.isDtor = true;
			method.retType = null;
		}
		else if (func.getDeclarator().getName() instanceof ICPPASTConversionName)
		{
			ICPPASTConversionName conversion = (ICPPASTConversionName) func.getDeclarator().getName();
			method.isCastOperator = true;
			method.castType = conversion.getTypeId();
		}
		
		method.args.addAll(evalParameters(funcBinding));
		
		ctx.stackMngr.reset();

		// We need this so we know if the return expression needs to be made
		// java boolean.
		if (funcBinding instanceof IFunction)
		{
			IFunction funcb = (IFunction) funcBinding;
			IFunctionType funcType = funcb.getType();
			ctx.currentReturnType = funcType.getReturnType();
		}
		
		// eval1Stmt work recursively to generate the function body.
		method.body = (MCompoundStmt) ctx.stmtEvaluator.eval1Stmt(func.getBody());

		// For debugging purposes, we put return type back to null.
		ctx.currentReturnType = null;
		
		// If we have any local variables that are objects, or arrays of objects,
		// we must create an explicit stack so they can be added to it and explicity
		// cleaned up at termination points (return, break, continue, end block).
		if (ctx.stackMngr.getMaxLocalVariableId() != null)
		{
			MStringExpression expr = new MStringExpression();
			expr.contents = "Object[] __stack = new Object[" + ctx.stackMngr.getMaxLocalVariableId() + "]";
			method.body.statements.add(0, ModelCreation.createExprStmt(ctx, expr));
		}

		CompositeInfo info = ctx.converter.currentInfoStack.peekFirst();

		if (func instanceof ICPPASTFunctionDefinition && info != null)
		{
			List<FieldInfo> fields = ctx.converter.collectFieldsForClass(info.declSpecifier);
			MExpression superInit = null;
			
			// Now check for C++ constructor initializers...
			ICPPASTFunctionDefinition funcCpp = (ICPPASTFunctionDefinition) func;
			ICPPASTConstructorChainInitializer[] chains = funcCpp.getMemberInitializers();

			if (chains != null && chains.length != 0)
			{
				for (ICPPASTConstructorChainInitializer chain : chains)
				{
					// We may have to call a constructor... 
					if ((chain.getMemberInitializerId().resolveBinding() instanceof IVariable &&
						((IVariable)chain.getMemberInitializerId().resolveBinding()).getType() instanceof ICompositeType)) // &&
						//!(eval1Expr(chain.getInitializerValue()) instanceof ClassInstanceCreation))
					{
						MLiteralExpression lit = 
								ModelCreation.createLiteral(ctx.typeMngr.cppToJavaType(((IVariable) chain.getMemberInitializerId().resolveBinding()).getType(), TypeType.IMPLEMENTATION));
						
						MClassInstanceCreation create = new MClassInstanceCreation();
						create.name = lit;
						create.args.add(ctx.initMngr.eval1Init(chain.getInitializer(), ctx.converter.evalBindingReturnType(chain.getMemberInitializerId().resolveBinding()), chain.getMemberInitializerId(), InitType.WRAPPED));
						
						// Match this initializer with the correct field.
						for (FieldInfo fieldInfo : fields)
						{
							if (chain.getMemberInitializerId().resolveBinding().getName().equals(fieldInfo.field.getName()))
								fieldInfo.init = create;
						}
					}
					else if (chain.getInitializer() != null)
					{
						// Match this initializer with the correct field.
						for (FieldInfo fieldInfo : fields)
						{
							if (chain.getMemberInitializerId().resolveBinding().getName().equals(fieldInfo.field.getName()) &&
								ctx.bitfieldMngr.isBitfield(fieldInfo.declarator.getName()))
							{
								IType tp = ctx.converter.evalBindingReturnType(chain.getMemberInitializerId().resolveBinding());
								fieldInfo.init = ctx.initMngr.eval1Init(chain.getInitializer(), tp, fieldInfo.declarator.getName(), InitType.WRAPPED);
							}
							else if (chain.getMemberInitializerId().resolveBinding().getName().equals(fieldInfo.field.getName()))
							{
								IType tp = ctx.converter.evalBindingReturnType(chain.getMemberInitializerId().resolveBinding());
								fieldInfo.init = ctx.initMngr.eval1Init(chain.getInitializer(), tp , fieldInfo.declarator.getName(), InitType.WRAPPED);
							}
						}
						
						if (info.hasSuper && chain.getMemberInitializerId().resolveBinding().getName().equals(info.superClass))
						{
							superInit = ctx.initMngr.eval1Init(chain.getInitializer(), ctx.converter.evalBindingReturnType(chain.getMemberInitializerId().resolveBinding()), chain.getMemberInitializerId(), InitType.WRAPPED);
						}
					}
					else
					{
						// TODO...
					}
				}
			}
			
			if (method.isCtor)
			{
				MyLogger.log("ctor");
				
				// This function generates an initializer for all fields that need initializing.
				ctx.specialGenerator.generateCtorStatements(fields, method.body);
				
				// If we have a super class, call super constructor.
				if (info.hasSuper)
				{
					MFunctionCallExpression expr = ModelCreation.createFuncCall("super");
					
					if (superInit != null)
						expr.args.add(superInit);
					
					method.body.statements.add(0, ModelCreation.createExprStmt(ctx, expr));
				}
			}
			else if (method.isDtor)
			{
				// This will destruct all fields that need destructing.
				ctx.specialGenerator.generateDtorStatements(fields, method.body, info.hasSuper);
			}
		}
			
		// Generates functions for default arguments.
		makeDefaultCalls(func.getDeclarator(), funcBinding, method.parent);
	}
	
	/**
	 * Builds default argument function calls.
	 * For example:
	 *   int func_with_defaults(int one, int two = 5);
	 * would generate:
	 * 	 int func_with_defaults(int one) {
	 *	     return func_with_defaults(one, 5);
	 *   }
	 */
	void makeDefaultCalls(IASTFunctionDeclarator func, IBinding funcBinding, CppClass parent) throws DOMException
	{
		// getDefaultValues will return an item for every parameter.
		// ie. null for params without a default value.
		List<MExpression> defaultValues = getDefaultValues(func);

		// We start from the right because C++ can only have default values at the
		// right and we can stop when we get to a null (meaning there is no more defaults).
		for (int k = defaultValues.size() - 1; k >= 0; k--)
		{
			if (defaultValues.get(k) == null)
				break;

			// Create the method declaration.
			// This will handle void return types.
			CppFunction methodDef = ctx.declModels.new CppFunction();
	
			methodDef.name = TypeManager.getSimpleName(func.getName());
			methodDef.retType = evalReturnType(funcBinding);
			
			// This gets a parameter variable declaration for each param.
			List<MSimpleDecl> list = evalParameters(funcBinding);

			// Add a param declaration for each param up until we want to use
			// default values (which won't have a param).
			for (int k2 = 0; k2 < k; k2++)
				methodDef.args.add(list.get(k2));

			// This code block simple calls the original method with
			// passed in arguments plus default arguments.
			MFunctionCallExpression method = ModelCreation.createFuncCall(TypeManager.getSimpleName(func.getName()));

			// Add the passed in params by name.
			List<String> names = getArgumentNames(funcBinding);
			for (int k3 = 0; k3 < k; k3++)
			{
				method.args.add(ModelCreation.createLiteral(names.get(k3)));
			}

			// Now add the default values.
			List<MExpression> vals = getDefaultValues(func);
			for (int k4 = k; k4 < defaultValues.size(); k4++)
				method.args.add(vals.get(k4));

			MCompoundStmt block = ctx.stmtModels.new MCompoundStmt();
			
			// If not a void function, return the result of the method call.
			if (evalReturnType(funcBinding).toString().equals("void"))
			{
				MExprStmt exprStmt = ModelCreation.createExprStmt(ctx, method);
				block.statements.add(exprStmt);
			}
			else
			{
				MReturnStmt retu = ctx.stmtModels.new MReturnStmt();
				retu.expr = method;
				block.statements.add(retu);
			}

			methodDef.body = block;
			parent.declarations.add(methodDef);
		}
	}
	
	/**
	 * Gets the argument names for a function.
	 */
	private List<String> getArgumentNames(IBinding funcBinding) throws DOMException
	{
		List<String> names = new ArrayList<String>();

		if (funcBinding instanceof IFunction)
		{
			IFunction func = (IFunction) funcBinding;
			IParameter[] params = func.getParameters();

			int missingCount = 0;
			for (IParameter param : params)
			{	
				if (param.getName() == null || param.getName().isEmpty())
					names.add("missing" + missingCount++);
				else
					names.add(param.getName());
			}
		}
		return names;
	}
}
