package org.apache.lucene.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.text.html.HTMLDocument.Iterator;

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
		
		// Load data from qrels and results files
		loadInputData(qrelsPath, resultsPath);

		// Writes evaluation metrics to output file
		evaluate(outputPath);

	}
	
	private static void loadInputData(String qrelsPath, String resultsPath) {
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
	
	private static void evaluate(String outputPath) {
		File outputFile = new File(outputPath);
		FileWriter outputWriter;
		try {
			outputWriter = new FileWriter(outputFile);
			PrintWriter pw = new PrintWriter(outputWriter);

			for (Entry<String, List<JudgedDocument>> entry : qrelsMap.entrySet()) {
			    //System.out.println("clave=" + entry.getKey() + ", valor=" + entry.getValue());
			    pw.println("INFORMATION_NEED " + entry.getKey());
			    pw.println("precision " + precision());
			    pw.println("recall " + recall());
			    pw.println("F1 " + f1balanced());
			    pw.println("prec@10 " + precAt10());
//			    pw.println("average_precision " + average_precision());
//			    pw.println("interpolated_recall_precision " + average_precision());
			    pw.println();
			}
			
			outputWriter.close();
						
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String precision() {
		return "";
	}
	
	private static String recall() {
		return "";
	}
	
	private static String f1balanced() {
		return "";
	}
	
	private static String precAt10() {
		return "";
	}

}
