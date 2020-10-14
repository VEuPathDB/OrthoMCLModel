package org.orthomcl.service.core.layout;

public enum EdgeType {

  Ortholog("O"),
  Coortholog("C"),
  Inparalog("P"),
  PeripheralCore("L"),
  PeripheralPeripheral("M"),
  Normal("N");

  public static EdgeType get(String codeOrName) {
    // check if code matches
    for (EdgeType type : EdgeType.values()) {
      if (type.code.equals(codeOrName))
        return type;
    }
    // check if name matches
    return EdgeType.valueOf(codeOrName);
  }

  private final String code;

  private EdgeType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
