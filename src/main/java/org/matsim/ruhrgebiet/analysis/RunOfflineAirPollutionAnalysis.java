/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.ruhrgebiet.analysis;

import com.beust.jcommander.Parameter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.List;

/**
 * @author ikaddoura
 */

public class RunOfflineAirPollutionAnalysis {

	//private final static String runDirectory = "public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.0-1pct/output-ruhrgebiet-v1.0-1pct/";
	//private final static String runId = "ruhrgebiet-v1.0-1pct";

	//private final static String runDirectory = "C:\\Users\\Janek\\repos\\runs-svn\\nemo\\wissenschaftsforum2019_simulationsbasierteZukunftsforschung\\run3_gesundeStadt-mit-RSV\\";
	//private final static String runId = "run3_gesundeStadt-mit-RSV";

	//private final static String hbefaFileCold = "C:/Users/Janek/repos/shared-svn/projects/detailedEval/emissions/hbefaForMatsim/new/EFA_ColdStart_vehcat_2005average.txt";
	//private final static String hbefaFileWarm = "C:/Users/Janek/repos/shared-svn/projects/detailedEval/emissions/hbefaForMatsim/new/EFA_HOT_vehcat_2005average.txt";

	@Parameter(names = {"-sharedSvn", "-svn"}, required = true)
	private String sharedSvn = "";

	@Parameter(names = {"-runId", "-rId"}, required = true)
	private String runId = "";

	@Parameter(names = {"-runDir", "-dir"}, required = true)
	private String runDirectory = "";

	RunOfflineAirPollutionAnalysis(String baseDirectory, String runId, String sharedSvn) {

		this.runDirectory = baseDirectory;
		this.runId = runId;
		this.sharedSvn = sharedSvn;

	}

	/**
	 * Run this to genereate emission events
	 *
	 * @param args Command line args to configure the script. works like: -svn /path/to/your/shared-svn/root/ -rId your-run-id -dir /path/to/results/directory/
	 */
	/*public static void main(String[] args) {

		var analysis = new RunOfflineAirPollutionAnalysis();
		JCommander.newBuilder().addObject(analysis).build().parse(args);
		analysis.run();
	}

	 */
	public static void main(String[] args) {

		var dirs = List.of(
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-10pct",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-20pct",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-30pct",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-40pct",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-50pct",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-60pct",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-70pct",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-80pct",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-90pct",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-base"
		);

		var runIds = List.of(
				"deurbanisation-10pct-matches",
				"deurbanisation-20pct-matches",
				"deurbanisation-30pct-matches",
				"deurbanisation-40pct-matches",
				"deurbanisation-50pct-matches",
				"deurbanisation-60pct-matches",
				"deurbanisation-70pct-matches",
				"deurbanisation-80pct-matches",
				"deurbanisation-90pct-matches",
				"baseCase"
		);

		var sharedSvn = "C:\\Users\\Janekdererste\\repos\\shared-svn";


		for (int i = 0; i < dirs.size(); i++) {

			var dir = dirs.get(i);
			var id = runIds.get(i);

			new RunOfflineAirPollutionAnalysis(dir, id, sharedSvn).run();
		}
	}

	private void run() {

		runDirectory = (runDirectory.endsWith("/") || runDirectory.endsWith("\\")) ? runDirectory : runDirectory + "/";
		sharedSvn = (sharedSvn.endsWith("/") || sharedSvn.endsWith("\\")) ? sharedSvn : sharedSvn + "/";

		final String hbefaFileCold = sharedSvn + "projects/matsim-germany/hbefa/hbefa-files/v3.2/EFA_ColdStart_vehcat_2005average.txt";
		final String hbefaFileWarm = sharedSvn + "projects/matsim-germany/hbefa/hbefa-files/v3.2/EFA_HOT_vehcat_2005average.txt";

		Logger.getRootLogger().setLevel(Level.WARN);

		Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml");
		config.vehicles().setVehiclesFile(runDirectory + runId + ".output_vehicles.xml.gz");
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");

		config.plans().setInputFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);

		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setAverageColdEmissionFactorsFile(hbefaFileCold);
		eConfig.setAverageWarmEmissionFactorsFile(hbefaFileWarm);
		eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable);
		eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
		eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);



		Scenario scenario = ScenarioUtils.loadScenario(config);

		// network
		for (Link link : scenario.getNetwork().getLinks().values()) {

			double freespeed;

			if (link.getFreespeed() <= 13.888889) {
				freespeed = link.getFreespeed() * 2;
				// for non motorway roads, the free speed level was reduced.  Note that this speed increase is only done for the emissions,
				// so that we obtain correct lookups.
			} else {
				freespeed = link.getFreespeed();
				// for motorways, the original speed levels seems ok.
			}

			if(freespeed <= 8.333333333){ //30kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
			} else if(freespeed <= 11.111111111){ //40kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
			} else if(freespeed <= 13.888888889){ //50kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 16.666666667){ //60kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 19.444444444){ //70kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
			} else if(freespeed <= 22.222222222){ //80kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
			} else if(freespeed > 22.222222222){ //faster
				link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
			} else {
				throw new RuntimeException("Link not considered...");
			}
		}

		Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
		Id<VehicleType> bikeVehicleTypeId = Id.create("bike", VehicleType.class);

		// vehicles
		var carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
		VehicleUtils.setHbefaVehicleCategory(carVehicleType.getEngineInformation(), HbefaVehicleCategory.PASSENGER_CAR.toString());
		var bikeVehicleType = scenario.getVehicles().getVehicleTypes().get(bikeVehicleTypeId);
		VehicleUtils.setHbefaVehicleCategory(bikeVehicleType.getEngineInformation(), HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());

		// the following is copy paste from the example...

		EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule() {
			@Override
			public void install() {
				bind(Scenario.class).toInstance(scenario);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(EmissionModule.class);
			}
		};

		final String emissionEventOutputFile = runDirectory + runId + ".emission.events.offline.xml.gz";
		final String eventsFile = runDirectory + runId + ".output_events.xml.gz";

		com.google.inject.Injector injector = Injector.createInjector(config, module);

		EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

		EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		emissionEventWriter.closeFile();
	}

}

