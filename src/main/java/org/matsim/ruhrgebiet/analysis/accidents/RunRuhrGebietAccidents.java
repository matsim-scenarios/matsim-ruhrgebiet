package org.matsim.ruhrgebiet.analysis.accidents;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.runExample.AccidentsNetworkModification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.ruhrgebiet.prepare.accidents.WriteBVWPAccidentRoadTypesIntoLinkAttributes;
import org.matsim.ruhrgebiet.run.RunRuhrgebietScenario;

public class RunRuhrGebietAccidents {

	private static final Logger log = Logger.getLogger(WriteBVWPAccidentRoadTypesIntoLinkAttributes.class);
	
	public static void main(String[] args) throws IOException {
		
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
 			args = new String[] {"scenarios/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1-1pct.config.xml"}  ;
		}
		
		Config config = RunRuhrgebietScenario.loadConfig(args);
		AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
     	config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1.network-with-bvwp-accidents-attributes.xml.gz");
     	
		Scenario scenario = RunRuhrgebietScenario.loadScenario(config);
		accidentsSettings.setEnableAccidentsModule(true);
		accidentsSettings.setScaleFactor(100);
		
		Controler controler = RunRuhrgebietScenario.prepareControler(scenario);
		controler.run();

	}
}
