/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.FastEmissionGridAnalyzer;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author amit, ihab
 */

public class GenerateAirPollutionSpatialPlots {
	private static final Logger log = Logger.getLogger(GenerateAirPollutionSpatialPlots.class);

	private static final double xMin = 317373;
	private static final double yMin = 5675521.;
	private static final double xMax = 418575.;
	private static final double yMax = 5736671.;

	private static final double gridSize = 100.;
	private static final double smoothingRadius = 500.;
	private static final double scaleFactor = 100.;

	@Parameter(names = {"-dir"}, required = true)
	private final String runDir = "";

	@Parameter(names = {"-runId"}, required = true)
	private final String runId = "";

	@Parameter(names = {"-outDir"})
	private final String outDir = "";

	private GenerateAirPollutionSpatialPlots() {

	}

	public static void main(String[] args) {

		GenerateAirPollutionSpatialPlots plots = new GenerateAirPollutionSpatialPlots();

		JCommander.newBuilder().addObject(plots).build().parse(args);

		plots.writeEmissions();
	}

	private void writeEmissions() {

		var runDirPath = Paths.get(runDir);
		final String events = runDirPath.resolve(runId + ".emission.events.offline.xml.gz").toString();
		final String networkFile = runDirPath.resolve(runId + ".output_network.xml.gz").toString();
		final String outputDir = StringUtils.isBlank(outDir) ? runDir : outDir;
		final String outputFile = Paths.get(outputDir).resolve(runId + ".emissionsgrid.csv").toString();

		var boundingBox = createBoundingBox();

		var filteredNetwork = NetworkUtils.readNetwork(networkFile).getLinks().values().parallelStream()
				.filter(link -> boundingBox.covers(MGC.coord2Point(link.getFromNode().getCoord())) || boundingBox.covers(MGC.coord2Point(link.getToNode().getCoord())))
				.collect(NetworkUtils.getCollector());

		var rasterMap = FastEmissionGridAnalyzer.processEventsFile(events, filteredNetwork, gridSize, 20);

		//write NOx
		var noxRaster = rasterMap.get(Pollutant.NOx);

		try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.TDF)) {

			// print header
			printer.printRecord("x", "y", "NOx");

			// print pollution per cell
			noxRaster.forEachCoordinate((x, y, value) -> {

				if (value < 0.1) return; // don't write values smaller than 0.1g/ha

				try {
					printer.printRecord(x, y, value * 100); // times 100 because of 1pct sample
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Geometry createBoundingBox() {
		return new GeometryFactory().createPolygon(new Coordinate[]{
				new Coordinate(xMin, yMin), new Coordinate(xMax, yMin),
				new Coordinate(xMax, yMax), new Coordinate(xMin, yMax),
				new Coordinate(xMin, yMin)
		});
	}
}
