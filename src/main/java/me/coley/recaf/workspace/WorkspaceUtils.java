package me.coley.recaf.workspace;

import com.eclipsesource.json.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceUtils {
	/**
	 * @param json Json file.
	 * @return Workspace loaded from a json config.
	 * @throws Exception Thrown if the file could not be read or parsed.
	 */
	public static Workspace fromJson(File json) throws Exception {
		return fromJson(FileUtils.readFileToString(json, "UTF-8"));
	}

	/**
	 * @param json Json text.
	 * @return Workspace loaded from a json string.
	 * @throws Exception Thrown if the json could not be parsed.
	 */
	public static Workspace fromJson(String json) throws Exception {
		JsonObject root = Json.parse(json).asObject();
		JsonObject jprimary = root.get("primary").asObject();
		JavaResource primary = deserializeResource(jprimary);
		List<JavaResource> libraries = new ArrayList<>();
		if(root.names().contains("libraries")) {
			JsonArray jlibraries = root.get("libraries").asArray();
			for(JsonValue value : jlibraries) {
				libraries.add(deserializeResource(value.asObject()));
			}
		}
		return new Workspace(primary, libraries);
	}

	/**
	 * @param workspace Workspace instance.
	 * @return Json text representation of a workspace.
	 */
	public static String toJson(Workspace workspace) {
		JsonObject root = Json.object();
		JsonObject jprimary = serializeResource(workspace.getPrimary());
		JsonArray jlibraries = new JsonArray();
		for(JavaResource library : workspace.getLibraries()) {
			jlibraries.add(serializeResource(library));
		}
		root.add("primary", jprimary);
		root.add("libraries", jlibraries);
		return root.toString(WriterConfig.PRETTY_PRINT);
	}

	/**
	 * @param resource Resource reference.
	 * @return Json representation of the reference.
	 */
	private static JsonObject serializeResource(JavaResource resource) {
		JsonObject root = Json.object();
		// TODO: Relative file paths if possible (CLASS/JAR)
		switch(resource.getKind()) {
			case CLASS:
				File clazz = ((ClassResource) resource).getFile();
				root.add("kind", "class");
				root.add("source", clazz.getAbsolutePath());
				break;
			case JAR:
				File jar = ((JarResource) resource).getFile();
				root.add("kind", "jar");
				root.add("source", jar.getAbsolutePath());
				break;
			case MAVEN:
				MavenResource maven = (MavenResource) resource;
				root.add("kind", "maven");
				root.add("source", maven.getCoords());
				break;
			case URL:
				UrlResource url = (UrlResource) resource;
				root.add("kind", "url");
				root.add("source", url.getUrl().toString());
				break;
			case INSTRUMENTATION:
				root.add("kind", "instrumentation");
				root.add("source", "n/a");
				break;
		}
		if (resource.getSkippedPrefixes().size() > 0 ) {
			JsonArray skipped = Json.array(resource.getSkippedPrefixes().toArray(new String[0]));
			root.add("skipped", skipped);
		}
		return root;
	}

	/**
	 * @param jresource Json representation of a resource reference.
	 * @return Resource reference.
	 * @throws IllegalArgumentException Thrown if the json object was malformed or if instantiation of the resource failed.
	 */
	private static JavaResource deserializeResource(JsonObject jresource) throws IllegalArgumentException {
		String kind = jresource.getString("kind", null);
		if (kind == null)
			throw new IllegalArgumentException("Invalid resource, kind not specified!");
		String source = jresource.getString("source", null);
		if (source == null)
			throw new IllegalArgumentException("Invalid resource, source not specified!");
		JavaResource resource = null;
		switch(kind) {
			case "class":
				File clazz = new File(source);
				if (!clazz.exists())
					throw new IllegalArgumentException("Invalid resource, file does not exist: " + source);
				resource = new ClassResource(clazz);
				break;
			case "jar":
				File jar = new File(source);
				if (!jar.exists())
					throw new IllegalArgumentException("Invalid resource, file does not exist: " + source);
				resource = new JarResource(jar);
				break;
			case "maven":
				String[] args = source.split(":");
				if (args.length != 3)
					throw new IllegalArgumentException("Invalid resource, maven source format invalid: " + source);
				resource = new MavenResource(args[0], args[1], args[2]);
				break;
			case "url":
				URL url;
				try {
					url = new URL(source);
				} catch(MalformedURLException ex) {
					throw new IllegalArgumentException("Invalid resource, url source format invalid: " + source, ex);
				}
				resource = new UrlResource(url);
				break;
			case "instrumentation":
				// TODO: Special case here? Or exception?
				break;
		}
		if (resource != null) {
			JsonValue value = jresource.get("skipped");
			if (value != null) {
				List<String> skipped = new ArrayList<>();
				value.asArray().forEach(val -> skipped.add(val.asString()));
				resource.setSkippedPrefixes(skipped);
			}
		}
		return resource;
	}
}