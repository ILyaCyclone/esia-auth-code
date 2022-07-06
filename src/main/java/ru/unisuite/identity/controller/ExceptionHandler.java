package ru.unisuite.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.unisuite.identity.cabinet.ProfileIsNotTrustedException;
import ru.unisuite.identity.dto.PersonalDataDto;

import java.util.StringJoiner;

@RestControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(ProfileIsNotTrustedException.class)
    ResponseEntity<String> handleProfileIsNotTrustedException(ProfileIsNotTrustedException e) {
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
            reply += "\nСледуйте инструкциям на сайте \"Госуслуг\" для подтверждения учётной записи.";
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body(reply);
    }

}
