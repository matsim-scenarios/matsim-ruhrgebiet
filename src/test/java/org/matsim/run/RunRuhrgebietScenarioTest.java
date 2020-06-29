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
package org.matsim.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.ruhrgebiet.run.RunRuhrgebietScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.matsim.testcases.MatsimTestUtils.EPSILON;

/**
 * @author zmeng, ikaddoura
 */
public class RunRuhrgebietScenarioTest {
	private static final Logger log = Logger.getLogger(RunRuhrgebietScenarioTest.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private static void downsample(final Map<Id<Person>, ? extends Person> map, final double sample) {
		final Random rnd = MatsimRandom.getLocalInstance();
		log.warn("map size before=" + map.size());
		map.values().removeIf(person -> rnd.nextDouble() > sample);
		log.warn("map size after=" + map.size());
	}

	@Test
	public final void loadConfig() {

		String configFileName = "scenarios/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1-1pct.config.xml";

		Config config = RunRuhrgebietScenario.loadConfig(new String[]{configFileName});
		assertNotNull(config);

	}

	@Test
	public final void runScenarioOneIteration() {

		String configFileName = "scenarios/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1-1pct.config.xml";

		Config config = RunRuhrgebietScenario.loadConfig(new String[]{configFileName});
		config.controler().setWriteEventsInterval(1);
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWritePlansUntilIteration(0);
		config.controler().setWritePlansInterval(0);
		config.qsim().setNumberOfThreads(1);
		config.global().setNumberOfThreads(1);

		Scenario scenario = RunRuhrgebietScenario.loadScenario(config);
		final double sample = 0.01;
		downsample(scenario.getPopulation().getPersons(), sample);
		config.qsim().setFlowCapFactor(config.qsim().getFlowCapFactor() * sample);
		config.qsim().setStorageCapFactor(config.qsim().getStorageCapFactor() * sample);

		org.matsim.core.controler.Controler controler = RunRuhrgebietScenario.loadControler(scenario);
		controler.run();

		// modal split
		Map<String, Double> modestats = getModestats(utils.getOutputDirectory() + "ruhrgebiet-v1.1-1pct.modestats.txt");
		Assert.assertEquals(0.07893242475865986, modestats.get("bike"), EPSILON);
		Assert.assertEquals(0.3424190800681431, modestats.get("car"), EPSILON);
		Assert.assertEquals(0.25837592277115273, modestats.get("pt"), EPSILON);
		Assert.assertEquals(0.1254968767745599, modestats.get("walk"), EPSILON);
		Assert.assertEquals(0.1947756956274844, modestats.get("ride"), EPSILON);
	}

	@Ignore // TODO: Make this test fit into travis-ci.
	@Test
	public final void runScenario20Iterations() {

		String configFileName = "scenarios/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1-1pct.config.xml";

		Config config = RunRuhrgebietScenario.loadConfig(new String[]{configFileName});
		config.controler().setWriteEventsInterval(0);
		config.controler().setLastIteration(20);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWritePlansUntilIteration(0);
		config.controler().setWritePlansInterval(0);
		config.qsim().setNumberOfThreads(1);
		config.global().setNumberOfThreads(1);

		Scenario scenario = RunRuhrgebietScenario.loadScenario(config);
		final double sample = 0.01;
		downsample(scenario.getPopulation().getPersons(), sample);
		config.qsim().setFlowCapFactor(config.qsim().getFlowCapFactor() * sample);
		config.qsim().setStorageCapFactor(config.qsim().getStorageCapFactor() * sample);

		RunRuhrgebietScenario.loadControler(scenario).run();

		// modal split

		Map<String, Double> modestats = getModestats(utils.getOutputDirectory() + "ruhrgebiet-v1.1-1pct.modestats.txt");
		Assert.assertEquals(0.09121245828698554, modestats.get("bike"), 0.05);
		Assert.assertEquals(0.3770856507230256, modestats.get("car"), 0.05);
		Assert.assertEquals(0.29699666295884314, modestats.get("pt"), 0.05);
		Assert.assertEquals(0.06229143492769744, modestats.get("walk"), 0.05);
		Assert.assertEquals(0.1724137931034483, modestats.get("ride"), 0.05);

		// scores

		// TODO: add score test
//			Assert.assertEquals(xxx, ruhrgebietScenarioRunner.getScoreStats().getScoreHistory().get(ScoreStatsControlerListener.ScoreItem.average).get(20), EPSILON);
	}

	private Map<String, Double> getModestats(String modestats) {
		File inputFile = new File(modestats);
		Map<String, Double> getModestats = new HashMap<>();

		try (BufferedReader in = new BufferedReader(new FileReader(inputFile))) {
			String line = in.readLine();
			String[] modes = line.split("\t");
			String line2 = in.readLine();
			String[] modeSplit = line2.split("\t");

			for (int i = 1; i < modes.length; i++) {
				getModestats.put(modes[i], Double.valueOf(modeSplit[i]));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return getModestats;
	}
}

