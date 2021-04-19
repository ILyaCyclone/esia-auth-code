package cyclone.esia.authcode.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import cyclone.esia.authcode.dto.AccessTokenDto;
import cyclone.esia.authcode.dto.ContactDto;
import cyclone.esia.authcode.dto.PersonalDataDto;
import cyclone.esia.authcode.profile.Contacts;

import java.util.List;

public interface PersonalDataService {

    PersonalDataDto getPersonalDataDto(long oid, AccessTokenDto accessTokenDto) throws JsonProcessingException;

    <T> List<T> getCollection(long oid, AccessTokenDto accessTokenDto, PersonDataCollectionType collectionType, Class<T> resultClass) throws JsonProcessingException;

    <T> List<T> getCollectionEmbedded(long oid, AccessTokenDto accessTokenDto, PersonDataCollectionType collectionType, Class<T> resultClass) throws JsonProcessingException;

    Contacts mapToContacts(List<ContactDto> contactDtos);
}
