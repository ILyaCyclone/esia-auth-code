package ru.unisuite.identity.service;

import lombok.Data;
import ru.unisuite.identity.profile.ProfileJsonNode;

@Data
public class ProfileJsonNodeMappingException extends RuntimeException {
    private final ProfileJsonNode profileJsonNode;

    public ProfileJsonNodeMappingException(String message, Throwable cause, ProfileJsonNode profileJsonNode) {
        super(message, cause);

        this.profileJsonNode = profileJsonNode;
    }
}
