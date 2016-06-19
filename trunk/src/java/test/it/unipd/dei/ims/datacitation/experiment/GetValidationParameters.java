/*
 * Copyright 2015 University of Padua, Italy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.unipd.dei.ims.datacitation.experiment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.basex.core.Context;
import org.basex.query.QueryException;
import org.xml.sax.SAXException;

import it.unipd.dei.ims.datacitation.basex.BaseXDB;
import it.unipd.dei.ims.datacitation.buildcitation.PathMatcher;
import it.unipd.dei.ims.datacitation.buildcitation.ReferenceBuilder;
import it.unipd.dei.ims.datacitation.citationprocessing.BuildCitationTree;
import it.unipd.dei.ims.datacitation.citationprocessing.PathProcessor;
import it.unipd.dei.ims.datacitation.config.InitDataCitation;
import it.unipd.dei.ims.datacitation.evaluation.CalculateMeasure;
import it.unipd.dei.ims.datacitation.groundtruth.GroundTruthBuilder;
import it.unipd.dei.ims.datacitation.training.TrainingSetBuilder;

/**
 * This class build the citation tree and validate the parameters of the model.
 * It prints the data to be used in a paper by reporting the measures and the
 * paramenters for each tested fold.
 * 
 * @author <a href="mailto:silvello@dei.unipd.it">Gianmaria Silvello</a>
 * @version 0.1
 * @since 0.1
 * 
 */
public class GetValidationParameters {

	private static String[] treeMode = { "exact", "bestshallow", "mixed" };
	private static String[] weightF = { "FSDN", "SDN", "FDN", "FS" };
	private static String[] scoreT = { "0.1", "0.5", "1" };

	private static int experimentSize = 36;

	public static void main(String[] args) throws XPathExpressionException, IOException, SAXException,
			ParserConfigurationException, TransformerException, QueryException, TransformerFactoryConfigurationError {

		System.out.println("validation test");

		TrainingSetBuilder tsb = new TrainingSetBuilder();

		GetValidationParameters.validate(tsb);
	}

