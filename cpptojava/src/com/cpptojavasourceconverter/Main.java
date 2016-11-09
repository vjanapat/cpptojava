package com.cpptojavasourceconverter;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.io.FileInputStream;

import java.io.InputStream;
import java.util.Properties;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider;

public class Main
{
	
	static String HOME_PATH = "";
	/**
	 * Main method to start with
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String... args) throws Exception
	{
		GlobalCtx global = new GlobalCtx();
		InputStream input = null;
		Properties prop = new Properties();
		
		
		String INPUT = "";
		String OUTPUT = "";
		String LIST_OF_TESTS = "";
		// Load Config properties to know tests to be ran.
		try {
			input = new FileInputStream("config.properties");
			prop.load(input);
			HOME_PATH = prop.getProperty("HOME_PATH");
			INPUT = prop.getProperty("INPUT");
			OUTPUT = prop.getProperty("OUTPUT");
			LIST_OF_TESTS = prop.getProperty("LIST_OF_TESTS");
		} catch (IOException IE) {
			IE.printStackTrace();
		}
		StringBuffer inputtest = new StringBuffer(HOME_PATH);
		inputtest.append(INPUT);
		inputtest.append(LIST_OF_TESTS);
		BufferedReader br = new BufferedReader(new FileReader( inputtest.toString() ));
	    String line = br.readLine();
	    // Execute each test one after the other.
	    while (line != null) {
	    	if (!line.isEmpty() && !line.startsWith("#"))
	    	{
	    		IASTTranslationUnit tu = getTranslationUnit(HOME_PATH + INPUT + line + ".cpp");
	    		Traverser parser = new Traverser();
	    		String outputCode = parser.traverse(tu, global);
	    		
	    		FileOutputStream fos = new FileOutputStream(HOME_PATH + OUTPUT + line + ".java");
	    		OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8"); 
	    		out.write(outputCode);
	    		out.close();
	    	}
	    	line = br.readLine();
	    }
	    br.close();
	}
	/**
	 * Load contents of C++ file to a Abstract Syntax Tree Format(IASTTranslationUnit)  
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	private static IASTTranslationUnit getTranslationUnit(String filename) throws Exception
	{
		IParserLogService log = new DefaultLogService();
		FileContent ct = FileContent.createForExternalFileLocation(filename);
		return GPPLanguage.getDefault().getASTTranslationUnit(ct, new Scanner(), new FileProvider(), null, 0, log);
	}
	
	private static class FileProvider extends InternalFileContentProvider
	{
		@Override
		public InternalFileContent getContentForInclusion(String path,
				IMacroDictionary macroDictionary) {
			return (InternalFileContent) InternalFileContent.createForExternalFileLocation(path);
		}

		@Override
		public InternalFileContent getContentForInclusion(
				IIndexFileLocation arg0, String arg1) {
			return null;
		}
	}

	
	private static class Scanner implements IScannerInfo
	{
		@Override
		public Map<String, String> getDefinedSymbols() {
			Map<String, String> hm = new HashMap<String, String>();
			// hm.put("NULL", "0"); // example only...
			return hm;
		}		
		
		@Override
		public String[] getIncludePaths() {
			return new String[] { HOME_PATH };
		}
	}
	
}
