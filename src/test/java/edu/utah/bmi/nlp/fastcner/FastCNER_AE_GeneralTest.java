/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utah.bmi.nlp.fastcner;


import edu.utah.bmi.nlp.fastcner.uima.FastCNER_AE_General;
import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.type.system.ConceptBASE;
import edu.utah.bmi.nlp.type.system.Sentence;
import edu.utah.bmi.nlp.type.system.Token;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.ae.AnnotationPrinter;
import edu.utah.bmi.nlp.uima.ae.SimpleParser_AE;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.junit.Assert.assertTrue;

/**
 * This test class is used to test the correctness of FastNER's UIMA AE using UimaFit
 * <p>
 * Created by Jianlin Shi on 4/26/16.
 */
public class FastCNER_AE_GeneralTest {
	private AnalysisEngine fastCNER_AE, simpleParser_AE, annoprinter;
	private AdaptableUIMACPERunner runner;
	private JCas jCas;
	private Object[] configurationData;

	@Before
	public void setUp() {
		String typeDescriptor = "desc/type/All_Types";
		runner = new AdaptableUIMACPERunner(typeDescriptor, "target/generated-test-sources/");
		runner.addConceptTypes(new FastCNER("conf/crule_test.xlsx", true).getTypeDefinitions().values());
		runner.reInitTypeSystem("target/generated-test-sources/customized");
		jCas = runner.initJCas();
//      Set up the parameters
		configurationData = new Object[]{FastCNER_AE_General.PARAM_RULE_FILE_OR_STR, "conf/crule_test.xlsx",
				FastCNER_AE_General.PARAM_SENTENCE_TYPE_NAME, "edu.utah.bmi.nlp.type.system.Sentence",
				FastCNER_AE_General.PARAM_MARK_PSEUDO, true,
				FastCNER_AE_General.PARAM_LOG_RULE_INFO, true};
		try {
			fastCNER_AE = createEngine(FastCNER_AE_General.class,
					configurationData);
			simpleParser_AE = createEngine(SimpleParser_AE.class, new Object[]{});
			annoprinter=createEngine(AnnotationPrinter.class,new Object[]{AnnotationPrinter.PARAM_TYPE_NAME, Sentence.class.getCanonicalName()});
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test() throws ResourceInitializationException,
			AnalysisEngineProcessException, CASException, IOException, InvalidXMLException {
		String text = "The patient denies any problem with visual changes or hearing changes.";
		jCas.setDocumentText(text);
		simpleParser_AE.process(jCas);
		annoprinter.process(jCas);
		fastCNER_AE.process(jCas);

		FSIndex annoIndex = jCas.getAnnotationIndex(Concept.type);
		Iterator annoIter = annoIndex.iterator();
		ArrayList<Annotation> concepts = new ArrayList<>();
		while (annoIter.hasNext()) {
			Object anno = annoIter.next();
//            System.out.println(anno.getClass().getCanonicalName());
			concepts.add((Annotation) anno);
//            System.out.println(concepts.get(concepts.size() - 1).getCoveredText());
		}
		assertTrue("Didn't get the right number of concepts", concepts.size() == 1);
		assertTrue("Didn't get the right concept: 'hearing'", concepts.get(0).getCoveredText().equals("hearing"));
		assertTrue("Didn't get the right concept type: 'hearing'",
				concepts.get(0).getClass().getCanonicalName().equals("edu.utah.bmi.nlp.type.system.HEARING"));

		annoIndex = jCas.getAnnotationIndex(ConceptBASE.type);
		annoIter = annoIndex.iterator();
		concepts = new ArrayList<>();
		while (annoIter.hasNext()) {
			Annotation anno = (Annotation) annoIter.next();
			TOP obj = jCas.getJfsFromCaddr(anno.getAddress());
			//            System.out.println(anno.getClass().getCanonicalName());
			concepts.add(anno);
//            System.out.println(concepts.get(concepts.size() - 1).getCoveredText());
		}

		assertTrue("Didn't get the right number of concepts", concepts.size() == 2);
		assertTrue("Didn't get the right concept: 'pa'", concepts.get(0).getCoveredText().equals("pa"));
		assertTrue("Didn't get the right concept type: 'pa'",
				concepts.get(0).getClass().getCanonicalName().equals("edu.utah.bmi.nlp.type.system.PROBLEM"));

		annoIndex = jCas.getAnnotationIndex(Token.type);
		annoIter = annoIndex.iterator();
		ArrayList<Token> tokens = new ArrayList<Token>();
		while (annoIter.hasNext()) {
			tokens.add((Token) annoIter.next());
			System.out.println(tokens.get(tokens.size()-1).getCoveredText());
		}
		assertTrue("Didn't get the right number of concepts", tokens.size() == 11);
		assertTrue("Didn't get the right token: 'The'", tokens.get(0).getBegin() == 0 && tokens.get(0).getEnd() == 3 && text.substring(0, 3).equals(tokens.get(0).getCoveredText()));
		assertTrue("Didn't get the right token: 'patient'", tokens.get(1).getBegin() == 4 && tokens.get(1).getEnd() == 11 && text.substring(4, 11).equals(tokens.get(1).getCoveredText()));
		assertTrue("Didn't get the right token: 'denies'", tokens.get(2).getBegin() == 12 && tokens.get(2).getEnd() == 18 && text.substring(12, 18).equals(tokens.get(2).getCoveredText()));
		assertTrue("Didn't get the right token: 'any'", tokens.get(3).getBegin() == 19 && tokens.get(3).getEnd() == 22 && text.substring(19, 22).equals(tokens.get(3).getCoveredText()));
		assertTrue("Didn't get the right token: 'problem'", tokens.get(4).getBegin() == 23 && tokens.get(4).getEnd() == 30 && text.substring(23, 30).equals(tokens.get(4).getCoveredText()));
		assertTrue("Didn't get the right token: 'with'", tokens.get(5).getBegin() == 31 && tokens.get(5).getEnd() == 35 && text.substring(31, 35).equals(tokens.get(5).getCoveredText()));
		assertTrue("Didn't get the right token: 'visual'", tokens.get(6).getBegin() == 36 && tokens.get(6).getEnd() == 42 && text.substring(36, 42).equals(tokens.get(6).getCoveredText()));
		assertTrue("Didn't get the right token: 'changes'", tokens.get(7).getBegin() == 43 && tokens.get(7).getEnd() == 50 && text.substring(43, 50).equals(tokens.get(7).getCoveredText()));
		assertTrue("Didn't get the right token: 'or'", tokens.get(8).getBegin() == 51 && tokens.get(8).getEnd() == 53 && text.substring(51, 53).equals(tokens.get(8).getCoveredText()));
		assertTrue("Didn't get the right token: 'hearing'", tokens.get(9).getBegin() == 54 && tokens.get(9).getEnd() == 61 && text.substring(54, 61).equals(tokens.get(9).getCoveredText()));
		assertTrue("Didn't get the right token: 'changes'", tokens.get(10).getBegin() == 62 && tokens.get(10).getEnd() == 69 && text.substring(62, 69).equals(tokens.get(10).getCoveredText()));
	}


}