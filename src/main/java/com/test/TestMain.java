package com.test;

import java.io.IOException;
import java.util.List;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import com.entity.Blog;
import com.mapper.BlogMapper;

public class TestMain {
	private SqlSessionFactoryBuilder builder;
	private SqlSessionFactory factory;
	private SqlSession session;

	@Before
	public void before() throws IOException {
		builder = new SqlSessionFactoryBuilder();
		SqlSessionFactory factory = builder.build(Resources.getResourceAsStream("mybatis-config.xml"));
		session = factory.openSession();
	}
	@Test
	public void testByNamespace() {
		List<Blog> selectList = session.selectList("org.mybatis.example.BlogMapper.selectBlog");
		for (Blog blog : selectList) {
			System.out.println(blog);

		}
	}
	
	@Test
	public void testByMapper() {
		BlogMapper mapper = session.getMapper(BlogMapper.class);
		List<Blog> blogs =mapper.selectBlog();
		for (Blog blog : blogs) {
			System.out.println("By mapper: ->>" + blog);

		}
	}

	public static void main(String[] args) {
		
	}
}
