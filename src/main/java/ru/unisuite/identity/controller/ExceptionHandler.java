package ru.unisuite.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.unisuite.identity.cabinet.ProfileIsNotTrustedException;
import ru.unisuite.identity.dto.PersonalDataDto;
import ru.unisuite.identity.service.ConsentDeclinedException;

import java.util.StringJoiner;

@RestControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(ProfileIsNotTrustedException.class)
    public ResponseEntity<String> handleProfileIsNotTrustedException(ProfileIsNotTrustedException e) {
        PersonalDataDto personalData = e.getPersonalData();
        boolean isVerifying = personalData.isVerifying();

        String reply = "Уважаем" + (personalData.getGender() == PersonalDataDto.Gender.M ? "ый" : "ая")
                + " " + new StringJoiner(" ").add(personalData.getFirstName()).add(personalData.getMiddleName()) + ','
                + "\nВаша учётная запись на \"Госуслугах\" не является подтверждённой."
                + "\nВход в Личный кабинет осуществляется только по подтверждённой учётной записи.\n";

        if (isVerifying) {
            reply += "\nВ настоящее время происходит процесс проверки данных на стороне \"Госуслуг\"." +
                    " Попробуйте выполнить вход позднее.";
        } else {
            reply += "\nСледуйте инструкциям на портале \"Госуслуг\" для подтверждения учётной записи.";
        }

        return replyWithText(HttpStatus.FORBIDDEN, reply);
    }


    @org.springframework.web.bind.annotation.ExceptionHandler(ConsentDeclinedException.class)
    public ResponseEntity<String> handleConsentDeclinedException(ConsentDeclinedException e) {
        return replyWithText(HttpStatus.FORBIDDEN,
                "Для входа в Личный кабинет с помощью портала \"Госуслуг\" необходимо предоставить " +
                        "университету права на чтение данных Вашего профиля");
    }


    private ResponseEntity<String> replyWithText(HttpStatus httpStatus, String text) {
        return ResponseEntity.status(httpStatus)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body(text);
    }

}
