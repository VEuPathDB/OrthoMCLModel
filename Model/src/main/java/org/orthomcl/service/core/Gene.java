package org.orthomcl.service.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.gusdb.fgputil.json.ToJson;
import org.json.JSONObject;

public class Gene implements ToJson {

  private final String sourceId;
  private Taxon taxon;
  private String description;
  private long length;
  private final Set<String> ecNumbers = new LinkedHashSet<>();
  private final Map<String, int[]> pfamDomains = new LinkedHashMap<>();

  public Gene(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getSourceId() {
    return sourceId;
  }

  /**
   * @return the taxon
   */
  public Taxon getTaxon() {
    return taxon;
  }

  /**
   * @param taxon
   *          the taxon to set
   */
  public void setTaxon(Taxon taxon) {
    this.taxon = taxon;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description
   *          the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the length
   */
  public long getLength() {
    return length;
  }

  /**
   * @param length
   *          the length to set
   */
  public void setLength(long length) {
    this.length = length;
  }

  /**
   * @return the ecNumbers
   */
  public Set<String> getEcNumbers() {
    return ecNumbers;
  }

  /**
   * @param ecNumbers
   *          the ecNumbers to set
   */
  public void addEcNumber(String ecNumber) {
    this.ecNumbers.add(ecNumber);
  }

  public Map<String, int[]> getPFamDomains() {
    return pfamDomains;
  }

  public void addPFamDomain(String accession, int[] location) {
    pfamDomains.put(accession, location);
  }

  @Override
  public JSONObject toJson() {
    return new JSONObject()
      .put("sourceId", sourceId)
      .put("taxon", taxon.toJson())
      .put("description", description)
      .put("length", length)
      .put("ecNumbers", ecNumbers)
      .put("pfamDomains", pfamDomains);
  }
}
