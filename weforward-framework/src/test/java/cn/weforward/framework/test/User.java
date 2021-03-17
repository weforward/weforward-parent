/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.framework.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import cn.weforward.protocol.doc.annotation.DocAttribute;
import cn.weforward.protocol.doc.annotation.DocObject;

@DocObject
public class User {

	private String m_Name;

	private User m_User;

	private List<User> m_Users;

	private UserList m_UserList;

	private BigInteger m_Age;

	private BigDecimal m_AgeDetail;

	public User() {

	}

	public void setName(String name) {
		m_Name = name;
	}

	public String getName() {
		return m_Name;
	}

	public void setUser(User user) {
		m_User = user;
	}

	@DocAttribute
	public User getUser() {
		return m_User;
	}

	public void setUsers(List<User> users) {
		m_Users = users;
	}

	@DocAttribute
	public List<User> getUsers() {
		return m_Users;
	}

	public void setUserList(UserList list) {
		m_UserList = list;
	}

	@DocAttribute
	public UserList getUserList() {
		return m_UserList;
	}

	public void setAge(BigInteger v) {
		m_Age = v;
	}

	@DocAttribute
	public BigInteger getAge() {
		return m_Age;
	}

	public void setAgeDetail(BigDecimal v) {
		m_AgeDetail = v;
	}

	@DocAttribute
	public BigDecimal getAgeDetail() {
		return m_AgeDetail;
	}
}
