package com.blnkfinance.blnk.types;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Identity document shape returned by {@code POST search/identities}.
 * {@code dob} and {@code created_at} are Unix-seconds NUMBERS ({@code dob}
 * can be negative for dates before 1970). Optional response convenience.
 */
public final class SearchIdentityDocument {

  private String id;
  private String identityId;
  private String identityType;
  private String organizationName;
  private String category;
  private String firstName;
  private String lastName;
  private String otherNames;
  private String gender;
  private String emailAddress;
  private String phoneNumber;
  private String nationality;
  private String street;
  private String country;
  private String state;
  private String postCode;
  private String city;
  private Long dob;
  private Long createdAt;
  private Map<String, Object> metaData;

  private SearchIdentityDocument() {}

  public static SearchIdentityDocument create() {
    return new SearchIdentityDocument();
  }

  public SearchIdentityDocument id(String id) {
    this.id = id;
    return this;
  }

  public SearchIdentityDocument identityId(String identityId) {
    this.identityId = identityId;
    return this;
  }

  public SearchIdentityDocument identityType(String identityType) {
    this.identityType = identityType;
    return this;
  }

  public SearchIdentityDocument organizationName(String organizationName) {
    this.organizationName = organizationName;
    return this;
  }

  public SearchIdentityDocument category(String category) {
    this.category = category;
    return this;
  }

  public SearchIdentityDocument firstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public SearchIdentityDocument lastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public SearchIdentityDocument otherNames(String otherNames) {
    this.otherNames = otherNames;
    return this;
  }

  public SearchIdentityDocument gender(String gender) {
    this.gender = gender;
    return this;
  }

  public SearchIdentityDocument emailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
    return this;
  }

  public SearchIdentityDocument phoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

  public SearchIdentityDocument nationality(String nationality) {
    this.nationality = nationality;
    return this;
  }

  public SearchIdentityDocument street(String street) {
    this.street = street;
    return this;
  }

  public SearchIdentityDocument country(String country) {
    this.country = country;
    return this;
  }

  public SearchIdentityDocument state(String state) {
    this.state = state;
    return this;
  }

  public SearchIdentityDocument postCode(String postCode) {
    this.postCode = postCode;
    return this;
  }

  public SearchIdentityDocument city(String city) {
    this.city = city;
    return this;
  }

  /** Typesense-indexed date of birth (Unix timestamp seconds; may be negative). */
  public SearchIdentityDocument dob(Long dob) {
    this.dob = dob;
    return this;
  }

  /** Typesense-indexed creation time (Unix timestamp seconds). */
  public SearchIdentityDocument createdAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public SearchIdentityDocument metaData(Map<String, Object> metaData) {
    this.metaData = metaData;
    return this;
  }

  public String id() {
    return id;
  }

  public String identityId() {
    return identityId;
  }

  public String identityType() {
    return identityType;
  }

  public String organizationName() {
    return organizationName;
  }

  public String category() {
    return category;
  }

  public String firstName() {
    return firstName;
  }

  public String lastName() {
    return lastName;
  }

  public String otherNames() {
    return otherNames;
  }

  public String gender() {
    return gender;
  }

  public String emailAddress() {
    return emailAddress;
  }

  public String phoneNumber() {
    return phoneNumber;
  }

  public String nationality() {
    return nationality;
  }

  public String street() {
    return street;
  }

  public String country() {
    return country;
  }

  public String state() {
    return state;
  }

  public String postCode() {
    return postCode;
  }

  public String city() {
    return city;
  }

  public Long dob() {
    return dob;
  }

  public Long createdAt() {
    return createdAt;
  }

  public Map<String, Object> metaData() {
    return metaData;
  }

  /** Tolerant parse: missing keys → null fields; extra keys ignored. */
  public static SearchIdentityDocument fromJson(JsonNode node) {
    SearchIdentityDocument document = new SearchIdentityDocument();
    if (node == null || !node.isObject()) {
      return document;
    }
    document.id = text(node.get("id"));
    document.identityId = text(node.get("identity_id"));
    document.identityType = text(node.get("identity_type"));
    document.organizationName = text(node.get("organization_name"));
    document.category = text(node.get("category"));
    document.firstName = text(node.get("first_name"));
    document.lastName = text(node.get("last_name"));
    document.otherNames = text(node.get("other_names"));
    document.gender = text(node.get("gender"));
    document.emailAddress = text(node.get("email_address"));
    document.phoneNumber = text(node.get("phone_number"));
    document.nationality = text(node.get("nationality"));
    document.street = text(node.get("street"));
    document.country = text(node.get("country"));
    document.state = text(node.get("state"));
    document.postCode = text(node.get("post_code"));
    document.city = text(node.get("city"));
    document.dob = longValue(node.get("dob"));
    document.createdAt = longValue(node.get("created_at"));
    document.metaData = map(node.get("meta_data"));
    return document;
  }

  private static String text(JsonNode node) {
    return node == null || !node.isTextual() ? null : node.asText();
  }

  private static Long longValue(JsonNode node) {
    return node == null || !node.isNumber() ? null : node.longValue();
  }

  private static Map<String, Object> map(JsonNode node) {
    if (node == null || !node.isObject()) {
      return null;
    }
    return BlnkJson.mapper().convertValue(node, new TypeReference<Map<String, Object>>() {});
  }
}
