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

import edu.utah.bmi.nlp.core.Rule;
import edu.utah.bmi.nlp.core.Span;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a class extended from FastRule, which apply the full rule as a match (does not consider group capturing)
 *
 * @author Jianlin Shi
 */
@SuppressWarnings("rawtypes")
public class FastRuleWOG extends FastRule {
//    fields are defined in abstract class

    public FastRuleWOG() {
    }

    public FastRuleWOG(String ruleStr) {
        initiate(ruleStr, "\t", false);
    }

    public FastRuleWOG(String ruleStr, boolean caseSensitive) {
        initiate(ruleStr, "\t", caseSensitive);
    }

    public FastRuleWOG(HashMap<Integer, Rule> ruleStore) {
        initiate(ruleStore);
    }

    /**
     * @param contextTokens *
     */
    public HashMap<String, ArrayList<Span>> processTokens(ArrayList<String> contextTokens) {
        // use the first "startposition" to remember the original start matching
        // position.
        // use the 2nd one to remember the start position in which recursion.
        HashMap<String, ArrayList<Span>> matches = new HashMap<String, ArrayList<Span>>();
        for (int i = 0; i < contextTokens.size(); i++) {
            // System.out.println(contextTokens.get(i));
            processRules(contextTokens, rulesMap, i, i, matches);
        }
        removePseudoMatches(matches);
        return matches;
    }

    /**
     * @param contextTokens
     * @param rule
     * @param matchBegin
     * @param startPosition
     * @param matches       K: determinant
     *                      V: position of the last token that matches the rule in the input
     *                      ArrayList
     */
    protected void processRules(ArrayList<String> contextTokens, HashMap rule, int matchBegin, int startPosition,
                                HashMap<String, ArrayList<Span>> matches) {
        // when reach the end of the tunedcontext, end the iteration
        if (startPosition < contextTokens.size()) {
            // start processing the tunedcontext tokens
            String thisToken = contextTokens.get(startPosition);
//			System.out.println("thisToken-"+thisToken);
            if (rule.containsKey("\\w+")) {
                processRules(contextTokens, (HashMap) rule.get("\\w+"), matchBegin, startPosition + 1, matches);
            }
            // if the end of a rule is met
            if (rule.containsKey(END)) {
                addDeterminants(rule, matches, matchBegin, startPosition);
            }
            // if the current token match the element of a rule
            if (rule.containsKey(thisToken)) {
                processRules(contextTokens, (HashMap) rule.get(thisToken), matchBegin, startPosition + 1, matches);
            }
            if (rule.containsKey("\\d+") && StringUtils.isNumeric(thisToken)) {
                processRules(contextTokens, (HashMap) rule.get("\\d+"), matchBegin, startPosition + 1, matches);
            }
        } else if (startPosition == contextTokens.size() && rule.containsKey(END)) {
            addDeterminants(rule, matches, matchBegin, startPosition);
        }
    }


    public HashMap<String, ArrayList<Span>> processSpans(ArrayList<Span> contextTokens) {
        // use the first "startposition" to remember the original start matching
        // position.
        // use the 2nd one to remember the start position in which recursion.
        HashMap<String, ArrayList<Span>> matches = new HashMap<String, ArrayList<Span>>();
        for (int i = 0; i < contextTokens.size(); i++) {
            // System.out.println(contextTokens.get(i));
            processSpans(contextTokens, rulesMap, i, i, matches);
        }
        removePseudoMatches(matches);
        return matches;
    }

    /**
     * @param contextTokens
     * @param rule
     * @param matchBegin
     * @param currentPosition
     * @param matches         K: determinant
     *                        V: position of the last token that matches the rule in the input
     *                        ArrayList
     */
    protected void processSpans(ArrayList<Span> contextTokens, HashMap rule, int matchBegin, int currentPosition,
                                HashMap<String, ArrayList<Span>> matches) {
        // when reach the end of the tunedcontext, end the iteration
        if (currentPosition < contextTokens.size()) {
            // start processing the tunedcontext tokens
            String thisToken = contextTokens.get(currentPosition).text;
//			System.out.println("thisToken-"+thisToken);
            if (rule.containsKey("\\w+")) {
                processSpans(contextTokens, (HashMap) rule.get("\\w+"), matchBegin, currentPosition + 1, matches);
            }
            // if the end of a rule is met
            if (rule.containsKey(END)) {
//                convert to absolute offset
                addDeterminants(rule, matches, contextTokens.get(matchBegin).begin, contextTokens.get(currentPosition - 1).end);
            }
            // if the current token match the element of a rule
            if (rule.containsKey(thisToken)) {
                processSpans(contextTokens, (HashMap) rule.get(thisToken), matchBegin, currentPosition + 1, matches);
            }
            if (rule.containsKey("\\d+") && NumberUtils.isNumber(thisToken)) {
                processSpans(contextTokens, (HashMap) rule.get("\\d+"), matchBegin, currentPosition + 1, matches);
            }
        } else if (currentPosition == contextTokens.size() && rule.containsKey(END)) {
            //                convert to absolute offset
            addDeterminants(rule, matches, contextTokens.get(matchBegin).begin, contextTokens.get(currentPosition - 1).end);
        }
    }


    /**
     * if reaches the end of one or more ruleStore, add all corresponding
     * determinants into the results
     * <p/>
     * The priority of multiple applicable ruleStore can be modified. This version
     * uses the following three ruleStore:
     * 1. if determinant spans overlap, choose the determinant with the widest
     * span
     * 2. else if prefer right determinant, choose the determinant with the
     * largest end.
     * 3. else if prefer left determinant, choose the determinant with the
     * smallest begin.
     *
     * @param rule
     * @param matches
     * @param matchBegin
     * @param matchEnd
     */
    @SuppressWarnings("unchecked")
    protected void addDeterminants(HashMap rule, HashMap<String, ArrayList<Span>> matches, int matchBegin, int matchEnd) {
        HashMap<String, Integer> deterRule = (HashMap<String, Integer>) rule.get(END);
        Span currentSpan;
        ArrayList<Span> currentSpanList;
        for (Object key : deterRule.keySet()) {
            // matches.put(Determinants.valueOf(key.toString()), new
            // Span(startposition, i - 1));
            // choose the largest span, may be improved later(e.g. defined order)
            // System.out.println(key.getClass());
            currentSpan = new Span(matchBegin, matchEnd);
            currentSpan.ruleId = deterRule.get(key);
            if (matches.containsKey((String) key)) {
//              because the ruleStore are all processed at the same time from the input left to the input right,
//                it becomes more efficient to compare the overlaps
                currentSpanList = matches.get((String) key);
                Span lastSpan = currentSpanList.get(currentSpanList.size() - 1);

//                  Since there is no directional preference, assume the span is not exclusive within each determinant.
                if (currentSpan.end <= lastSpan.end) {
//                      if currentSpan is within lastSpan
                    continue;
                } else if (lastSpan.end >= currentSpan.begin) {
//                      if overlap and current span is wider than last span

                    if (lastSpan.width < currentSpan.width) {
                        currentSpanList.remove(currentSpanList.size() - 1);
                    } else {
                        continue;
                    }
                }
                currentSpanList.add(currentSpan);
            } else {
                currentSpanList = new ArrayList<Span>();
                currentSpanList.add(currentSpan);
            }
            matches.put((String) key, currentSpanList);
        }
    }


}
