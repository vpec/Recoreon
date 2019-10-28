package org.apache.lucene.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Evaluation {
	
	private static HashMap<String, List<JudgedDocument>> qrelsMap = new HashMap<String, List<JudgedDocument>>();
	
	private static HashMap<String, List<String>> resultsMap = new HashMap<String, List<String>>();

	public static void main(String[] args) {
		String usage = "java org.apache.lucene.demo.IndexFiles"
				+ " [-qrels QRELS_FILE_NAME] [-results RESULTS_FILE_NAME] [-output OUTPUT_FILE_NAME]\n\n"
				+ "This uses qrels for evaluating the results, writting the output to the output file";
		String qrelsPath = null, resultsPath = null, outputPath = null;
		for (int i = 0; i < args.length; i++) {
			if ("-qrels".equals(args[i])) {
				qrelsPath = args[i + 1];
				i++;
			} else if ("-results".equals(args[i])) {
				resultsPath = args[i + 1];
				i++;
			} else if ("-output".equals(args[i])) {
				outputPath = args[i + 1];
			}
		}

		if (qrelsPath == null || resultsPath == null || outputPath == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}

		File qrelsFile = new File(qrelsPath);
		File resultsFile = new File(resultsPath);
		try {
			BufferedReader qrelsBr = new BufferedReader(new FileReader(qrelsFile));
			BufferedReader resultsBr = new BufferedReader(new FileReader(resultsFile));

			String line;
			// Read qrels file
			while ((line = qrelsBr.readLine()) != null) {
				String[] elements = line.split("\\s+");
				if (!qrelsMap.containsKey(elements[0])) {
					qrelsMap.put(elements[0], new ArrayList<>());
				}
				qrelsMap.get(elements[0])
					.add(new JudgedDocument(elements[1], elements[2].contentEquals("1")));
			}
			
			// Read results file
			while ((line = qrelsBr.readLine()) != null) {
				String[] elements = line.split("\\s+");
				if (!resultsMap.containsKey(elements[0])) {
					resultsMap.put(elements[0], new ArrayList<>());
				}
				resultsMap.get(elements[0]).add(elements[1]);
			}

			qrelsBr.close();
			resultsBr.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
