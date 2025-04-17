package org.orthomcl.service.core.phyletic;

import java.util.List;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.validation.ValidObjectFactory.RunnableObj;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.query.param.Param;
import org.gusdb.wdk.model.query.param.ParamHandler;
import org.gusdb.wdk.model.query.param.StringParamHandler;
import org.gusdb.wdk.model.query.spec.QueryInstanceSpec;

public class ExpressionParamHandler extends StringParamHandler {

    private static final Logger LOG = Logger.getLogger(ExpressionParamHandler.class);

    // PostgreSQL-compatible SQL (replacing MINUS with EXCEPT)
  private static final String GROUP_SQL = "(SELECT group_id FROM ( " +
    "SELECT group_id, " +
    "SUM(CAST(number_of_taxa AS INTEGER)) AS number_of_taxa, " +
    "SUM(CAST(number_of_proteins AS INTEGER)) AS number_of_proteins " +
    "FROM apidb.orthologgrouptaxon " +
      "WHERE lower(three_letter_abbrev) IN (";

    private final ExpressionParser _parser = new ExpressionParser();

    public ExpressionParamHandler() {}

    public ExpressionParamHandler(ExpressionParamHandler handler, Param param) {
	super(handler, param);
    }

  @Override
  public ParamHandler clone(Param paramToClone) {
      return new ExpressionParamHandler(this, paramToClone);
  }

  @Override
  public String toInternalValue(RunnableObj<QueryInstanceSpec> ctxVals) throws WdkModelException {
      String stableValue = ctxVals.get().get(_param.getName());
      LOG.debug("transforming phyletic param: " + stableValue);

      // parse the expression and get the tree structure
      ExpressionNode root = _parser.parse(stableValue);
      String sql = composeSql(GROUP_SQL, root);

      return sql;
  }

    private String composeSql(String coreSql, ExpressionNode node) throws WdkModelException {
	StringBuilder sql = new StringBuilder();
	if (node instanceof BooleanNode) {
	    BooleanNode booleanNode = (BooleanNode) node;
	    sql.append(composeSql(coreSql, booleanNode.getLeft()));
	    if (booleanNode.getOperator().equals("and")) {
		sql.append(" INTERSECT ");
	    }
	    else if (booleanNode.getOperator().equals("or")) {
		sql.append(" UNION ");
	    }
	    sql.append(composeSql(coreSql, booleanNode.getRight()));
	}
	else {
	    LeafNode leaf = (LeafNode) node;
	    List<String> terms = leaf.getTerms();
	    StringBuilder sqlTerms = new StringBuilder();
	    for (String term : terms) {
		String species = "'" + term + "'";
		if (sqlTerms.length() > 1)
		    sqlTerms.append(",");
		sqlTerms.append(species);
	    }

	    // PostgreSQL: Replace MINUS with EXCEPT
	    if (leaf.getCount() == 0 && leaf.getCondition().equals("=")) {
		sql.append("((SELECT DISTINCT group_id FROM apidb.orthologgrouptaxon) EXCEPT ");
	    }

	    sql.append(coreSql);
	    sql.append(sqlTerms).append(")");
	    sql.append(" GROUP BY group_id) WHERE ");

	    if (leaf.isOnSpecies()) {
		sql.append("number_of_taxa");
	    }
	    else {
		sql.append("number_of_proteins");
	    }

	    if (leaf.getCount() == 0 && leaf.getCondition().equals("=")) {
		sql.append(" >= 1))");
	    }
	    else {
		sql.append(" ").append(leaf.getCondition()).append(leaf.getCount()).append(")");
	    }
	}
	return sql.toString();
    }

}
