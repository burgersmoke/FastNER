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

package edu.utah.bmi.nlp.fastner.uima;

import edu.utah.bmi.nlp.fastner.FastNER;
import edu.utah.bmi.nlp.fastner.TypeDefinition;
import edu.utah.bmi.nlp.type.system.Concept;
import edu.utah.bmi.nlp.type.system.Token;
import edu.utah.bmi.nlp.uima.AdaptableUIMACPERunner;
import edu.utah.bmi.nlp.uima.ae.SimpleParser_AE;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
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
 * Created by
 *
 * @author Jianlin Shi on 4/30/17.
 */
public class FastNER_AE_GeneralTest {
    private AnalysisEngine fastNER_AE, simpleParser_AE;
    private AdaptableUIMACPERunner runner;
    private JCas jCas;
    private Object[] configurationData;

    @Before
    public void setUp() {
        String typeDescriptor = "desc/type/All_Types";
        runner = new AdaptableUIMACPERunner(typeDescriptor);
        for (TypeDefinition typeDefinition : new FastNER("conf/rules.xlsx").getTypeDefinition().values()) {
            runner.addConceptType(typeDefinition.fullTypeName, typeDefinition.fullSuperTypeName);
        }
        for (TypeDefinition typeDefinition : new FastNER("conf/rules_g.tsv").getTypeDefinition().values()) {
            runner.addConceptType(typeDefinition.fullTypeName, typeDefinition.fullSuperTypeName);
        }
        runner.reInitTypeSystem("desc/type/customized");
        jCas = runner.initJCas();
//      Set up the parameters
        configurationData = new Object[]{FastNER_AE_General.PARAM_RULE_FILE_OR_STR, "conf/rules.xlsx",
                FastNER_AE_General.PARAM_SENTENCE_TYPE_NAME, "edu.utah.bmi.nlp.type.system.Sentence",
                FastNER_AE_General.PARAM_MARK_PSEUDO, true,
                FastNER_AE_General.PARAM_LOG_RULE_INFO, true};
        try {
            fastNER_AE = createEngine(FastNER_AE_General.class,
                    configurationData);
            simpleParser_AE = createEngine(SimpleParser_AE.class, new Object[]{});
        } catch (ResourceInitializationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test1() throws ResourceInitializationException,
            AnalysisEngineProcessException, CASException, IOException, InvalidXMLException {
        String text = "The patient denies any problem with visual changes or hearing changes.";
        jCas.setDocumentText(text);
        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);


        FSIndex annoIndex = jCas.getAnnotationIndex(Concept.type);
        Iterator annoIter = annoIndex.iterator();
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        while (annoIter.hasNext()) {
            concepts.add((Concept) annoIter.next());
        }
        assertTrue("Didn't get the right number of concepts", concepts.size() == 1);
        assertTrue("Didn't get the right concept: 'patient denies'", concepts.get(0).getCoveredText().equals("patient denies"));


        annoIndex = jCas.getAnnotationIndex(Token.type);
        annoIter = annoIndex.iterator();
        ArrayList<Token> tokens = new ArrayList<Token>();
        while (annoIter.hasNext()) {
            tokens.add((Token) annoIter.next());
//            System.out.println(tokens.get(tokens.size()-1).getCoveredText());
        }
        assertTrue("Didn't get the right number of concepts", tokens.size() == 11);

//        ##print the assertions below
//        for (int i=0;i<tokens.size();i++){
//            Token token=tokens.get(i);
//            int begin=token.getBegin();
//            int end=token.getEnd();
//            System.out.println("assertTrue(\"Didn't get the right token: \'"+token.getCoveredText()+"\'\", tokens.get("+i+").getBegin()=="+begin+" && tokens.get("+i+").getEnd()=="+end+" && text.substring("+begin+","+end+").equals(tokens.get("+i+").getCoveredText()));");
//        }
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


    @Test
    public void test2() throws ResourceInitializationException,
            AnalysisEngineProcessException, CASException, IOException, InvalidXMLException {

        String text = "a fever of 103.8 , tachycardia in the 130s-150s , and initial hypertensive in the 140s .";
        jCas.setDocumentText(text);
        configurationData[1] = "conf/rules_g.tsv";
        fastNER_AE = createEngine(FastNER_AE_General.class, configurationData);

        simpleParser_AE.process(jCas);
        fastNER_AE.process(jCas);

        FSIndex annoIndex = jCas.getAnnotationIndex(Concept.type);
        Iterator annoIter = annoIndex.iterator();
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        while (annoIter.hasNext()) {
            concepts.add((Concept) annoIter.next());
        }
        for (Concept concept : concepts) {
            System.out.println(concept.getBegin() + "-" + concept.getEnd() + "\t" + concept.getType().getShortName() + ": >" + concept.getCoveredText() + "<");
        }
    }
}