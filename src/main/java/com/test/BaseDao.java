package com.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@SuppressWarnings("unchecked")
public abstract class BaseDao<T> {
	private  Class<T> clazz;
	{
		Type type = this.getClass().getGenericSuperclass();
		System.out.println(type);
		ParameterizedType pt =(ParameterizedType)type;
		System.out.println(pt.getActualTypeArguments().length);
		clazz = (Class<T>)pt.getActualTypeArguments()[0];
		
		
	}

}
