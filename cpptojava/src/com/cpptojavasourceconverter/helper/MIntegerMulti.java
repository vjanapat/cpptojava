package com.cpptojavasourceconverter.helper;

public class MIntegerMulti implements IInteger
{
	private final int[] val;
	private int currentOffset;
	
	private MIntegerMulti(int[] arr, int offset)
	{
		val = arr;
		currentOffset = offset;
	}
	
	private MIntegerMulti(int dim1)
	{
		val = new int[dim1];
		currentOffset = 0;
	}
	
	public static IInteger create(int dim1)
	{
		return new MIntegerMulti(dim1);
	}
	
	public static IInteger create(int[] arr, int offset)
	{
		return new MIntegerMulti(arr, offset);
	}
	
	@Override
	public MIntegerMulti addressOf() 
	{
		return this;
	}

	@Override
	public int get()
	{
		return val[currentOffset];
	}

	@Override
	public int set(int value) 
	{
		val[currentOffset] = value;
		return value;
	}

	@Override
	public MIntegerMulti ptrPostInc()
	{
		int temp = currentOffset++;
		return new MIntegerMulti(val, temp);
	}

	@Override
	public MIntegerMulti ptrPostDec() 
	{
		int temp = currentOffset++;
		return new MIntegerMulti(val, temp);
	}

	@Override
	public MIntegerMulti ptrAdjust(int cnt) 
	{
		currentOffset += cnt;
		return null;
	}

	@Override
	public MIntegerMulti ptrOffset(int cnt) 
	{
		return new MIntegerMulti(val, currentOffset + cnt);
	}

	@Override
	public int postInc() 
	{
		return val[currentOffset]++;
	}

	@Override
	public int postDec() 
	{
		return val[currentOffset]--;
	}

	@Override
	public MIntegerMulti ptrCopy()
	{
		// must make a copy of currentOffset like real pointers
		return new MIntegerMulti(val, currentOffset);
	}

	@Override
	public IPtrObject<IInteger> ptrAddressOf() 
	{
		return PtrObject.valueOf((IInteger) this);
	}

	@Override
	public int ptrCompare()
	{
		// Get the pointer offset to compare with
		// another pointer offset from the same block.
		return currentOffset + 1;
	}

	@Override
	public int[] deep() {
		return val;
	}
}
