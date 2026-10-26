package th.ac.sama.identity.domain;

import jakarta.persistence.*;

@Embeddable
public class Profile {
  private String phone = "";
  private String address = "";

  protected Profile() {}

  public Profile(String phone, String address) {
    this.phone = phone;
    this.address = address;
  }

  public String getPhone() {
    return phone;
  }

  public String getAddress() {
    return address;
  }
}
