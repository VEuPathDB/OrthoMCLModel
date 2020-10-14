package org.orthomcl.model.phyletic;

import org.gusdb.wdk.model.WdkModelException;
import org.junit.Test;
import org.orthomcl.service.core.phyletic.ExpressionNode;
import org.orthomcl.service.core.phyletic.ExpressionParser;

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

    private void testExpression(String exp) throws WdkModelException {
        System.out.println("Expression: " + exp);
        ExpressionNode node = parser.parse(exp);
        System.out.println("Result: " + node);
    }
}
