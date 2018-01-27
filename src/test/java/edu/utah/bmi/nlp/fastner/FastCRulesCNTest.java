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

package edu.utah.bmi.nlp.fastner;


import edu.utah.bmi.nlp.core.DeterminantValueSet;
import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;
import edu.utah.bmi.nlp.fastcner.FastCNER;
import edu.utah.bmi.nlp.fastner.FastRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Jianlin Shi
 * Created on 6/2/16.
 */
public class FastCRulesCNTest {


    @Test
    public void testCn() throws Exception {
        FastCNER fastCNER = new FastCNER("conf/crule_cn_test.tsv");
        String input = "无咯血咯痰症状";
        HashMap<String, ArrayList<Span>> res = fastCNER.processString(input);
        assert (res.size() == 1 && res.containsKey("VISUAL"));
        assert (res.get("VISUAL").size() == 2);
        assert (res.get("VISUAL").get(0).toString().equals("(Rule2: 1-3):咯血"));
        assert (res.get("VISUAL").get(1).toString().equals("(Rule2: 3-5):咯痰"));
    }


    @Test
    public void testSquareBracket() {
        HashMap<Integer, Rule> rules = new HashMap<>();
        rules.put(1, new Rule(1, "胸[痛|闷]", "R1", 1.5, DeterminantValueSet.Determinants.ACTUAL));
        FastCNER fastCNER = new FastCNER(rules);
        FastRule.logger.setLevel(Level.FINER);
        fastCNER.fastRule.printRulesMap();
        HashMap ruleMap = fastCNER.fastRule.rulesMap;
        assert (ruleMap.size() == 1 && ruleMap.containsKey('胸'));
        ruleMap = (HashMap) ruleMap.get('胸');
        assert (ruleMap.size() == 2 && ruleMap.containsKey('痛') && ruleMap.containsKey('闷'));
    }

    @Test
    public void testPseudo() {
        HashMap<Integer, Rule> rules = new HashMap<>();
        rules.put(1, new Rule(1, "无心前区不适", "R1", 1.5, DeterminantValueSet.Determinants.PSEUDO));
        rules.put(2, new Rule(2, "心前区不适", "R1", 1, DeterminantValueSet.Determinants.ACTUAL));
        rules.put(2, new Rule(2, "胸痛", "R1", 1, DeterminantValueSet.Determinants.ACTUAL));
        rules.put(3, new Rule(3, "胸闷", "R2", 1, DeterminantValueSet.Determinants.ACTUAL));
        FastCNER fcruleSB = new FastCNER(rules);
        fcruleSB.printRulesMap();
        HashMap<String, ArrayList<Span>> res = fcruleSB.processString("患者诉胸闷、胸痛、无心前区不适");
        assert (res.size() == 2);
        assert (res.containsKey("R1") && res.containsKey("R2"));
        assert (res.get("R1").size() == 1);
        assert (res.get("R1").get(0).toString().equals("(Rule2: 6-8):胸痛"));
        assert (res.get("R2").size() == 1);
        assert (res.get("R2").get(0).toString().equals("(Rule3: 3-5):胸闷"));
    }


}