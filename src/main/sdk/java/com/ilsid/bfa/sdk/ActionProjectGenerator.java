package com.ilsid.bfa.sdk;

import java.io.IOException;

/**
 * Generates Action project template.
 * 
 * @author illia.sydorovych
 *
 */
public class ActionProjectGenerator {

	public static void main(String[] args) throws IOException {
		String projectPath = null;
		if (args.length > 0) {
			projectPath = args[0];
		}
		new ProjectTemplateExtractor().extract(projectPath);
	}

}
