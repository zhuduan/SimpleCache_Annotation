package org.zhuduan.cache.test;

import java.io.Serializable;

public class TestPojo implements Serializable{

	private static final long serialVersionUID = 8105294098062113668L;

	private Integer id;
	
	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString(){
		return ("I am testPojo");
	}
}
