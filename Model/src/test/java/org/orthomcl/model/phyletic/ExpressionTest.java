package org.orthomcl.model.phyletic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.gusdb.wdk.model.WdkModelException;
import org.junit.Test;
import org.orthomcl.service.core.phyletic.BooleanNode;
import org.orthomcl.service.core.phyletic.ExpressionNode;
import org.orthomcl.service.core.phyletic.ExpressionParser;
import org.orthomcl.service.core.phyletic.LeafNode;

public class ExpressionTest {

    private ExpressionParser parser;
    
    public ExpressionTest() {
        parser = new ExpressionParser();
    }

    @Test
    public void testsFromParamHandlerTest() throws WdkModelException {
        testExpression("pviv = 2");
        testExpression("pfal >3T");
        testExpression("pber<= 5");
        testExpression("pyoe+pkno>=4T");
        testExpression("(pcha+PIRO)= 6");
        testExpression("(BASI+ tann+bbov <7T)");
        testExpression("COCC = 2 AND cmur>3T");
        testExpression("tgon<= 2T or ncan= 4");
        testExpression("cpar = 2 AND chom+ FUNG +ASCO>3T OR aory =4T");
        testExpression("(ylip +spom = 2 and psti>3T) OR ncra =4T");
        testExpression("((egos = 2 AND (cimm + cpos)>3T) Or (calb =4T And mgri>=5))");
    }

    @Test
    public void testSingleExpression() throws WdkModelException {
        testExpression("abcd = 2");
        testExpression("abor >3T");
        testExpression("ancd<= 5");
        testExpression("abcd+bcds>=4T");
        testExpression("(abcd+bcds)= 6");
        testExpression("(abcd+ bcde+cdef <7T)");
    }
    
    @Test
    public void testBooleanExpression() throws WdkModelException {
        testExpression("abcd = 2 AND abc>3T");
        testExpression("abcd<= 2T or abc= 4");
        testExpression("abcd = 2 AND abc+ basd +andor>3T OR decf =4T");
        testExpression("(aord +bacd = 2 and abc>3T) OR decf =4T");
        testExpression("((abcd = 2 AND (orst + band)>3T) Or (decf =4T And ortf>=5))");
    }
    
    @Test(expected=WdkModelException.class)
    public void testMissingCondition() throws WdkModelException {
        testExpression("abcd+cdes");
    }
    
    @Test(expected=WdkModelException.class)
    public void testWrongBoolean() throws WdkModelException {
        testExpression("(aord +bacd = 2 and abc>3T) XOR (decf =4T)");
    }
    
    @Test(expected=WdkModelException.class)
    public void testMissingCount() throws WdkModelException {
        testExpression("abcd+cdes=T");
    }
    
    @Test(expected=WdkModelException.class)
    public void testInvalidSpeciesFlag() throws WdkModelException {
        testExpression("abcd+cdes=3P");
    }

    // Regression test: 'nema' (species Nematocida ausubeli) and 'NEMA' (the
    // Nematoda clade) are distinct rows in apidb.orthologgrouptaxon that
    // differ only by case. Terms must be preserved exactly as typed, not
    // folded to a single case, or the two get conflated at query time.
    @Test
    public void testTermCaseIsPreserved() throws WdkModelException {
        ExpressionNode node = parser.parse("NEMA+nema=2");
        assertTrue(node instanceof LeafNode);
        LeafNode leaf = (LeafNode) node;
        assertEquals(Arrays.asList("NEMA", "nema"), leaf.getTerms());
    }

    // Boolean operators should still be recognized regardless of case, even
    // though terms themselves are no longer forced to lowercase.
    @Test
    public void testBooleanOperatorCaseInsensitive() throws WdkModelException {
        ExpressionNode node = parser.parse("nema=1 AND NEMA=1");
        assertTrue(node instanceof BooleanNode);
        assertEquals("and", ((BooleanNode) node).getOperator());
    }

    private void testExpression(String exp) throws WdkModelException {
        System.out.println("Expression: " + exp);
        ExpressionNode node = parser.parse(exp);
        System.out.println("Result: " + node);
    }
}
