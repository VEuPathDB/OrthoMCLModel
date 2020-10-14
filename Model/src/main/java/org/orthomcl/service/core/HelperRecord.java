package org.orthomcl.service.core;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.answer.AnswerValue;
import org.gusdb.wdk.model.answer.factory.AnswerValueFactory;
import org.gusdb.wdk.model.answer.spec.AnswerSpec;
import org.gusdb.wdk.model.question.Question;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.user.StepContainer;
import org.gusdb.wdk.model.user.User;

public class HelperRecord {

  private static final String HELPER_QUESTION = "HelperQuestions.ByDefault";

  public static RecordInstance get(WdkModel wdkModel) throws WdkModelException, WdkUserException {
    // load helper record into request
    Question question = wdkModel.getQuestionByFullName(HELPER_QUESTION)
        .orElseThrow(() -> new WdkModelException(HELPER_QUESTION + " does not exist in this model."));
    User user = wdkModel.getSystemUser();
    AnswerValue answerValue = AnswerValueFactory
        .makeAnswer(user, AnswerSpec
            .builder(wdkModel)
            .setQuestionFullName(question.getFullName())
            .buildRunnable(user, StepContainer.emptyContainer()));
    return answerValue.getRecordInstances()[0];
  }

}
