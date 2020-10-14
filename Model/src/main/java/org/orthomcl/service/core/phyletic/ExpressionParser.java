package org.orthomcl.service.core.phyletic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gusdb.wdk.model.WdkModelException;

public class ExpressionParser {

    private static final String BLOCK_PREFIX = "__block__";

    private static final String BLOCK_PATTERN = Pattern.quote(BLOCK_PREFIX)
            + "\\d+";
    private static final String BOOLEAN_PATTERN = "\\b((and)|(or))\\b";
    private static final String CONDITION_PATTERN = "=|(>(=)?)|(<(=)?)";

    private static final Map<String, String> BOOLEAN =
            new HashMap<String, String>();
    static {
        BOOLEAN.put("and", "and");
        BOOLEAN.put("&&", "and");
        BOOLEAN.put("or", "or");
        BOOLEAN.put("||", "or");
        BOOLEAN.put("not", "not");
    }

    private final Pattern booleanPattern;
    private final Pattern conditionPattern;

    public ExpressionParser() {
        booleanPattern = Pattern.compile(BOOLEAN_PATTERN);
        conditionPattern = Pattern.compile(CONDITION_PATTERN);
    }

    public ExpressionNode parse(String expression) throws WdkModelException {
        expression = expression.toLowerCase();

        Map<String, String> blocks = new HashMap<String, String>();
        return parseNode(expression, blocks);
    }

    private ExpressionNode parseNode(String expression,
            Map<String, String> blocks) throws WdkModelException {
        expression = expression.trim();

        // check if the node is a block identifier
        if (expression.matches(BLOCK_PATTERN)) {
            String block = blocks.get(expression);
            return parseNode(block, blocks);
        }

        // replace the blocks with parentheses
        expression = replaceBlocks(expression, blocks);

        // find first boolean operator
        Matcher matcher = booleanPattern.matcher(expression);
        if (matcher.find()) { // it is a boolean expression
            String left = expression.substring(0, matcher.start());
            ExpressionNode leftNode = parseNode(left, blocks);
            String right = expression.substring(matcher.end() + 1);
            ExpressionNode rightNode = parseNode(right, blocks);
            String operator = BOOLEAN.get(matcher.group());
            return new BooleanNode(leftNode, rightNode, operator);
        } else { // it is not a boolean expression
            return parseLeaf(expression, blocks);
        }
    }

    private ExpressionNode parseLeaf(String expression,
            Map<String, String> blocks) throws WdkModelException {
        expression = expression.trim();

        // check if the node is a block identifier
        if (expression.matches(BLOCK_PATTERN)) {
            String block = blocks.get(expression);
            return parseNode(block, blocks);
        }

        Matcher matcher = conditionPattern.matcher(expression);
        if (matcher.find()) {
            String terms = expression.substring(0, matcher.start());
            String counts = expression.substring(matcher.end());
            String condition = matcher.group();
            LeafNode leaf = new LeafNode();
            leaf.setCondition(condition);
            parseTerms(leaf, terms, blocks);
            parseCounts(leaf, counts);
            return leaf;
        } else {
            throw new WdkModelException("The expression is invalid: "
                    + expression);
        }
    }

    private void parseTerms(LeafNode leaf, String terms,
            Map<String, String> blocks) {
        // replace the blocks back
        for (String key : blocks.keySet()) {
            terms = terms.replace(key, blocks.get(key));
        }

        String[] array = terms.trim().split("\\s*\\+\\s*");
        // TODO - validate the terms
        leaf.setTerms(Arrays.asList(array));
    }

    private void parseCounts(LeafNode leaf, String counts)
            throws WdkModelException {
        counts = counts.trim();
        boolean onSpecies = counts.endsWith("t");
        if (onSpecies) counts = counts.substring(0, counts.length() - 1);
        try {
            int count = Integer.valueOf(counts);
            leaf.setOnSpecies(onSpecies);
            leaf.setCount(count);
        }
        catch (NumberFormatException ex) {
            throw new WdkModelException("The expression is invalid "
                    + "(expected: \\d+(T)? ): " + counts);
        }
    }

    private String replaceBlocks(String expression, Map<String, String> blocks)
            throws WdkModelException {
        int count = 0, start = 0, previous = 0;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') {
                count++;
                if (count == 1) start = i;
            } else if (ch == ')') {
                if (count <= 0)
                    throw new WdkModelException("Invalid expression. "
                            + "Unbalanced ')': " + expression);
                count--;
                if (count == 0) {
                    builder.append(expression.substring(previous, start));
                    String key = BLOCK_PREFIX + blocks.size();
                    builder.append(key);
                    String block = expression.substring(start + 1, i);
                    blocks.put(key, block);
                    previous = i + 1;
                }
            }
        }
        if (count != 0)
            throw new WdkModelException("Invalid expression. Unbalanced '(': "
                    + expression);
        if (previous != expression.length()) {
            builder.append(expression.substring(previous));
        }
        return builder.toString();
    }
}
