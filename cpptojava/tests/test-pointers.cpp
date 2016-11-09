void ptr_test_method()
{
	int a;
	int b[20];

	// Test a variety of pointer expressions...
	int * c = &a;
	int * d = b;
	int * e = c;
	int * f = &b[1];
	int * g = d++;
	int * h = --g;
	int * i = d + 10;
	int * j = ++d + 5;
	int k = f[5];
	int l = *j;
	int * m = &f[4];

	// Test compound ptr assignment...
	f += 2;
	f -= 5;

	// Test null pointers...
	f = 0;
	f == 0;
	f != 0;
	if (f) ;
	int * n = 0;

	// Test plain assignment...
	f[4] = 111;
	*f = 112;
	(((*(f + 2)))) = 113;
	*(f--) = 114;

	// Test compound assignment...
	f[1] *= 3;
	(*f) += 4;

	// Test pointer comparison
	f < &f[1];
	&f[0] == f;

	&f[1] - &f[0];

	// Test addressof ptr
	&f;
}

class foo { };

int func3(foo a, foo * b, foo ** c, foo& d, const foo& e, int& f)
{
	// Test various pointer to basic types...
	char * ptr1;
	int ** ptr2;
	short * ptr3;
	unsigned * ptr4;
	bool * ptr5;
	int * ptr6;

	// Test multiple declarators with one declaration...
	foo * ptr7, * ptr8;

	// Test address of operator...
	ptr7 = &a;

	// Test basic pointer assignment...
	ptr8 = ptr7;

	// Test number literals...
	return 1072;
}

int * top_level_ptr = new int;
int * top_level_ptr2 = new int[25];

