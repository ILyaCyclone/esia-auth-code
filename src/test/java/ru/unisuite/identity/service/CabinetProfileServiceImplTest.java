package ru.unisuite.identity.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.unisuite.identity.cabinet.CabinetProfileService;
import ru.unisuite.identity.cabinet.CabinetProfileServiceImpl;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // don't use embedded H2
@ContextConfiguration(classes = {CabinetProfileServiceImpl.class})
@ActiveProfiles("dev")
class CabinetProfileServiceImplTest {

    @Autowired
    CabinetProfileService cabinetProfileService;

    @Test
    void contextLoads() {
    }

//    @Test
//    void testIsProfileFetchRequired() {
//        long oid = -99L;
//        boolean profileFetchRequired = cabinetProfileService.isProfileFetchRequired(oid);
//
//        assertTrue(profileFetchRequired);
//    }
}