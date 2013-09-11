/*
 * Copyright (C) 2013 Iorga Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/].
 */
package com.iorga.iraj.util;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

@NamedQueries({
	@NamedQuery(
		name = User.QUERY_ACTIVE_BY_LOGIN_AND_PASSWORD,
		query = "from User where login = :login and password = :password and active is true"),
	@NamedQuery(
		name = User.EXISTS_WITH_LOGIN,
		query = "select 1 from User where login = :login"),
	@NamedQuery(
		name = User.UPDATE_SET_ACTIVE_FOR_USERS,
		query = "update User user set user.active = :active where user in (:users)"),
	@NamedQuery(
		name = User.GET_PASSWORD_WITH_LOGIN,
		query = "select password from User where login = :login")
})
@Entity
@Table(name = "USER")
public class User implements Serializable, Principal {
	private static final long serialVersionUID = 1L;

	public static final String QUERY_ACTIVE_BY_LOGIN_AND_PASSWORD = "User.QUERY_ACTIVE_BY_LOGIN_AND_PASSWORD";
	public static final String EXISTS_WITH_LOGIN = "User.EXISTS_WITH_LOGIN";
	public static final String UPDATE_SET_ACTIVE_FOR_USERS = "User.UPDATE_SET_ACTIVE_FOR_USERS";
	public static final String GET_PASSWORD_WITH_LOGIN = "User.GET_PASSWORD_WITH_LOGIN";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID", unique = true, nullable = false)
	private Integer userId;

	@Version
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "VERSION", length = 19)
	private Date version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ID_PROF", nullable = false)
	private Profile profile;

	@Column(name = "LB_LOGI", nullable = false, length = 20)
	private String login;

	@Column(name = "LB_PASS", nullable = false, length = 100)
	private String password;

	@Column(name = "LB_LAST_NAME", nullable = false, length = 50)
	private String lastName;

	@Column(name = "LB_FIRS_NAME", nullable = false, length = 50)
	private String firstName;

	@Column(name = "BL_ACTI", nullable = false)
	private Boolean active;

	@Temporal(TemporalType.DATE)
	@Column(name = "DT_CREA", nullable = true, length = 10)
	private Date creationDate;

	@Column(name = "CD_USERCREA", nullable = true, length = 10)
	private String creationLogin;

	@Temporal(TemporalType.DATE)
	@Column(name = "DT_MODI", nullable = true, length = 10)
	private Date modificationDate;

	@Column(name = "CD_USERMODI", nullable = true, length = 10)
	private String modificationLogin;


	public User(final String login, final String password, final String lastName, final String firstName, final Profile profile) {
		this.login = login;
		this.password = password;
		this.lastName = lastName;
		this.firstName = firstName;
		this.profile = profile;
		this.active = Boolean.TRUE;
	}

	public User() {}


	@Override
	public String getName() {
		return getLogin();
	}


	public Integer getUserId() {
		return userId;
	}

	public void setUserId(final Integer id) {
		this.userId = id;
	}

	public Date getVersion() {
		return version;
	}

	public void setVersion(final Date version) {
		this.version = version;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(final Profile profile) {
		this.profile = profile;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(final String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(final Boolean active) {
		this.active = active;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(final Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getCreationLogin() {
		return creationLogin;
	}

	public void setCreationLogin(final String creationLogin) {
		this.creationLogin = creationLogin;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(final Date modificationDate) {
		this.modificationDate = modificationDate;
	}

	public String getModificationLogin() {
		return modificationLogin;
	}

	public void setModificationLogin(final String modificationLogin) {
		this.modificationLogin = modificationLogin;
	}
}
