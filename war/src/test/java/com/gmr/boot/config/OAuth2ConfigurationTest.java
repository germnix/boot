package com.gmr.boot.config;


import com.gmr.boot.rest.Constants;
import com.gmr.boot.test.AbstractIntegrationTest;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.ResponseBodyExtractionOptions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:beforeTestRun.sql"}),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = {"classpath:cleanup.sql"})})
//@DatabaseSetup(value = "OAuth2ConfigurationTest.xml")
public class OAuth2ConfigurationTest extends AbstractIntegrationTest {

    @Before
    public void setUp() {
        RestAssured.port = getPort();
    }


    /**
     * Access to a protected url, without having been authenticated before, is forbidden.
     */
    @Test
    public void accessForbiddenToProtectedResource() {
        given()
                .queryParam("currentUser", true)
                .get(Constants.API_PREFIX + "/user")
                .then()
                .statusCode(401);
    }


    /**
     * Requesting an access token without the client application identifying itself is forbidden.
     */
    @Test
    public void requestAccessTokenNoClientCredentialsAndFail() {
        given()
                .formParam("grant_type", "password")
                .formParam("username", "admin")
                .formParam("password", "admin")
                .post("/oauth/token")
                .then()
                .statusCode(401);
    }


    /**
     * Requesting an access token with the wrong user credentials is forbidden.
     */
    @Test
    public void requestAccessTokenWrongPasswordAndFail() {
        String response = given()
                .header("Authorization", "Basic Ym9vdF93ZWJhcHA6NTlkMTRmMDEtMzhkYS00MDFjLTgwMTQtYjZjMDM1NjI3MWM4")
                .formParam("grant_type", "password")
                .formParam("username", "admin")
                .formParam("password", "wrongPassword")
                .post("/oauth/token")
                .then()
                .statusCode(400)
                .extract().body().asString();

        System.out.println(response);
    }


    /**
     * Requesting an access token with the right user credentials and client credentials
     * is successful.
     */
    @Test
    public void requestAccessTokenSuccessful() {
        ResponseBodyExtractionOptions body = given()
                .header("Authorization", "Basic Ym9vdF93ZWJhcHA6NTlkMTRmMDEtMzhkYS00MDFjLTgwMTQtYjZjMDM1NjI3MWM4")
                .formParam("grant_type", "password")
                .formParam("username", "admin")
                .formParam("password", "admin")
                .post("/oauth/token")
                .then()
                .statusCode(200)
                .extract().body();

        assertNotNull(body.jsonPath().get("access_token"));
        assertNotNull(body.jsonPath().get("refresh_token"));
        assertEquals("bearer", body.jsonPath().get("token_type"));
        assertEquals("read write", body.jsonPath().get("scope"));
    }


    /**
     * Using an access token to request a protected resource is successful.
     */
    @Test
    public void accessTokenCanAccessProtectedResource() {
        String accessToken = authenticate();

        ResponseBodyExtractionOptions body = given()
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("currentUser", true)
                .get(Constants.API_PREFIX + "/user")
                .then()
                .statusCode(200)
                .extract().body();

        System.out.println(body.asString());
        assertEquals("admin", body.jsonPath().get("principal.username"));
    }


    /**
     * After a change password operation, requesting a new access token with the new password
     * should be successful.
     */
    @Test
    public void requestAccessTokenAfterPwdChangeSuccessful() {
        String newPassword = "newPassword";
        String accessToken = authenticate();

        // Change password
        given()
                .formParam("oldPassword", "password")
                .formParam("newPassword", newPassword)
                .header("Authorization", "Bearer " + accessToken)
                .post(Constants.API_PREFIX + "/user/password")
                .then()
                .statusCode(200);

        // Test that new password works
        given()
                .header("Authorization", "Basic Ym9vdF93ZWJhcHA6NTlkMTRmMDEtMzhkYS00MDFjLTgwMTQtYjZjMDM1NjI3MWM4")
                .formParam("grant_type", "password")
                .formParam("username", "admin")
                .formParam("password", newPassword)
                .post("/oauth/token")
                .then()
                .statusCode(200);
    }



    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------- Helper methods -------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    private String authenticate() {
        ResponseBodyExtractionOptions body = given()
                .header("Authorization", "Basic Ym9vdF93ZWJhcHA6NTlkMTRmMDEtMzhkYS00MDFjLTgwMTQtYjZjMDM1NjI3MWM4")
                .formParam("grant_type", "password")
                .formParam("username", "admin")
                .formParam("password", "admin")
                .post("/oauth/token")
                .then()
                .statusCode(200)
                .extract().body();

        return body.jsonPath().get("access_token");
    }

}