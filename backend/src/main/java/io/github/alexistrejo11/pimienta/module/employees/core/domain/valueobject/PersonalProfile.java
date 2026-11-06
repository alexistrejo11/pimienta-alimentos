package io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject;

import io.github.alexistrejo11.pimienta.shared.util.Strings;
import java.time.LocalDate;

public record PersonalProfile(
    String firstName,
    String lastName,
    String photoUrl,
    String email,
    String phone,
    String address,
    LocalDate birthDate,
    String nationality) {

  public PersonalProfile {
    firstName = Strings.nullToEmpty(firstName);
    lastName = Strings.nullToEmpty(lastName);
    email = Strings.nullToEmpty(email);
    phone = Strings.nullToEmpty(phone);
    address = Strings.nullToEmpty(address);
    String cleanedNationality = Strings.nullToEmpty(nationality);
    nationality = cleanedNationality.isEmpty() ? "Mexicana" : cleanedNationality;
  }

  public static PersonalProfile empty() {
    return new PersonalProfile("", "", "", "", "", "", null, "Mexicana");
  }

  public PersonalProfile setFirstName(String firstName) {
    return new PersonalProfile(
        firstName, lastName, photoUrl, email, phone, address, birthDate, nationality);
  }

  public PersonalProfile setLastName(String lastName) {
    return new PersonalProfile(
        firstName, lastName, photoUrl, email, phone, address, birthDate, nationality);
  }

  public PersonalProfile setEmail(String email) {
    return new PersonalProfile(
        firstName, lastName, photoUrl, email, phone, address, birthDate, nationality);
  }

  public PersonalProfile setPhone(String phone) {
    return new PersonalProfile(
        firstName, lastName, photoUrl, email, phone, address, birthDate, nationality);
  }

  public PersonalProfile setAddress(String address) {
    return new PersonalProfile(
        firstName, lastName, photoUrl, email, phone, address, birthDate, nationality);
  }

  public PersonalProfile setBirthDate(LocalDate birthDate) {
    return new PersonalProfile(
        firstName, lastName, photoUrl, email, phone, address, birthDate, nationality);
  }

  public PersonalProfile setNationality(String nationality) {
    return new PersonalProfile(
        firstName, lastName, photoUrl, email, phone, address, birthDate, nationality);
  }

  public PersonalProfile setPhotoUrl(String photoUrl) {
    return new PersonalProfile(
        firstName, lastName, photoUrl, email, phone, address, birthDate, nationality);
  }
}
