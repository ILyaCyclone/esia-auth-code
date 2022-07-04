package ru.unisuite.identity.profile;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class ProfileJsonNode {
    private final JsonNode personalData;

    private final List<JsonNode> addresses;
    private final List<JsonNode> contacts;
    private final List<JsonNode> documents;
}
