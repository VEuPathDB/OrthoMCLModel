package org.orthomcl.service.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.record.PrimaryKeyAliasPlugin;
import org.gusdb.wdk.model.record.RecordNotFoundException;
import org.gusdb.wdk.model.user.User;
import org.gusdb.wdk.model.query.SqlQuery;
import org.gusdb.wdk.model.query.param.Param;

public class SequenceAliasPlugin implements PrimaryKeyAliasPlugin {

  private static final String
      SEQUENCE_PK_COL_NAME = "full_id",
      ID_SEARCH_QUERY_SET = "SequenceIds",
      ID_SEARCH_QUERY_NAME = "ByIdList",
      ID_SEARCH_PARAM_NAME = "source_ids";

  private static final Pattern ID_REGEX = Pattern.compile("^[\\w\\-\\|\\.]+$");

  @Override
  public List<Map<String, Object>> getPrimaryKey(User user, Map<String, Object> inputPkValues)
      throws WdkModelException, RecordNotFoundException {

    // check for full_id PK column
    String fullId = Optional.ofNullable(inputPkValues.get(SEQUENCE_PK_COL_NAME))
        .orElseThrow(() -> new RecordNotFoundException("Primary key for sequence did not contain full_id column."))
        .toString();

    // check for SQL injection
    if (!ID_REGEX.matcher(fullId).find()) {
      throw new RecordNotFoundException("Record not found.");
    }

    WdkModel wdkModel = user.getWdkModel();
    DatabaseInstance appDb = wdkModel.getAppDb();

    // fill in requested PK as a pseudo dataset SQL to fill into the sequences ID query
    String datasetSql = "select '" + fullId + "' as " + SEQUENCE_PK_COL_NAME + appDb.getPlatform().getDummyTable();

    SqlQuery query = (SqlQuery)wdkModel.getQuerySet(ID_SEARCH_QUERY_SET).getQuery(ID_SEARCH_QUERY_NAME);
    Param param = query.getParamMap().get(ID_SEARCH_PARAM_NAME);

    String idQuerySql = param.replaceSql(query.getSql(), null, datasetSql);

    List<Map<String, Object>> foundPks = new SQLRunner(appDb.getDataSource(), idQuerySql).executeQuery(rs -> {
      List<Map<String, Object>> rows = new ArrayList<>();
      while (rs.next()) {
        rows.add(Map.of(SEQUENCE_PK_COL_NAME, rs.getString(SEQUENCE_PK_COL_NAME)));
      }
      return rows;
    });

    if (foundPks.isEmpty()) {
      throw new RecordNotFoundException("Value '" + fullId + "' does not map to any sequence.");
    }

    return foundPks;
  }

}
