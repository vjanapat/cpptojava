package com.cpptojavasourceconverter;

import org.eclipse.cdt.core.dom.ast.IType;

import com.cpptojavasourceconverter.models.DeclarationModels;
import com.cpptojavasourceconverter.models.StmtModels;

public class TranslationUnitContext 
{
	SourceConverter     converter;
	StackManager        stackMngr;
	ExpressionEvaluator exprEvaluator;
	StmtEvaluator       stmtEvaluator;
	BitfieldManager     bitfieldMngr;
	EnumManager         enumMngr;
	FunctionManager     funcMngr;
	SpecialGenerator    specialGenerator;
	InitializationManager initMngr;
	TypeManager           typeMngr;
	GlobalCtx             global;
	StmtModels            stmtModels;
	public DeclarationModels     declModels;
	public int                   tabLevel;
	
	String currentFileName;
	IType currentReturnType;
}
