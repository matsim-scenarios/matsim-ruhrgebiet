package org.matsim.ruhrgebiet.prepare.accidents;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
		
		Config config = RunRuhrgebietScenario.loadConfig(args);
		AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
     	config.plans().setInputFile(null);
     	config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1-with-RSV.network.xml.gz");
     	
		Scenario scenario = RunRuhrgebietScenario.loadScenario(config);
		AccidentsNetworkModification accidentsNetworkModification = new AccidentsNetworkModification(scenario);
		String landOSMInputShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.1-1pct/original_data/OSM_RuhrGebiet/gis_osm_landuse_a_free_1.shp";
		String tunnelLinkCSVInputFile = null;
		String planfreeLinkCSVInputFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.1-1pct/input/ruhrgebiet-v1.1.PlanfreeLinks.csv";
		String outputFile = "ruhrgebiet-v1.1.network-with-bvwp-accidents-attributes.xml.gz";
		NetworkUtils.writeNetwork(accidentsNetworkModification.setLinkAttributsBasedOnOSMFile(
				landOSMInputShapeFile ,
				"EPSG:31468",
				readColumn(0,tunnelLinkCSVInputFile,";"),
				readColumn(0,planfreeLinkCSVInputFile, ";")
				),
				outputFile);

	}

	public static String[] readColumn(int numCol, String csvFile, String separator) throws IOException {
		
		StringBuilder sb = new StringBuilder();
		String line;

		URL url = new URL(csvFile);
		BufferedReader br = new BufferedReader(
		        new InputStreamReader(url.openStream()));
		
		// read line by line
        try {
			while ((line = br.readLine()) != null) 
			{
				String value = "ERROR";
				String list[] = line.split(separator);
				if(numCol<list.length) {
					value = list[numCol];
				}
				
			    sb.append(value).append(separator);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        return sb.toString().split(separator);
	}
}
