package com.awebone.collection;

import java.util.Comparator;

public class StudentComparator implements Comparator<Student> {

	@Override
	public int compare(Student arg0, Student arg1) {
		// TODO Auto-generated method stub
		return arg0.name.compareTo(arg1.name);
	}

}
