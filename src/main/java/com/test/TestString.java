package com.test;

public class TestString {
	public  final String h1="hello2";
	public  String getData() {
		return "hello";
	}
	public static void main(String[] args) {
		String a="hello";
		String b="hello";
		String c=a+b;
		String d=a+"hello";
		String e=a+b;
		String f = "hellohello";
		System.out.println("a==b: " + (a==b));
		System.out.println("c==d: "+ (c==d));
		System.out.println("c==e: "+ (c==e));
		System.out.println("d==f: "+ (d==f));
	
	}

}
