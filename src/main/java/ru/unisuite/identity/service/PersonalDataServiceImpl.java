package ru.unisuite.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.dto.AccessTokenDto;
import ru.unisuite.identity.dto.ContactDto;
import ru.unisuite.identity.dto.PersonalDataDto;
import ru.unisuite.identity.oauth2.Scope;
import ru.unisuite.identity.profile.Contacts;
import ru.unisuite.identity.profile.ProfileJsonNode;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class PersonalDataServiceImpl implements PersonalDataService {
    private static final Logger logger = LoggerFactory.getLogger(PersonalDataServiceImpl.class);

    private final EsiaProperties esiaProperties;

    private final ObjectMapper jsonMapper;
    private final RestTemplate restTemplate;

    private String dataCollectionsUriTemplate;
    private String dataCollectionsEmbeddedUriTemplate;

    @PostConstruct
    public void init() {
        dataCollectionsUriTemplate = esiaProperties.getEsiaBaseUrl() + "/rs/prns/{oid}/{collectionType}";
        dataCollectionsEmbeddedUriTemplate = dataCollectionsUriTemplate + "?embed=(elements)";
    }


    @Override
    public Contacts mapToContacts(List<ContactDto> contactDtos) {
        Contacts contacts = new Contacts();
        contactDtos.forEach(contactDto -> {
            String contactValue = contactDto.getValue();
            boolean verified = contactDto.getVerifiedStatus() == ContactDto.VerifiedStatus.VERIFIED;
            switch (contactDto.getType()) {
                case EML:
                    if (verified) contacts.addEmail(contactValue);
                    break;
                case MBT:
                    if (verified) contacts.addMobilePhone(contactValue);
                    break;
                case PHN:
                    contacts.addHomePhone(contactValue);
                    break;
                default:
                    logger.warn("Unknown contact type '{}'", contactDto.getType());
            }
        });
        return contacts;
    }

    @Override
    public PersonalDataDto getPersonalDataDto(long oid, AccessTokenDto accessTokenDto) throws JsonProcessingException {
        String url = esiaProperties.getEsiaBaseUrl() + "/rs/prns/" + oid;
        ResponseEntity<String> personalDataResponse = restTemplate.exchange(url, HttpMethod.GET
                , authorizationRequestEntity(accessTokenDto), String.class);
        PersonalDataDto personalData = jsonMapper.readValue(personalDataResponse.getBody(), PersonalDataDto.class);

        return personalData;
    }

    @Override
    public JsonNode getPersonalDataAsJsonNode(long oid, AccessTokenDto accessTokenDto) throws JsonProcessingException {
        String url = esiaProperties.getEsiaBaseUrl() + "/rs/prns/" + oid;
        ResponseEntity<String> personalDataResponse = restTemplate.exchange(url, HttpMethod.GET
                , authorizationRequestEntity(accessTokenDto), String.class);
        return jsonMapper.readTree(personalDataResponse.getBody());
    }


    @Override
    public <T> List<T> getCollection(long oid, AccessTokenDto accessTokenDto, PersonDataCollectionType collectionType, Class<T> resultClass) throws JsonProcessingException {
        HttpEntity<String> requestEntity = authorizationRequestEntity(accessTokenDto);

        ResponseEntity<String> collectionResponseEntity = restTemplate.exchange(dataCollectionsUriTemplate, HttpMethod.GET
                , requestEntity, String.class);
        String collectionResponseString = collectionResponseEntity.getBody(); //  {"stateFacts":["hasSize"],"size":1,"eTag":"93E882A620BEDE1884695515724C772A43278794","elements":["https://esia-portal1.test.gosuslugi.ru/rs/prns/1000299654/ctts/14434265"]}
        logger.debug("collectionResponseString: {}", collectionResponseString);

        return iteratorToStream(jsonMapper.readTree(collectionResponseString).get("elements").elements())
                .map(collectionElement -> {
                    String elementUrl = collectionElement.asText();
                    validateCollectionElementUrl(elementUrl);

                    ResponseEntity<String> elementResponseEntity = restTemplate.exchange(elementUrl, HttpMethod.GET
                            , requestEntity, String.class);
                    String elementResponseString = elementResponseEntity.getBody();
                    logger.trace(elementResponseString);

                    return mapCollectionElement(elementResponseString, resultClass);
                })
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<T> getCollectionEmbedded(long oid, AccessTokenDto accessTokenDto, PersonDataCollectionType collectionType, Class<T> resultClass) throws JsonProcessingException {
        ResponseEntity<String> collectionResponseEntity = restTemplate.exchange(dataCollectionsEmbeddedUriTemplate
                , HttpMethod.GET, authorizationRequestEntity(accessTokenDto), String.class
                , oid, collectionType.urlPart());

        String collectionResponseString = collectionResponseEntity.getBody();
        logger.debug("getCollectionEmbedded collectionResponseString: {}", collectionResponseString);

        return iteratorToStream(jsonMapper.readTree(collectionResponseString).get("elements").elements())
                .map(elementJsonNode -> mapCollectionElement(elementJsonNode, resultClass))
                .collect(Collectors.toList());
    }

    @Override
    public List<JsonNode> getCollectionEmbeddedAsJsonNodes(long oid, AccessTokenDto accessTokenDto, PersonDataCollectionType collectionType) throws JsonProcessingException {
        ResponseEntity<String> collectionResponseEntity = restTemplate.exchange(dataCollectionsEmbeddedUriTemplate
                , HttpMethod.GET, authorizationRequestEntity(accessTokenDto), String.class
                , oid, collectionType.urlPart());

        String collectionResponseString = collectionResponseEntity.getBody();
        logger.debug("getCollectionEmbedded collectionResponseString: {}", collectionResponseString);

        return iteratorToStream(jsonMapper.readTree(collectionResponseString).get("elements").elements())
                .collect(Collectors.toList());
    }

    @Override
    public ProfileJsonNode getProfileJsonNode(long oid, AccessTokenDto accessTokenDto) {
        try {
            JsonNode personalDataJsonNode = getPersonalDataAsJsonNode(oid, accessTokenDto);
            List<JsonNode> contactsJsonNode = getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.CONTACTS);
            List<JsonNode> documentsJsonNode = getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.DOCUMENTS);
            List<JsonNode> addressesJsonNode;
            if (esiaProperties.getScopes().contains(Scope.ADDRESSES)) {
                addressesJsonNode = getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.ADDRESSES);
            } else {
                addressesJsonNode = Collections.emptyList();
            }

            return new ProfileJsonNode(personalDataJsonNode, addressesJsonNode
                    , contactsJsonNode, documentsJsonNode);
        } catch (Exception e) {
            throw new EsiaAccessException("Could not get ProfileJsonNode {oid=" + oid + '}', e);
        }
    }

    @Override
    public PersonalDataDto extractPersonalData(ProfileJsonNode profileJsonNode) {
        try {
            return jsonMapper.treeToValue(profileJsonNode.getPersonalData(), PersonalDataDto.class);
        } catch (JsonProcessingException e) {
            throw new ProfileJsonNodeMappingException("Could not extract personal data", e, profileJsonNode);
        }
    }



    private void validateCollectionElementUrl(String url) {
        // http(s)://(xxx.)gosuslugi.ru(/xxx)
        if (!url.matches("https?://(.+?\\.)?gosuslugi\\.ru($|/.+)")) {
            throw new EsiaAccessException("Collection URL is not safe `" + url + '\'');
        }
    }

    private <T> T mapCollectionElement(String elementResponseString, Class<T> resultClass) {
        try {
            return jsonMapper.readValue(elementResponseString, resultClass);
        } catch (JsonProcessingException e) {
            throw new EsiaAccessException("Could not map to " + resultClass + " class following String: '"
                    + (elementResponseString.length() > 100 ? elementResponseString.substring(0, 99) + "…" : elementResponseString)
                    + "'", e);
        }
    }

    private <T> T mapCollectionElement(JsonNode elementJsonNode, Class<T> resultClass) {
        try {
            return jsonMapper.treeToValue(elementJsonNode, resultClass);
        } catch (JsonProcessingException e) {
            throw new EsiaAccessException("Could not map to " + resultClass + " class following JsonNode: '"
                    + elementJsonNode + '\'', e);
        }
    }

    private HttpEntity<String> authorizationRequestEntity(AccessTokenDto accessTokenDto) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.AUTHORIZATION, accessTokenDto.getTokenType() + " " + accessTokenDto.getAccessToken());
        return new HttpEntity<>(httpHeaders);
    }



    private <T> Stream<T> iteratorToStream(Iterator<T> iterator) {
        boolean parallel = false;
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), parallel);
    }
}
