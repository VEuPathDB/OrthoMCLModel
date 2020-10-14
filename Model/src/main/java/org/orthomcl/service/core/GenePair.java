package org.orthomcl.service.core;

import org.gusdb.fgputil.json.ToJson;
import org.json.JSONObject;

public class GenePair implements ToJson {

  private final String queryId;
  private final String subjectId;

  public GenePair(String queryId, String subjectId) {
    this.queryId = queryId;
    this.subjectId = subjectId;
  }
  
  /**
   * @return the queryId
   */
  public String getQueryId() {
    return queryId;
  }

  /**
   * @return the subjectId
   */
  public String getSubjectId() {
    return subjectId;
  }

  @Override
  public int hashCode() {
    return queryId.hashCode() ^ subjectId.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof GenePair) {
      GenePair pair = (GenePair) obj;
      return (pair.queryId.equals(queryId) && pair.subjectId.equals(subjectId)) 
             || (pair.queryId.equals(subjectId) && pair.subjectId.equals(queryId)) ;
    }
    else {
      return false;
    }
  }

  @Override
  public String toString() {
    return queryId + "|" + subjectId;
  }

  @Override
  public JSONObject toJson() {
    return new JSONObject()
      .put("queryId", queryId)
      .put("subjectId", subjectId);
  }
}
