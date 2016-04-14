package info.krumholz.tcgservice;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan
@EnableAutoConfiguration
public class Main {

	public static boolean ssl;

	public static void main(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption("p", "port", true, "The port for the service");
		options.addOption("s", "ssl", true, "Ssl enabled");
		CommandLineParser parser = new DefaultParser();
		CommandLine command = parser.parse(options, args);

		Map<String, Object> defaultProperties = new HashMap<String, Object>();
		SpringApplication app = new SpringApplication(Main.class);
		if (command.hasOption("p")) {
			defaultProperties.put("server.port", Integer.parseInt(command.getOptionValue("p")));
			System.out.println("Port:" + command.getOptionValue("p"));
		} else {
			defaultProperties.put("server.port", 80);
			System.out.println("Port: 80 (default)");
		}
		if (command.hasOption("s")) {
			ssl = true;
			System.out.println("using ssl");
		} else {
			ssl = false;
			System.out.println("using NO ssl");
		}

		app.setDefaultProperties(defaultProperties);
		app.run(args);
	}
}
