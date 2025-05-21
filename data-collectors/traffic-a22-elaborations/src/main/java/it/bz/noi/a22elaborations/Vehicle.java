// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.a22elaborations;

public class Vehicle
{

	private String stationcode;
	private long timestamp;
	private double distance;
	private double headway;
	private double length;
	private int axles;
	private boolean against_traffic;
	private int class_nr;
	private double speed;
	private int direction;
	private String plateIntiails;
	private String plateNat;

	public Vehicle(String stationcode, long timestamp, double distance, double headway, double length, int axles,
			boolean against_traffic, int class_nr, double speed, int direction, String plateIntiails, String plateNat)
	{
		this.stationcode = stationcode;
		this.timestamp = timestamp;
		this.distance = distance;
		this.headway = headway;
		this.length = length;
		this.axles = axles;
		this.against_traffic = against_traffic;
		this.class_nr = class_nr;
		this.speed = speed;
		this.direction = direction;
		this.plateIntiails = plateIntiails;
		this.plateNat = plateNat;
	}

	public String getStationcode()
	{
		return stationcode;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public double getDistance()
	{
		return distance;
	}

	public double getHeadway()
	{
		return headway;
	}

	public double getLength()
	{
		return length;
	}

	public int getAxles()
	{
		return axles;
	}

	public boolean isAgainst_traffic()
	{
		return against_traffic;
	}
	
	public String getPlateIntiails()
	{
		return plateIntiails;
	}

	public String getPlateNat()
	{
		return plateNat;
	}

	// classe = 1 -> "AUTOVETTURA"
	// classe = 2 -> "FURGONE"
	// classe = 3 -> "AUTOCARRO"
	// classe = 4 -> "AUTOARTICOLATO"
	// classe = 5 -> "AUTOTRENO"
	// classe = 6 -> "PULLMAN"
	// classe = 7 -> "MOTO O MOTOCICLO"
	public int getClass_nr()
	{
		return class_nr;
	}

	public double getSpeed()
	{
		return speed;
	}

	public int getDirection()
	{
		return direction;
	}

	// HEAVY VEHICLES: AUTOARTICOLATO or AUTOTRENO or AUTOCARRO if lunghezza >= 890 cm
	public boolean isHeavy() {
		return getClass_nr() == 4 || getClass_nr() == 5 || (getClass_nr() == 3 && getLength() >= 890);
	}	
	
	// LIGHT VEHICLES: AUTOVETTURA or FURGONE or MOTO O MOTOCICLO
	public boolean isLight() {
		return getClass_nr() == 1 || getClass_nr() == 2 || getClass_nr() == 7;
	}

	// BUSES: AUTOCARRO if lunghezza < 890 cm or PULLMAN
	public boolean isBus() {
		return getClass_nr() == 6 || (getClass_nr() == 3 && getLength() < 890);
	}
}
