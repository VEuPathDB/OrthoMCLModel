package org.orthomcl.service.core.layout;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.user.User;
import org.json.JSONException;
import org.orthomcl.service.core.Group;
import org.orthomcl.service.core.GroupManager;

public class GroupLayoutManager extends LayoutManager {

  private static final String LAYOUT_ATTRIBUTE = "layout";

  public GroupLayoutManager(WdkModel wdkModel) {
    super(wdkModel);
  }

  public GroupLayout getLayout(User user, String name) throws WdkModelException, WdkUserException {
    GroupManager groupManager = new GroupManager(_wdkModel);
    RecordInstance groupRecord = groupManager.getGroupRecord(user, name);

    // load layout content
    String layoutString = groupRecord.getAttributeValue(LAYOUT_ATTRIBUTE).getValue();
    if (layoutString == null)
      return null;

    // load group
    Group group = groupManager.getGroup(groupRecord);
    GroupLayout layout = new GroupLayout(group, getSize());

    try {
      loadLayout(layout, layoutString);

      return layout;
    }
    catch (JSONException ex) {
      throw new WdkModelException(ex);
    }
  }
}
