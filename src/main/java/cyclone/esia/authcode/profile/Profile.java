package cyclone.esia.authcode.profile;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import cyclone.esia.authcode.dto.AddressDto;
import cyclone.esia.authcode.dto.ContactDto;
import cyclone.esia.authcode.dto.DocumentDto;
import cyclone.esia.authcode.dto.PersonalDataDto;
import lombok.Data;

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
