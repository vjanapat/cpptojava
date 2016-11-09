package com.cpptojavasourceconverter.helper;

public interface CppType<T>
{
	void destruct();
	T copy();
	T opAssign(T right);
}
