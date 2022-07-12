package ru.unisuite.identity.service;

/**
 * ESIA-007004: Владелец ресурса или сервис авторизации отклонил запрос
 * error=access_denied&error_description=ESIA-007004%3A+The+resource+owner+or+authorization+server+denied+the+request.
 */
public class ConsentDeclinedException extends RuntimeException {
}
