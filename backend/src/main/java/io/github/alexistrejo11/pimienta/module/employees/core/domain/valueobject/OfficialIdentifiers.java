package io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject;

import io.github.alexistrejo11.pimienta.shared.util.Strings;

/**
 * Identificadores oficiales (CURP, RFC, NSS, CLABE, número interno).
 */
public record OfficialIdentifiers(
    String curp,
    String rfc,
    String nss,
    String clabe,
    String employeeNumber) {

  public OfficialIdentifiers {
    curp = Strings.nullToEmpty(curp);
    rfc = Strings.nullToEmpty(rfc);
    nss = Strings.nullToEmpty(nss);
    clabe = Strings.nullToEmpty(clabe);
    employeeNumber = Strings.nullToEmpty(employeeNumber);
  }

  public static OfficialIdentifiers empty() {
    return new OfficialIdentifiers("", "", "", "", "");
  }


  public OfficialIdentifiers setCurp(String curp) {
    return new OfficialIdentifiers(curp, this.rfc, this.nss, this.clabe, this.employeeNumber);
  }

  public OfficialIdentifiers setRfc(String rfc) {
    return new OfficialIdentifiers(this.curp, rfc, this.nss, this.clabe, this.employeeNumber);
  }

  public OfficialIdentifiers setNss(String nss) {
    return new OfficialIdentifiers(this.curp, this.rfc, nss, this.clabe, this.employeeNumber);
  }

  public OfficialIdentifiers setClabe(String clabe) {
    return new OfficialIdentifiers(this.curp, this.rfc, this.nss, clabe, this.employeeNumber);
  }

  public OfficialIdentifiers setEmployeeNumber(String employeeNumber) {
    return new OfficialIdentifiers(this.curp, this.rfc, this.nss, this.clabe, employeeNumber);
  }
}
