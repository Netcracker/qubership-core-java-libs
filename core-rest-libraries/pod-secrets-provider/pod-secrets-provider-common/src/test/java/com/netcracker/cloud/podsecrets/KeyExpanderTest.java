package com.netcracker.cloud.podsecrets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KeyExpanderTest {

    @ParameterizedTest
    @CsvSource({
            "db_password, db_password, DB_PASSWORD, db.password",
            "api_token,   api_token,   API_TOKEN,   api.token",
            "foo,         foo,         FOO,         foo"
    })
    void expand_producesThreeForms(String input, String lower, String upper, String dotted) {
        Set<String> result = KeyExpander.expand(input);
        assertThat(result).contains(lower, upper, dotted);
    }

    @ParameterizedTest
    @CsvSource({
            "DB_PASSWORD, db_password",
            "db.password, db_password",
            "db_password, db_password",
            "API_TOKEN,   api_token"
    })
    void normalise_roundTrips(String input, String expected) {
        assertThat(KeyExpander.normalise(input)).isEqualTo(expected);
    }
}
