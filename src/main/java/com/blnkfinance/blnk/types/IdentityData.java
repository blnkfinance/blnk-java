package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for {@code POST identities} and {@code PUT identities/{id}}.
 *
 * <p>All wire names are snake_case; unset fields are omitted from the
 * serialized body entirely. {@code dob} accepts either a String or a date
 * object (Date/Instant): it is stored RAW — validation runs on the raw value
 * and the identity serializer ({@code util.IdentitySerialization}) converts
 * date objects to the wire string BEFORE the request call.
 */
public final class IdentityData {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private IdentityData() {}

  public static IdentityData create() {
    return new IdentityData();
  }

  /** Optional caller-supplied id ({@code idt_} + UUID; validated when present). */
  public IdentityData identityId(String identityId) {
    fields.put("identity_id", identityId);
    return this;
  }

  /** {@code "individual"} | {@code "organization"} (enforced by the validator). */
  public IdentityData identityType(String identityType) {
    fields.put("identity_type", identityType);
    return this;
  }

  public IdentityData firstName(String firstName) {
    fields.put("first_name", firstName);
    return this;
  }

  public IdentityData lastName(String lastName) {
    fields.put("last_name", lastName);
    return this;
  }

  public IdentityData otherNames(String otherNames) {
    fields.put("other_names", otherNames);
    return this;
  }

  /** {@code "male"} | {@code "female"} | {@code "other"} (validated when present). */
  public IdentityData gender(String gender) {
    fields.put("gender", gender);
    return this;
  }

  /** ISO 8601 (RFC3339) string — passed through byte-for-byte on the wire. */
  public IdentityData dob(String dob) {
    fields.put("dob", dob);
    return this;
  }

  /** Date object — serialized to UTC ISO-8601 without fractional seconds before the call. */
  public IdentityData dob(Date dob) {
    fields.put("dob", dob);
    return this;
  }

  /** Date object — serialized to UTC ISO-8601 without fractional seconds before the call. */
  public IdentityData dob(Instant dob) {
    fields.put("dob", dob);
    return this;
  }

  public IdentityData emailAddress(String emailAddress) {
    fields.put("email_address", emailAddress);
    return this;
  }

  public IdentityData phoneNumber(String phoneNumber) {
    fields.put("phone_number", phoneNumber);
    return this;
  }

  public IdentityData nationality(String nationality) {
    fields.put("nationality", nationality);
    return this;
  }

  public IdentityData organizationName(String organizationName) {
    fields.put("organization_name", organizationName);
    return this;
  }

  public IdentityData category(String category) {
    fields.put("category", category);
    return this;
  }

  public IdentityData street(String street) {
    fields.put("street", street);
    return this;
  }

  public IdentityData country(String country) {
    fields.put("country", country);
    return this;
  }

  public IdentityData state(String state) {
    fields.put("state", state);
    return this;
  }

  public IdentityData postCode(String postCode) {
    fields.put("post_code", postCode);
    return this;
  }

  public IdentityData city(String city) {
    fields.put("city", city);
    return this;
  }

  public IdentityData metaData(Map<String, Object> metaData) {
    fields.put("meta_data", metaData);
    return this;
  }

  /** Field map as passed to validators; an unset field has no key in the map. */
  public Map<String, Object> toMap() {
    return new LinkedHashMap<>(fields);
  }

  /**
   * Wire body of the RAW fields. NOTE: {@code create}/{@code update} do NOT
   * send this directly — they send the serialized copy produced by
   * {@code IdentitySerialization.serializeIdentityData(toMap())}.
   */
  public ObjectNode toJson() {
    return BlnkJson.toObjectNode(toMap());
  }
}
