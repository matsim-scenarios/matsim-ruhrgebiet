/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup.FacilitiesSource;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesFromPopulation;
import org.matsim.ruhrgebiet.run.RunRuhrgebietScenario;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author dziemke, ikaddoura
 */
public class RunAccessibilityComputationRuhr {
	private static final Logger log = Logger.getLogger( RunAccessibilityComputationRuhr.class ) ;

	private final Envelope envelope = new Envelope(310200, 430700, 5676900, 5742200); // Ruhrgebiet
//	private final Envelope envelope = new Envelope(353400, 370700, 5690500, 5710700); // Essen
	private final List<String> consideredActivityTypePrefixes = Arrays.asList("work", "other", "education", "leisure");

	public static void main(String[] args) {
		String outputDirectory;
		String runId;	
		int tileSize_m;
		boolean downsample;
		
		if (args.length > 0) {
			outputDirectory = args[0];
			log.info("outputDirectory: " + outputDirectory);
			runId = args[1];
			log.info("runId: " + runId);
			tileSize_m = Integer.parseInt(args[2]);
			log.info("tileSize_m: " + tileSize_m);
			downsample = false;
		} else {
//			outputDirectory = "../../runs-svn/nemo/wissenschaftsforum2019_simulationsbasierteZukunftsforschung/run0_bc-ohne-RSV/";
//			runId = "run0_bc-ohne-RSV";
			outputDirectory = "../../runs-svn/nemo/wissenschaftsforum2019_simulationsbasierteZukunftsforschung/run3_gesundeStadt-mit-RSV/";
			runId = "run3_gesundeStadt-mit-RSV";
			tileSize_m = 1000;
			downsample = true;
		}
				
		RunAccessibilityComputationRuhr accessibilitiesRuhr = new RunAccessibilityComputationRuhr();
		accessibilitiesRuhr.run(outputDirectory, runId, tileSize_m, downsample);
	}

	private void run(String outputDirectory, String runId, int tileSize_m, boolean downsampling) {
		String dirSubString = "";
		if (downsampling) {
			dirSubString = "downsampling=" + downsampling + "_";
		}
		final String accessibilityOutputFolder = "accessibility_" + dirSubString + "tileSize=" + tileSize_m + "/";
		if (!outputDirectory.endsWith("/")) outputDirectory = outputDirectory + "/";

		//Config config = RunRuhrgebietScenario.prepareConfig(new String[]{outputDirectory + runId + ".output_config_adjusted-for-accessibility-computation-ik.xml"});
		var config = RunRuhrgebietScenario.loadConfig(new String[]{outputDirectory + runId + ".output_config.xml"}, new DvrpConfigGroup(),
				new MultiModeDrtConfigGroup(), new DrtFaresConfigGroup());
		config.facilities().setFacilitiesSource(FacilitiesSource.setInScenario);
		config.plans().setInputFile(runId + ".output_plans.xml.gz");
		config.network().setInputFile(runId + ".output_network.xml.gz");
		config.transit().setTransitScheduleFile(runId + ".output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile(runId + ".output_transitVehicles.xml.gz");
		config.vehicles().setVehiclesFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1.mode-vehicles.xml");
		config.controler().setFirstIteration(config.controler().getLastIteration());
		config.controler().setLastIteration(config.controler().getLastIteration());
		config.controler().setOutputDirectory(outputDirectory + accessibilityOutputFolder);

		//config.plansCalcRoute().getTeleportedModeSpeeds().put(TransportMode.bike, 3.53); // just take some value since accessibility computation expects bike to be teleported.

		//String eventsFilename = outputDirectory + "run3_gesundeStadt-mit-RSV.output_events.xml.gz";

		// required by accessiblity computation
		config.plansCalcRoute().setRoutingRandomness(0.);

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setTileSize_m(tileSize_m);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setEnvelope(envelope);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
		// Modes other than freespeed are set to false by default
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, false);

		BicycleConfigGroup bcg = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
		bcg.setBicycleMode(TransportMode.bike);
		bcg.setMaxBicycleSpeedForRouting(6.82); // Was 6.84 before. Need to set it to 6.84 to make consistent with corresponding mobsim speed

		Scenario scenario = ScenarioUtils.loadScenario(config);//RunRuhrgebietScenario.prepareScenario(config);

		// bicycle contrib requires the attribute "bicycleInfrastructureSpeedFactor" instead of "bike_speed_factor"
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAttributes().getAttribute("bike_speed_factor") != null) {
				link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, link.getAttributes().getAttribute("bike_speed_factor"));
			}
		}

		// down-sampling the scenario
		if (downsampling) {
			final double sample = 0.1;
			downsample( scenario.getPopulation().getPersons(), sample ) ;
			config.qsim().setFlowCapFactor( config.qsim().getFlowCapFactor() * sample );
			config.qsim().setStorageCapFactor( config.qsim().getStorageCapFactor() * sample );
		}

		StringBuilder activityConsideredForAccessibilityComputation = new StringBuilder();
		for (String consideredActivityPrefix : consideredActivityTypePrefixes) {
			if (activityConsideredForAccessibilityComputation.length() > 0)
				activityConsideredForAccessibilityComputation.append("_");
			activityConsideredForAccessibilityComputation.append(consideredActivityPrefix).append("*");
		}
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pE : plan.getPlanElements()) {
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						for (String consideredActivityTypePrefix : consideredActivityTypePrefixes) {
							if (act.getType().startsWith(consideredActivityTypePrefix)) {
								act.setType(activityConsideredForAccessibilityComputation.toString());
							}
						}
					}
				}
			}
		}

		ActivityParams actParams = new ActivityParams(activityConsideredForAccessibilityComputation.toString());
		actParams.setTypicalDuration(8 * 3600.); // shouldn't have any effect in the accessibility computation
		config.planCalcScore().addActivityParams(actParams);
		
		// now generate the facilities from the population and add them to the scenario
		{
			Scenario scenarioToGenerateFacilities = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			scenarioToGenerateFacilities.getConfig().facilities().setFacilitiesSource(FacilitiesSource.onePerActivityLocationInPlansFile);
			FacilitiesFromPopulation facilitiesFromPopulation = new FacilitiesFromPopulation(scenarioToGenerateFacilities);
			facilitiesFromPopulation.setAssignLinksToFacilitiesIfMissing(scenario.getNetwork());
			facilitiesFromPopulation.run(scenario.getPopulation());			
			for (ActivityFacility facility : scenarioToGenerateFacilities.getActivityFacilities().getFacilities().values()) {
				scenario.getActivityFacilities().addActivityFacility(facility);
			}
		}

		// yyyy In principle, should now be possible to replace the controler lines by something like
