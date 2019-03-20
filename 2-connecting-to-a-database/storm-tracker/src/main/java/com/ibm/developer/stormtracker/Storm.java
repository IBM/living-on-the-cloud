package com.ibm.developer.stormtracker;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "storms")
@SequenceGenerator(name = "storms_id_generator", allocationSize = 1, initialValue = 10)
public class Storm {

	@Id
	@GeneratedValue(generator = "storms_id_generator")
	private long id;
	@Column(name = "start_date")
	private String startDate;
	@Column(name = "end_date")
	private String endDate;
	@Column(name = "start_location")
	private String startLocation;
	@Column(name = "end_location")
	private String endLocation;
	@Column(name = "type")
	private String type;
	@Column(name = "intensity")
	private int intensity;

	Storm() {
	}

	public Storm(String startDate, String endDate, String startLocation, String endLocation, String type,
			int intensity) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.startLocation = startLocation;
		this.endLocation = endLocation;
		this.type = type;
		this.intensity = intensity;
	}

	public long getId() {
		return id;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public String getStartLocation() {
		return startLocation;
	}

	public String getEndLocation() {
		return endLocation;
	}

	public String getType() {
		return type;
	}

	public int getIntensity() {
		return intensity;
	}

}