	private static void validate(TrainingSetBuilder tsb) throws IOException, SAXException, ParserConfigurationException,
			TransformerException, XPathExpressionException, QueryException, TransformerFactoryConfigurationError {

		// load the config properties
		InitDataCitation prop = new InitDataCitation();

		prop.loadProperties();

		// the file where the results will be stored.
		File resultFile = new File(prop.getProperty("datacitation.path.resultDir").concat(File.separator)
				.concat("validationParameters.csv"));

		if (resultFile.exists()) {
			// delete the file if it exists
			FileUtils.forceDelete(resultFile);
			// create the new file
			FileUtils.touch(resultFile);
		} else {
			// create the new file
			FileUtils.touch(resultFile);
		}

		FileWriter writer = new FileWriter(resultFile);

		String xpathCitationPath = prop.getProperty("datacitation.path.groundtruth").concat("_XPath");

		int folds = Integer.valueOf(prop.getProperty("datacitation.training.folds"));

		int size = Integer.valueOf(prop.getProperty("datacitation.trainingset.size"));

		if (size != -1 && size < folds) {
			// leave one out method
		} else {

			int trainingSize = tsb.getReferences().length;

			int d;
			// k-fold validation with all files
			if (size == -1) {
				// how many file for each fold; ignore additional files
				d = trainingSize / folds;
				size = trainingSize;
			} else {
				// k-fold validation with "size" files
				d = size / folds;
			}

			ArrayList<Double> tmpP = new ArrayList<Double>(d);
			ArrayList<Double> tmpR = new ArrayList<Double>(d);
			ArrayList<Double> tmpF = new ArrayList<Double>(d);

			int[][] sets = new int[folds][d];

			RandomDataGenerator rdg = new RandomDataGenerator();

			// randomly choose fileSamples files from the input dir
			int[] n = rdg.nextPermutation(trainingSize, trainingSize);

			int j = 0;
			int fold = 0;
			// build the sets n-1 training and 1 validation
			while (fold < folds) {
				for (int i = 0; i < d; i++) {
					sets[fold][i] = n[j];
					j++;
				}

				fold++;
			}

			// the validation array, all the folds act as validation set one
			// time
			int[] validation = new int[folds];

			// populate the validation array with the index of each fold
			for (int i = 0; i < folds; i++) {
				validation[i] = i;
			}

			// load the ground truth
			GroundTruthBuilder gtb = new GroundTruthBuilder();
			HashMap<String, String> gt = gtb.readGroundTruth(false);

			HashMap<String, ArrayList<Double>> measuresP = new HashMap<String, ArrayList<Double>>(experimentSize);
			HashMap<String, ArrayList<Double>> measuresR = new HashMap<String, ArrayList<Double>>(experimentSize);
			HashMap<String, ArrayList<Double>> measuresF = new HashMap<String, ArrayList<Double>>(experimentSize);

			for (int i = 0; i < treeMode.length; i++) {
				System.out.println("Training tree with mode: " + treeMode[i]);

				prop.setProperty("datacitation.citationtree.build-method", treeMode[i]);
				prop.setProperty("datacitation.citationtree.file",
						"resources/citationTree" + "-" + treeMode[i] + ".xml");
				// save the new properties
				prop.saveProperties();

				String dbName = prop.getProperty("basex.dbname");

				// the validation index
				int val = 0;

				// repeat the training and validation for each fold acting as
				// validation
				for (int vFold = 0; vFold < folds; vFold++) {
					for (int f = 0; f < folds; f++) {
						if (f != val) {
							// use the training set to build the tree
							/*
							 * TRAINING PHASE
							 */
							for (int v = 0; v < d; v++) {

								int entry = sets[f][v];

								BaseXDB db = new BaseXDB(dbName, tsb.getTrainingFile()[entry]);
								// // create the context
								Context ctx = db.getContext();

								byte[] encoded = Files.readAllBytes(Paths.get(tsb.getReferences()[entry]));

								BuildCitationTree.parseCitation(new String(encoded, "UTF-8"), ctx);
								ctx.closeDB();
								ctx.close();
							}
						}
					} // end of training phase for this folds configuration

					// the current value of val indicates the fold used for
					// validation
					/*
					 * VALIDATION PHASE
					 */
					System.out.println("Validation fold: " + val);
					for (int wf = 0; wf < weightF.length; wf++) {
						for (int st = 0; st < scoreT.length; st++) {

							// set the current properties for validation
							prop.setProperty("datacitation.citableunit.weightingFunction", weightF[wf]);
							prop.setProperty("datacitation.citableunit.scoreThreshold", scoreT[st]);

							prop.saveProperties();

							double mValueP = 0;
							double mValueR = 0;
							double mValueF = 0;

							for (int r = 0; r < d; r++) {

								// the current id of the file
								int id = sets[val][r];

								// the current reference
								String fn = tsb.getFileNames()[id];

								// the ground truth machine-readable
								// reference
								String mrgt = gt.get(tsb.getFileNames()[id]);

								/*
								 * Determine the validation reference
								 */

								// read the content of the file containing
								// the xpath of
								// the citation
								byte[] encoded = Files.readAllBytes(
										Paths.get(xpathCitationPath.concat(File.separator).concat(fn).concat(".txt")));
								String xPathNode = new String(encoded, "UTF-8");

								PathProcessor p = new PathProcessor(xPathNode);

								PathMatcher match = new PathMatcher(p.getProcessedPath());

								ArrayList<String> paths = match.getCandidatePaths();

								ReferenceBuilder refB = new ReferenceBuilder(xPathNode, tsb.getTrainingFile()[id],
										paths);

								// build the reference
								refB.buildReference();

								String mr = refB.getMachineReadableReference();

								mValueP = CalculateMeasure.precision(mr, mrgt);

								mValueR = CalculateMeasure.recall(mr, mrgt);

								mValueF = CalculateMeasure.fscore(mr, mrgt);

								String key = treeMode[i] + "-" + weightF[wf] + "-" + scoreT[st];

								if (!measuresP.containsKey(key)) {
									tmpP = new ArrayList<Double>(size);
									tmpR = new ArrayList<Double>(size);
									tmpF = new ArrayList<Double>(size);

									tmpP.add(new Double(mValueP));

									// update the value for this key
									measuresP.put(key, tmpP);

									tmpR.add(new Double(mValueR));

									measuresR.put(key, tmpR);

									tmpF.add(new Double(mValueF));
									measuresF.put(key, tmpF);
								} else {

									tmpP = measuresP.get(key);
									tmpP.add(new Double(mValueP));

									// update the value for this key
									measuresP.put(key, tmpP);

									tmpR = measuresR.get(key);
									tmpR.add(new Double(mValueR));

									measuresR.put(key, tmpR);

									tmpF = measuresF.get(key);
									tmpF.add(new Double(mValueF));

									measuresF.put(key, tmpF);
								}

							} // end validation for this fold

						}
					}

					// delete the training trees
					FileUtils.forceDelete(new File("resources/citationTree-" + treeMode[i] + ".xml"));

					val++;
				} // end validation loop
			} // end training

			Set<String> keysP = measuresP.keySet();

			writer.write("Tree type" + ";" + "weighting function" + ";" + "score threshold" + ";" + "avg-precision"
					+ ";" + "std-precision" + ";" + "avg-recall" + ";" + "std-recall" + ";" + "avg-fscore" + ";"
					+ "std-fscore\n");

			double[] vP = new double[size];
			double[] vR = new double[size];
			double[] vF = new double[size];

			for (String k : keysP) {
				// determine the optimization parameters
				Scanner scanner = new Scanner(k).useDelimiter("-");

				// the first token regards the tree
				String treeType = scanner.next();

				// the second token regards the weighting function
				String wFunction = scanner.next();

				// the third token regards the score threshold
				String sThreshold = scanner.next();

				scanner.close();

				for (int t = 0; t < measuresP.get(k).size(); t++) {
					vP[t] = measuresP.get(k).get(t).doubleValue();
					vR[t] = measuresR.get(k).get(t).doubleValue();
					vF[t] = measuresF.get(k).get(t).doubleValue();

				}

				double[] currVP = new double[folds];
				double[] currVR = new double[folds];
				double[] currVF = new double[folds];

				Mean avg = new Mean();

				int start = 0;
				int end = d - 1;
				for (int tt = 0; tt < folds; tt++) {

					if (end == start) {
						currVP[tt] = vP[start];
						
						currVR[tt] = vR[start];
						
						currVF[tt] = vF[start];

					} else {

						double[] tmpavg = java.util.Arrays.copyOfRange(vP, start, end);
						currVP[tt] = avg.evaluate(tmpavg);

						tmpavg = java.util.Arrays.copyOfRange(vR, start, end);
						currVR[tt] = avg.evaluate(tmpavg);

						tmpavg = java.util.Arrays.copyOfRange(vF, start, end);
						currVF[tt] = avg.evaluate(tmpavg);
					}

					start = end + 1;

					end = start + d - 1;

				}

				// Get the mean average precision calculated over the folds.
				// Average the precisions of single files in a fold and then
				// calculate the mean over all the folds
				Double avgP = avg.evaluate(currVP);
				Double avgR = avg.evaluate(currVR);
				Double avgF = avg.evaluate(currVF);

				StandardDeviation std = new StandardDeviation();
				Double stdP = std.evaluate(currVP);
				Double stdR = std.evaluate(currVR);
				Double stdF = std.evaluate(currVF);

				writer.write(treeType + ";" + wFunction + ";" + sThreshold + ";" + String.valueOf(avgP) + ";"
						+ String.valueOf(stdP) + ";" + String.valueOf(avgR) + ";" + String.valueOf(stdR) + ";"
						+ String.valueOf(avgF) + ";" + String.valueOf(stdF) + "\n");
			}

		}

		writer.flush();
		writer.close();

	}

}
