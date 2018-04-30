package com.awebone.collection;

import java.util.ArrayList;
import java.util.List;

public class TestGeneric {
	
	public List<Course> courses;
	
	public TestGeneric() {
		this.courses=new ArrayList<Course>();
	}
	
	public void testadd() {
		Course cr1=new Course("1","大学语文");
		courses.add(cr1);
		//泛型集合中，不能添加泛型规定的类型及其子类型以外的对象，否则会报错
		Course cr2=new Course("2","Java");
		courses.add(cr2);
	}
	
	public void testForEach() {
		for(Course cr:courses) {
			System.out.println(cr.id+":"+cr.name);
		}
	}
	
	/**
	 * 泛型集合可以添加泛型的子类型的对象实例
	 * @param args
	 */
	private void testChild() {
		Course ccr=new Course();
		ccr.id="3";
		ccr.name="子类型课程对象";
		courses.add(ccr);
	}

	public static void main(String[] args) {
		TestGeneric tg=new TestGeneric();
		tg.testadd();
		tg.testForEach();
		tg.testChild();
		tg.testForEach();
	}

}
