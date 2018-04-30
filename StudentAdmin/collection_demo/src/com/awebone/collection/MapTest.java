package com.awebone.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

public class MapTest {
	public Map<String,Student> students;
	public MapTest() {
		this.students=new HashMap<String,Student>();
	}
	
	private void testPut() {
		//创建Scanner对象，用来获取输入的学生ID和姓名
		Scanner console=new Scanner(System.in);
		int i=0;
		while(i<3) {
			System.out.println("请输入学生ID：");
			String ID=console.next();
			Student st=students.get(ID);
			//判断ID是否被占用
			if(st==null) {
				System.out.println("请输入学生姓名：");
				String name=console.next();
				Student newStudent=new Student(ID,name);
				students.put(ID, newStudent);
				System.out.println("成功添加学生："+students.get(ID).name);
				i++;
			}else {
				System.out.println("该学生ID已被占用");
				continue;
			}
		}
	}
	
	private void testKeySet() {
		//通过KeySet方法，返回Map中所有键的集合
		Set<String> keySet=students.keySet();
		//取得students的容量
		System.out.println("总共有："+students.size()+"个学生");
		//遍历kaySet，取得每一个键，再调用get方法取得每个键对应的value
		for (String stuId : keySet) {
			Student st=students.get(stuId);
			if(st!=null) {
				System.out.println("学生："+st.name);
			}
		}
	}
	
	private void testRemove() {
		//获取从键盘输入的待删除的学生ID字符串
		Scanner console=new Scanner(System.in);
		while(true) {
			//提示输入删除的学生ID
			System.out.println("请输入要删除的学生ID：");
			String ID=console.next();
			Student st=students.get(ID);
			if(st==null) {
				System.out.println("该ID不存在");
				continue;
			}
			students.remove(ID);
			System.out.println("成功删除学生："+st.name);
			break;
		}
	}
	
	//通过entrySet方法来遍历Map
	private void testEntrySet() {
		//通过entrySet方法,返回Map中的所有键值对
		Set<Entry<String, Student>> entrySet=students.entrySet();
		for (Entry<String, Student> entry : entrySet) {
			System.out.println("取得键："+entry.getKey());
			System.out.println("对应的值为："+entry.getValue().name);
		}
	}
	
	//利用put方法修改Map中已有的映射
	private void testModify() {
		System.out.println("请输入要修改的学生ID：");
		Scanner console=new Scanner(System.in);
		while(true) {
			String ID=console.next();
			Student st=students.get(ID);
			if(st==null) {
				System.out.println("该ID不存在");
				continue;
			}
			System.out.println("当前学生ID，所对应的学生为："+st.name);
			System.out.println("请输入新的学生的姓名：");
			String name=console.next();
			Student newStudent=new Student(ID,name);
			students.put(ID, newStudent);
			System.out.println("修改成功");
			break;
		}
	}
	
	/**
	 * 测试Map中是否包含某个Key值或Value值
	 */
	private void testContainsKeyOrValue() {
		// 提示输入学生ID
		System.out.println("请输入要查询的学生id");
		Scanner console=new Scanner(System.in);
		String id=console.next();
		//在Map中，用containsKey()方法，来判断是否包含某个Key值，用containsValue()方法，来判断是否包含某个Value值
		System.out.println("您输入的学生ID为："+id+"，在学生映射表中是否存在："+students.containsKey(id));
		if(students.containsKey(id)) {
			System.out.println("对应的学生为："+students.get(id).name);
		}
		
		System.out.println("请输入要查询的学生name");
		String name=console.next();
		//在Map中，用containsKey()方法，来判断是否包含某个Key值，用containsValue()方法，来判断是否包含某个Value值
		if(students.containsValue(new Student(null,name))) {
			System.out.println("确实包含学生为："+name);
		}else {
			System.out.println("学生映射表中不存在该学生");
		}
	}

	public static void main(String[] args) {
		MapTest mt=new MapTest();
		mt.testPut();
		mt.testKeySet();
//		mt.testRemove();
//		mt.testEntrySet();
//		mt.testModify();
//		mt.testEntrySet();
		
		mt.testContainsKeyOrValue();
	}

}
