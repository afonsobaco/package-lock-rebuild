package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DemoApplication {
	public static void main(String[] args) throws IOException, URISyntaxException {
		DemoApplication demo = new DemoApplication();
		demo.process(demo.convert(demo.getFileFromResource("package-json.json")));
	}

	private void process(Application application) {
		List<Dependency> dependencies = convertToDependency(application.getDependencies());
		dependencies = cleanRequires(dependencies);
		List<Dependency> dep = dependencies.stream().filter(d -> !d.isDev()).collect(Collectors.toList());
		List<Dependency> dev = dependencies.stream().filter(d -> d.isDev()).collect(Collectors.toList());
		print(dep, dev);
	}

	private Application convert(File file) throws JsonParseException,
	        JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		// JSON from file to Object
		Application dependency = mapper.readValue(file, Application.class);

		return dependency;

	}

	public List<Dependency> convertToDependency(Map<String, DeserializedDependency> map) {
		if (map == null) {
			return null;
		}
		List<Dependency> dependencies = new ArrayList<>();
		for (String name : map.keySet()) {
			DeserializedDependency deserialized = map.get(name);
			deserialized.setName(name);
			List<Dependency> requires = new ArrayList<>();
			if (deserialized.getRequires() != null) {

				for (String key : deserialized.getRequires().keySet()) {
					Dependency required = Dependency.builder()
					        .name(key)
					        .version(deserialized.getRequires().get(key))
					        .build();
					requires.add(required);
				}
				if (deserialized.getDependencies() != null) {
					var reqDeps = convertToDependency(deserialized.getDependencies());
					requires.addAll(
					        reqDeps.stream().flatMap(d -> d.getRequires().stream()).collect(Collectors.toList()));
				}
			}
			var dependency = Dependency.builder()
			        .name(name)
			        .version(deserialized.getVersion())
			        .resolved(deserialized.getResolved())
			        .integrity(deserialized.getIntegrity())
			        .dev(deserialized.isDev())
			        .nested(deserialized.isNested())
			        .optional(deserialized.isOptional())
			        .bundled(deserialized.isBundled())
			        .requires(requires)
			        .build();
			dependencies.add(dependency);

		}
		return dependencies;
	}

	public File getFileFromResource(String fileName) throws URISyntaxException {
		ClassLoader classLoader = getClass().getClassLoader();
		URL resource = classLoader.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("file not found! " + fileName);
		} else {
			return new File(resource.toURI());
		}
	}

	public List<Dependency> cleanRequires(List<Dependency> dependencies) {
		List<Dependency> result = new ArrayList<>();
		for (Dependency dependency : dependencies) {
			List<Dependency> allThatRequires = isRequired(dependency, dependencies);
			if (allThatRequires.isEmpty()) {
				result.add(dependency);
			} else {
				List<Dependency> filter = allThatRequires.stream().filter(r -> !r.isDev()).collect(Collectors.toList());
				List<Dependency> filterDev = allThatRequires.stream().filter(r -> r.isDev())
				        .collect(Collectors.toList());
				if (filter.size() == 0) {
					if (requiresAdder(dependency, filterDev, !dependency.isDev())) {
						result.add(dependency);
					}
				}
				if (filterDev.size() == 0) {
					if (requiresAdder(dependency, filter, dependency.isDev())) {
						result.add(dependency);
					}
				}
			}
		}
		return result;
	}

	private boolean requiresAdder(Dependency dependency, List<Dependency> filterDev,
	        boolean isdev) {
		if (filterDev.size() > 0) {
			if (isdev) {
				return true;
			} else {
				return shouldAddByVersion(dependency, filterDev);
			}
		}
		return false;
	}

	private boolean shouldAddByVersion(Dependency dependency, List<Dependency> filteredDeps) {
		var filter = filteredDeps.stream().map(d -> d.getRequires()).flatMap(Collection::stream)
		        .filter(f -> ((Dependency) f).getName().equals(dependency.getName())).collect(Collectors.toList());

		boolean isCompatible = false;
		// boolean isGreatest = true;
		String greatestCompatible = "";
		for (Dependency dep : filter) {
			if (dep.getVersion().startsWith("^") || dep.getVersion().startsWith("~")) {
				isCompatible = true;
			}
			if (greatestCompatible.equals("") || versionReader(dep.getVersion(), greatestCompatible) < 0) {
				greatestCompatible = dep.getVersion();
			}
			// if (versionReader(dep.getVersion(), dependency.getVersion()) < 0) {
			// isGreatest = false;
			// }
		}

		if (!isCompatible) {
			return !dependency.getVersion().equals(greatestCompatible);
		}

		return !(majorVersionCompatible(greatestCompatible, dependency.getVersion())
		        || minorVersionCompatible(greatestCompatible, dependency.getVersion()));

	}

	private void print(List<Dependency> dep, List<Dependency> dev) {

		System.out.println("\"dependencies\": {");
		dep.forEach(d -> {
			System.out.println(String.format("\"%s\": \"%s\",", d.getName(),
			        d.getVersion()));
		});
		System.out.println("},");
		System.out.println("\"devDependencies\": {");
		dev.forEach(d -> {
			System.out.println(String.format("\"%s\": \"%s\",", d.getName(),
			        d.getVersion()));
		});
		System.out.println("}");
	}

	private List<Dependency> isRequired(Dependency dependency, List<Dependency> dependencies) {
		List<Dependency> allThatRequires = new ArrayList<>();
		for (Dependency parent : dependencies) {
			if (parent.getRequires() != null) {
				var required = parent.getRequires().stream().filter(r -> r.getName().equals(dependency.getName()))
				        .findAny();
				if (required.isPresent()) {
					allThatRequires.add(parent);
				}
			}
		}
		return allThatRequires;
	}

	private int versionReader(String v1, String v2) {
		String[] p1, p2;
		p1 = getVersionParts(v1);
		p2 = getVersionParts(v2);
		int count = p1.length >= p2.length ? p2.length : p1.length;
		for (int i = 0; i < count; i++) {
			try {
				if (Integer.parseInt(p1[i]) > Integer.parseInt(p2[i])) {
					return -1;
				}
				if (Integer.parseInt(p1[i]) < Integer.parseInt(p2[i])) {
					return 1;
				}
			} catch (NumberFormatException e) {
			}
		}
		if (p1.length > p2.length) {
			return -1;
		}
		if (p1.length < p2.length) {
			return 1;
		}
		return 0;
	}

	public boolean majorVersionCompatible(String v1, String v2) {
		if (v1.replaceAll("[^\\d.]", "").equals(v2.replaceAll("[^\\d.]", ""))) {
			return true;
		}
		return checkCompatibility(v1, v2, 0);
	}

	public boolean minorVersionCompatible(String v1, String v2) {
		if (v1.replaceAll("[^\\d.]", "").equals(v2.replaceAll("[^\\d.]", ""))) {
			return true;
		}
		return checkCompatibility(v1, v2, 1);
	}

	private boolean checkCompatibility(String v1, String v2, int step) {
		String[] p1, p2;
		p1 = getVersionParts(v1);
		p2 = getVersionParts(v2);
		// "2.1.1"
		// "2.1.9"
		for (int i = 0; i <= step; i++) {
			try {
				var mv1 = Integer.parseInt(p1[step]);
				var mv2 = Integer.parseInt(p2[step]);
				if (mv1 != mv2) {
					return false;
				}
				if (i == step) {
					return true;
				}
			} catch (NumberFormatException e) {
			}
		}
		return false;
	}

	private String[] getVersionParts(String version) {
		String[] arr = version.replaceAll(" ", "").split("\\|\\|");
		return arr[arr.length - 1].replaceAll("[^\\d.]", "").split("\\.");
	}
}
