package ru.unisuite.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
import ru.unisuite.identity.profile.Contacts;
import ru.unisuite.identity.profile.ProfileJsonNode;

import javax.annotation.PostConstruct;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class PersonalDataServiceImpl implements PersonalDataService {
    private static final Logger logger = LoggerFactory.getLogger(PersonalDataServiceImpl.class);

    private final EsiaProperties esiaProperties;

    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final RestTemplate restTemplate;

    private String dataCollectionsUriTemplate;
    private String dataCollectionsEmbeddedUriTemplate;

    @PostConstruct
    public void init() {
        dataCollectionsUriTemplate = esiaProperties.getDataCollectionsUrl() + "/{oid}/{collectionType}";
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
        String url = esiaProperties.getDataCollectionsUrl() + "/" + oid;
        ResponseEntity<String> personalDataResponse = restTemplate.exchange(url, HttpMethod.GET
                , authorizationRequestEntity(accessTokenDto), String.class);
        PersonalDataDto personalData = objectMapper.readValue(personalDataResponse.getBody(), PersonalDataDto.class);

        return personalData;
    }

    @Override
    public JsonNode getPersonalDataAsJsonNode(long oid, AccessTokenDto accessTokenDto) throws JsonProcessingException {
        String url = esiaProperties.getDataCollectionsUrl() + "/" + oid;
        ResponseEntity<String> personalDataResponse = restTemplate.exchange(url, HttpMethod.GET
                , authorizationRequestEntity(accessTokenDto), String.class);
        return objectMapper.readTree(personalDataResponse.getBody());
    }


    @Override
    public <T> List<T> getCollection(long oid, AccessTokenDto accessTokenDto, PersonDataCollectionType collectionType, Class<T> resultClass) throws JsonProcessingException {
        HttpEntity<String> requestEntity = authorizationRequestEntity(accessTokenDto);

        ResponseEntity<String> collectionResponseEntity = restTemplate.exchange(dataCollectionsUriTemplate, HttpMethod.GET
                , requestEntity, String.class);
        String collectionResponseString = collectionResponseEntity.getBody(); //  {"stateFacts":["hasSize"],"size":1,"eTag":"93E882A620BEDE1884695515724C772A43278794","elements":["https://esia-portal1.test.gosuslugi.ru/rs/prns/1000299654/ctts/14434265"]}
        logger.debug("collectionResponseString: {}", collectionResponseString);

        return iteratorToStream(objectMapper.readTree(collectionResponseString).get("elements").elements())
                .map(collectionElement -> {
                    String elementUrl = collectionElement.asText();
                    validateCollectionElementUrl(elementUrl);

                    ResponseEntity<String> elementResponseEntity = restTemplate.exchange(elementUrl, HttpMethod.GET
                            , requestEntity, String.class);
                    String elementResponseString = elementResponseEntity.getBody();
                    logger.debug(elementResponseString);

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

        return iteratorToStream(objectMapper.readTree(collectionResponseString).get("elements").elements())
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

        return iteratorToStream(objectMapper.readTree(collectionResponseString).get("elements").elements())
                .collect(Collectors.toList());
    }

    @Override
    public String getProfileXml(long oid, AccessTokenDto accessTokenDto) {
        try {
            JsonNode personalDataJsonNode = getPersonalDataAsJsonNode(oid, accessTokenDto);
            List<JsonNode> contactsJsonNode = getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.CONTACTS);
            List<JsonNode> addressesJsonNode = getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.ADDRESSES);
            List<JsonNode> documentsJsonNode = getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.DOCUMENTS);

            ProfileJsonNode profileJsonNode = new ProfileJsonNode(personalDataJsonNode, addressesJsonNode
                    , contactsJsonNode, documentsJsonNode);

//            String jsonNodexml = xmlMapper.writeValueAsString(jsonNodeProfile);
            String profileXml = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profileJsonNode);
            return profileXml;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    private void validateCollectionElementUrl(String url) {
        if (!url.matches("https?:\\/\\/(.+?\\.)?gosuslugi\\.ru($|\\/.+)")) {
            throw new RuntimeException("Collection URL is not safe `" + url + '\'');
        }
    }

    private <T> T mapCollectionElement(String elementResponseString, Class<T> resultClass) {
        try {
            return objectMapper.readValue(elementResponseString, resultClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T mapCollectionElement(JsonNode elementJsonNode, Class<T> resultClass) {
        try {
            return objectMapper.treeToValue(elementJsonNode, resultClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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
