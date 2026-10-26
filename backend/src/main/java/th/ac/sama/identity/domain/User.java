package th.ac.sama.identity.domain;

import jakarta.persistence.*;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(name = "app_users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User extends BaseEntity {
  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String fullName;

  @Column(unique = true)
  private java.util.UUID authUserId;

  @Embedded private Profile profile = new Profile("", "");
  private boolean enabled = true;

  protected User() {}

  protected User(String email, String hash, String fullName) {
    this.email = email.toLowerCase();
    this.passwordHash = hash;
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getFullName() {
    return fullName;
  }

  public java.util.UUID getAuthUserId() {
    return authUserId;
  }

  public Profile getProfile() {
    return profile;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public abstract String getRole();

  public void updateProfile(String name, String email, Profile profile) {
    this.fullName = name.trim();
    this.email = email.trim().toLowerCase();
    this.profile = profile;
  }
}
