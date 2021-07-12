package ru.unisuite.identity.profile;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import ru.unisuite.identity.dto.AddressDto;
import ru.unisuite.identity.dto.ContactDto;
import ru.unisuite.identity.dto.DocumentDto;
import ru.unisuite.identity.dto.PersonalDataDto;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "profile")
public class Profile {

    private final PersonalDataDto personalData;

    @JacksonXmlElementWrapper(localName = "addresses")
    @JacksonXmlProperty(localName = "address")
    private final List<AddressDto> addresses;

    @JacksonXmlElementWrapper(localName = "contacts")
    @JacksonXmlProperty(localName = "contact")
    private final List<ContactDto> contacts;

    @JacksonXmlElementWrapper(localName = "documents")
    @JacksonXmlProperty(localName = "document")
    private final List<DocumentDto> documents;

}
