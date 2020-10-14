package org.orthomcl.service.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.fgputil.json.ToJson;
import org.json.JSONObject;

public class Group implements ToJson {

  private final Map<String, Gene> genes= new LinkedHashMap<>();
  private final Map<String, EcNumber> ecNumbers = new LinkedHashMap<>();
  private final Map<String, PFamDomain> pfamDomains = new LinkedHashMap<>();

  private final String name;

  public Group(String name) {
    this.name = name;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  public Collection<Gene> getGenes() {
    return genes.values();
  }

  public Gene getGene(String sourceId) {
    return genes.get(sourceId);
  }

  public void addGene(Gene gene) {
    this.genes.put(gene.getSourceId(), gene);
  }

  public Map<String, EcNumber> getEcNumbers() {
    return ecNumbers;
  }
  
  public EcNumber getEcNumber(String code) {
    return ecNumbers.get(code);
  }

  public void addEcNumber(EcNumber ecNumber) {
    this.ecNumbers.put(ecNumber.getCode(), ecNumber);
  }
  
  public Map<String, PFamDomain> getPFamDomains() {
    return pfamDomains;
  }
  
  public PFamDomain getPFamDomain(String accession) {
    return pfamDomains.get(accession);
  }
  
  public void addPFamDomain(PFamDomain pfamDomain) {
    pfamDomains.put(pfamDomain.getAccession(), pfamDomain);
  }

  @Override
  public JSONObject toJson() {
    return new JSONObject()
      .put("genes", ToJson.mapToJson(genes))
      .put("ecNumbers", ToJson.mapToJson(ecNumbers))
      .put("pfamDomains", ToJson.mapToJson(pfamDomains));
        
  }

}
