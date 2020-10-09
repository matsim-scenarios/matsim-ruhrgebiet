package org.matsim.ruhrgebiet.analysis;

import com.beust.jcommander.Parameter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.*;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.collections.Tuple;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateSingleAirPollutionValue {

	private final Map<Pollutant, Double> pollution = new HashMap<>();

	@Parameter(names = {"-events", "-e"}, required = true)
	private List<String> eventsFiles = new ArrayList<>();

	@Parameter(names = {"-output", "-o"}, required = true)
	private String outputFile = "";

	public GenerateSingleAirPollutionValue(List<String> inputs, String output) {
		this.eventsFiles = inputs;
		this.outputFile = output;
	}

	/**
	 * Run the script with command line args
	 *
	 * @param args e.g. -e /path/to/your/emission/events-file.xml.gz -e /path/to/your/other/emission/events-file.xml.gz -o /path/to/your/output/file.csv
	 */
/*	public static void main(String[] args) throws IOException {

		var analysis = new GenerateSingleAirPollutionValue();
		JCommander.newBuilder().addObject(analysis).build().parse(args);
		analysis.run();
	}
*/
	public static void main(String[] args) throws IOException {

		var eventFiles = List.of(
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-10pct\\deurbanisation-10pct-matches.emission.events.offline.xml.gz",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-20pct\\deurbanisation-20pct-matches.emission.events.offline.xml.gz",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-30pct\\deurbanisation-30pct-matches.emission.events.offline.xml.gz",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-40pct\\deurbanisation-40pct-matches.emission.events.offline.xml.gz",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-50pct\\deurbanisation-50pct-matches.emission.events.offline.xml.gz",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-60pct\\deurbanisation-60pct-matches.emission.events.offline.xml.gz",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-70pct\\deurbanisation-70pct-matches.emission.events.offline.xml.gz",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-80pct\\deurbanisation-80pct-matches.emission.events.offline.xml.gz",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-90pct\\deurbanisation-90pct-matches.emission.events.offline.xml.gz",
				"C:\\Users\\Janekdererste\\Desktop\\deurb\\output-base\\baseCase.emission.events.offline.xml.gz"
		);

		new GenerateSingleAirPollutionValue(eventFiles, "C:\\Users\\Janekdererste\\Desktop\\deurb\\total-emissions.csv").run();
	}

	public void run() throws IOException {

		var pollutionPerRun = eventsFiles.parallelStream()
				.map(file -> {
					Map<Pollutant, Double> pollution = new TreeMap<>();
					var manager = EventsUtils.createEventsManager();
					manager.addHandler(new Handler(pollution));
					var reader = new EmissionEventsReader(manager);
					manager.initProcessing();
					reader.readFile(file);
					manager.finishProcessing();
					return Tuple.of(file, pollution);
				})
				.peek(tuple -> {
					// some debugging output
					for (var p : tuple.getSecond().entrySet()) {
						System.out.println(extractFilename(tuple.getFirst()) + ": " + p.getKey() + ": \t\t" + p.getValue());
					}
				})
				.collect(Collectors.toList());

		// create the header out of the pollution map of the first run. Assuming, all runs emitted the same set of pollutants
		var csvHeader = pollutionPerRun.get(0).getSecond().keySet().stream()
				.map(pollutant -> pollutant.toString())
				.collect(Collectors.toList());

		csvHeader.add(0, "Run");


		try (Writer writer = Files.newBufferedWriter(Paths.get(outputFile))) {
			try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
				printer.printRecord(csvHeader);

				for (var tuple : pollutionPerRun) {

					// this most probably only works if the filename is the standard offline emissions events file
					var filename = extractFilename(tuple.getFirst());
					var runId = filename.substring(0, filename.indexOf('.'));
					printer.print(runId);

					for (Double value : tuple.getSecond().values()) {
						printer.print(value);
					}
					printer.println();
				}
				printer.flush();
			}
		}
	}

	private String extractFilename(String filePath) {

		var index = filePath.lastIndexOf("/");
		if (index < 0) {
			index = filePath.lastIndexOf("\\");
		}
		return index >= 0 ? filePath.substring(index + 1) : filePath;
	}


	private static class Handler implements ColdEmissionEventHandler, WarmEmissionEventHandler {

		private final Map<Pollutant, Double> pollution;

		private Handler(Map<Pollutant, Double> pollution) {
			this.pollution = pollution;
		}

		@Override
		public void handleEvent(ColdEmissionEvent event) {

			for (Map.Entry<Pollutant, Double> pollutant : event.getColdEmissions().entrySet()) {
				pollution.merge(pollutant.getKey(), pollutant.getValue(), Double::sum);
			}
		}

		@Override
		public void handleEvent(WarmEmissionEvent event) {

			for (Map.Entry<Pollutant, Double> pollutant : event.getWarmEmissions().entrySet()) {
				pollution.merge(pollutant.getKey(), pollutant.getValue(), Double::sum);
			}
		}
	}
}