//		AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder( scenario , eventsFilename );
//		builder.build().run() ;
		// Current shortcomings:
		// * It does not work with pt, in part because you cannot maven-use the swiss rail raptor from a contrib.   Hopefully,
		// this will change in the near future.
		// * I have not implemented the network filtering that seems to be in the code above.  Not sure why that has to be here and not in the
		// accessibility contrib.
		// kai, sep'19
		// The network filtering here was just a quick fix to get things running before the dev mtg. the network filtering is now in the contrib. dz, sept'19

		org.matsim.core.controler.Controler controler = RunRuhrgebietScenario.prepareControler(scenario);

		/*var module = new AccessibilityModuleRuhr();
		module.setConsideredActivityType(activityConsideredForAccessibilityComputation.toString());
		controler.addOverridingModule(module);
*/
		/*
		AccessibilityModuleRuhr module = new AccessibilityModuleRuhr();
		module.setConsideredActivityType(activityConsideredForAccessibilityComputation.toString());
		controler.addOverridingModule(module);
*/
		controler.run();

		// QGis
		boolean createQGisOutput = true;
		if (createQGisOutput) {
			final boolean includeDensityLayer = false;
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = -3.5; // (upperBound - lowerBound) ideally nicely divisible by (range - 2) // TODO
			final Double upperBound = 3.5;
			final int populationThreshold = (int) (0 / (1000/(double) tileSize_m * 1000/(double) tileSize_m)); // TODO

			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			
			String actSpecificWorkingDirectory = workingDirectory + activityConsideredForAccessibilityComputation + "/";
			for (Modes4Accessibility mode : acg.getIsComputingMode()) {
				VisualizationUtils.createQGisOutputGraduatedStandardColorRange(activityConsideredForAccessibilityComputation.toString(), mode.toString(), envelope, workingDirectory,
						config.global().getCoordinateSystem(), includeDensityLayer, lowerBound, upperBound, range, tileSize_m, populationThreshold);
				VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
			}
		}
	}

	private static void downsample( final Map<Id<Person>, ? extends Person> map, final double sample ) {
		final Random rnd = MatsimRandom.getLocalInstance();
		log.warn( "map size before=" + map.size() ) ;
		map.values().removeIf( person -> rnd.nextDouble()>sample ) ;
		log.warn( "map size after=" + map.size() ) ;
	}
}