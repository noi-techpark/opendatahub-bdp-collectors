// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.a22elaborations;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dto.DataTypeDto;

@Component
public class SyncDatatype
{
	static final String NR_LIGHT_VEHICLES = "Nr. Light Vehicles";
	static final String NR_HEAVY_VEHICLES = "Nr. Heavy Vehicles";
	static final String NR_BUSES = "Nr. Buses";
	static final String NR_EQUIVALENT_VEHICLES = "Nr. Equivalent Vehicles";
	static final String AVERAGE_SPEED_LIGHT_VEHICLES = "Average Speed Light Vehicles";
	static final String AVERAGE_SPEED_HEAVY_VEHICLES = "Average Speed Heavy Vehicles";
	static final String AVERAGE_SPEED_BUSES = "Average Speed Buses";
	static final String VARIANCE_SPEED_LIGHT_VEHICLES = "Variance Speed Light Vehicles";
	static final String VARIANCE_SPEED_HEAVY_VEHICLES = "Variance Speed Heavy Vehicles";
	static final String VARIANCE_SPEED_BUSES = "Variance Speed Buses";
	static final String AVERAGE_GAP = "Average Gap";
	static final String AVERAGE_HEADWAY = "Average Headway";
	static final String AVERAGE_DENSITY = "Average Density";
	static final String AVERAGE_FLOW = "Average Flow";

	static final String EURO_CATEGOY_PCT = "Euro Emission Standard";
	static final String PLATE_NATIONALITY_COUNT = "Vehicle Count by Nationality";

	private static Logger LOG = LoggerFactory.getLogger(SyncDatatype.class);

	@Autowired
	private A22TrafficJSONPusher pusher;

	/**
	 * Saves all data types to the bdp-core
	 */
	public void saveDatatypes()
	{

		List<DataTypeDto> dataTypeDtoList = new ArrayList<>();
		DataTypeDto lightVehicles = new DataTypeDto(NR_LIGHT_VEHICLES, "", "Number of light vehicles", "Mean");
		dataTypeDtoList.add(lightVehicles);
		DataTypeDto heavyVehicles = new DataTypeDto(NR_HEAVY_VEHICLES, "", "Number of heavy vehicles", "Mean");
		dataTypeDtoList.add(heavyVehicles);
		DataTypeDto autobus = new DataTypeDto(NR_BUSES, "", "Number of buses", "Mean");
		dataTypeDtoList.add(autobus);
		DataTypeDto equivalentVehicles = new DataTypeDto(NR_EQUIVALENT_VEHICLES, "", "Number of equivalent vehicles",
				"Mean");
		dataTypeDtoList.add(equivalentVehicles);
		DataTypeDto averageSpeedLight = new DataTypeDto(AVERAGE_SPEED_LIGHT_VEHICLES, "km/h",
				"Average Speed Light Vehicles", "Mean");
		dataTypeDtoList.add(averageSpeedLight);
		DataTypeDto averageSpeedHeavy = new DataTypeDto(AVERAGE_SPEED_HEAVY_VEHICLES, "km/h",
				"Average Speed Heavy Vehicles", "Mean");
		dataTypeDtoList.add(averageSpeedHeavy);
		DataTypeDto averageSpeedBuses = new DataTypeDto(AVERAGE_SPEED_BUSES, "km/h", "Average Speed Buses", "Mean");
		dataTypeDtoList.add(averageSpeedBuses);
		DataTypeDto varianceSpeedLight = new DataTypeDto(VARIANCE_SPEED_LIGHT_VEHICLES, "km/h",
				"Variance Speed Light Vehicles", "Mean");
		dataTypeDtoList.add(varianceSpeedLight);
		DataTypeDto varianceSpeedHeavy = new DataTypeDto(VARIANCE_SPEED_HEAVY_VEHICLES, "km/h",
				"Variance Speed Heavy Vehicles", "Mean");
		dataTypeDtoList.add(varianceSpeedHeavy);
		DataTypeDto varianceSpeedBuses = new DataTypeDto(VARIANCE_SPEED_BUSES, "km/h", "Variance Speed Buses", "Mean");
		dataTypeDtoList.add(varianceSpeedBuses);
		DataTypeDto averageGap = new DataTypeDto(AVERAGE_GAP, "s", "Average Gap", "Mean");
		dataTypeDtoList.add(averageGap);
		DataTypeDto averageHeadway = new DataTypeDto(AVERAGE_HEADWAY, "s", "Average Headway", "Mean");
		dataTypeDtoList.add(averageHeadway);
		DataTypeDto averageDensity = new DataTypeDto(AVERAGE_DENSITY, "vehicles / km", "Average Density", "Mean");
		dataTypeDtoList.add(averageDensity);
		DataTypeDto averageFlow = new DataTypeDto(AVERAGE_FLOW, "vehicles / hour", "Average Flow", "Mean");
		dataTypeDtoList.add(averageFlow);

		DataTypeDto europct = new DataTypeDto(EURO_CATEGOY_PCT, "%", "Euro emission standards distribution", "Mean");
		dataTypeDtoList.add(europct);

		DataTypeDto natCount = new DataTypeDto(PLATE_NATIONALITY_COUNT, "", "Vehicle Count by License Plate Nationality", "Count");
		dataTypeDtoList.add(natCount);

		pusher.syncDataTypes(dataTypeDtoList);

	}

	/*
	 * Method used only for development/debugging
	 */
	public static void main(String[] args)
	{
		LOG.info("Start MainSaveDatatype");
		//saveDatatypes();
	}

}
