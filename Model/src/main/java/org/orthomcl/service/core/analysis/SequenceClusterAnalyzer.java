package org.orthomcl.service.core.analysis;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.analysis.AbstractStepAnalyzer;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.user.analysis.ExecutionStatus;
import org.gusdb.wdk.model.user.analysis.IllegalAnswerValueException;
import org.gusdb.wdk.model.user.analysis.StatusLogger;
import org.json.JSONObject;
import org.orthomcl.service.core.layout.GeneSetLayoutGenerator;
import org.orthomcl.service.core.layout.GeneSetLayoutManager;
import org.orthomcl.service.core.layout.GroupLayout;

/**
 * @author Jerric
 */
public class SequenceClusterAnalyzer extends AbstractStepAnalyzer {

  private static final int MAX_RESULT_SIZE = 500;

  @Override 
  public JSONObject getResultViewModelJson() throws WdkModelException {
    String layoutString = getPersistentCharData();
    GeneSetLayoutManager manager = new GeneSetLayoutManager(getWdkModel());
    GroupLayout layout = manager.getLayout(getAnswerValue(), layoutString);
    return layout.toJson();
  }

  @Override
  public ExecutionStatus runAnalysis(AnswerValue answerValue, StatusLogger log) throws WdkModelException {
    GeneSetLayoutGenerator generator = new GeneSetLayoutGenerator();
    GroupLayout layout = generator.generateLayout(answerValue);
    this.setPersistentCharData(layout.toString());
    return ExecutionStatus.COMPLETE;
  }

  @Override
  public void validateAnswerValue(AnswerValue answerValue)
      throws IllegalAnswerValueException, WdkModelException {
    int resultSize = answerValue.getResultSizeFactory().getResultSize();
    // can only support cluster graphs with a limited number of nodes
    if (resultSize > MAX_RESULT_SIZE) {
      throw new IllegalAnswerValueException("Only graphs of 500 nodes or fewer are supported.");
    }
  }
}
