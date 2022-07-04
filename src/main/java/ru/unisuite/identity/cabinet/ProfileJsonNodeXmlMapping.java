package ru.unisuite.identity.cabinet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import ru.unisuite.identity.profile.ProfileJsonNode;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "profile")
public class ProfileJsonNodeXmlMapping {

    public ProfileJsonNodeXmlMapping(ProfileJsonNode profileJsonNode) {
        this.personalData = profileJsonNode.getPersonalData();
        this.addresses = profileJsonNode.getAddresses();
        this.contacts = profileJsonNode.getContacts();
        this.documents = profileJsonNode.getDocuments();
    }

    private final JsonNode personalData;

    @JacksonXmlElementWrapper(localName = "addresses")
    @JacksonXmlProperty(localName = "address")
    private final List<JsonNode> addresses;

    @JacksonXmlElementWrapper(localName = "contacts")
    @JacksonXmlProperty(localName = "contact")
    private final List<JsonNode> contacts;

    @JacksonXmlElementWrapper(localName = "documents")
    @JacksonXmlProperty(localName = "document")
    private final List<JsonNode> documents;
}
