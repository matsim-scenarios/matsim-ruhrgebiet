package org.matsim.ruhrgebiet.prepare.accidents;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class CreatePlanFreeLinkCSV {
	
	public static void main(String[] args) {
		
	   Network network = NetworkUtils.readNetwork("D:\\SVN-public\\matsim\\scenarios\\countries\\de\\ruhrgebiet\\ruhrgebiet-v1.1-1pct\\input\\ruhrgebiet-v1.1-with-RSV.network.xml.gz");
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		Set<Id<Link>> Ids = links.keySet();
		Set<String> types = new HashSet<String>();
		List<String> Planfreelinks = new ArrayList<String>();
		for (Id<Link> id: Ids) {
			if (network.getLinks().get(id).getAttributes().getAsMap().containsKey("type"))
		 {
			String type = network.getLinks().get(id).getAttributes().getAttribute("type").toString();
			if(type.equals("motorway")||type.equals("motorway_link")) {
				Planfreelinks.add(id.toString());
			}
		if(types.contains(type));
		else types.add(type);
		 }
		}
		WriteCsvBasedonList(Planfreelinks, "D:\\SVN-public\\matsim\\scenarios\\countries\\de\\ruhrgebiet\\ruhrgebiet-v1.1-1pct\\input\\PlanfreeLinks.csv");
		System.out.println(types);
	}
	
	
 public static void WriteCsvBasedonList(List<String> list, String pfad) {
//	 File csvFile = new File(pfad);
//	 if (csvFile.isFile()) {
//	     // create BufferedReader and read data from csv
//	 }
//	 try {
//		csvFile.createNewFile();
//	} catch (IOException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//	}
			FileWriter csvWriter;
			try {
				csvWriter = new FileWriter(pfad);
			for (String rowData : list) {
					csvWriter.write(rowData);
					csvWriter.append("\n");
				}

			csvWriter.flush();
			csvWriter.close();
			}
			catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	 
 }
}
