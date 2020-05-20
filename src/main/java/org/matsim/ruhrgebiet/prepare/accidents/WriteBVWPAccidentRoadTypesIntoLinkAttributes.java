package org.matsim.ruhrgebiet.prepare.accidents;


import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.contrib.accidents.runExample.AccidentsNetworkModification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.ruhrgebiet.run.RunRuhrgebietScenario;

public class WriteBVWPAccidentRoadTypesIntoLinkAttributes {
	
	private static final Logger log = Logger.getLogger(WriteBVWPAccidentRoadTypesIntoLinkAttributes.class);
	
	public static void main(String[] args) throws IOException {
		
		for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
 			args = new String[] {"scenarios/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1-1pct.config.xml"}  ;
		}
		
//		Config config = RunRuhrgebietScenario.prepareConfig(args);
//		AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
//		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1-with-RSV.network.xml.gz");
//		config.plans().setInputFile(null);
//		
//		Scenario scenario = RunRuhrgebietScenario.prepareScenario(config);
	   Network network = NetworkUtils.readNetwork("D:\\SVN-public\\matsim\\scenarios\\countries\\de\\ruhrgebiet\\ruhrgebiet-v1.1-1pct\\input\\ruhrgebiet-v1.1-with-RSV.network.xml.gz");
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		Set<Id<Link>> Ids = links.keySet();
		Set<String> types = new HashSet<String>();
		for (Id<Link> id: Ids) {
			if (network.getLinks().get(id).getAttributes().getAsMap().containsKey("type"))
		 {
			String type = network.getLinks().get(id).getAttributes().getAttribute("type").toString();
		if(types.contains(type));
		else types.add(type);
		 }
		}
		System.out.println(types);
	}


}
