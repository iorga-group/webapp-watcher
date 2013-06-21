package com.iorga.iraj.util;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

@Entity
@Table(name = "PROFILE")
public class Profile implements Serializable {
	private static final long serialVersionUID = -3753697674732869073L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID_PROF", unique = true, nullable = false)
	private Integer id;

	@Version
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "VERSION", length = 19)
	private Date version;

	@Column(name = "LB_PROF", nullable = false, length = 50)
	private String label;

	@Column(name = "CD_PROF", nullable = false, length = 15)
	private String code;



	public Profile(final String label, final String code) {
		this.label = label;
		this.code = code;
	}

	public Profile() {}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public Date getVersion() {
		return version;
	}

	public void setVersion(final Date version) {
		this.version = version;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}
}
