package org.orthomcl.model.layout;

import org.gusdb.fgputil.runtime.InstanceManager;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.user.Step;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.orthomcl.service.core.layout.GeneSetLayoutManager;
import org.orthomcl.service.core.layout.GroupLayout;

public class GeneSetLayoutTest {

  private final WdkModel _wdkModel;

  public GeneSetLayoutTest() {
    String projectId = System.getProperty(Utilities.ARGUMENT_PROJECT_ID);
    _wdkModel = InstanceManager.getInstance(WdkModel.class, projectId);
  }

  @Test
  public void testLoadLayout() throws WdkModelException {
    // this step has 6 sequences from 2 groups with the same PFam domain;
    long stepId = 100069240;
    RunnableObj<Step> runnableStep = _wdkModel.getStepFactory()
        .getStepByValidId(stepId, ValidationLevel.RUNNABLE)
        .getRunnable()
        .getOrThrow(step -> new WdkModelException(
            "Step with ID " + stepId + " is not runnable. " + step.getValidationBundle().toString()));
    GeneSetLayoutManager layoutManager = new GeneSetLayoutManager(_wdkModel);
    GroupLayout layout = layoutManager.getLayout(AnswerValueFactory.makeAnswer(runnableStep), getLayoutJson().toString());
    Assert.assertEquals(6, layout.getNodes().size());
  }

  // FIXME: configure layout to fix this test!!!
  private JSONObject getLayoutJson() {
    JSONObject json = new JSONObject();
    return json;
  }
}
