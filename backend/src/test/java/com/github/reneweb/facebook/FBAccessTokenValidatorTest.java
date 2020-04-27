package com.github.reneweb.facebook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FBAccessTokenValidatorTest {

  private String appId = "myAppId";
  private String appSecret = "myAppSecret";

  @Mock private RestTemplate restTemplate;

  private FBAccessTokenValidator fbAccessTokenValidator;

  @BeforeEach
  public void setup() {
    fbAccessTokenValidator = new FBAccessTokenValidator("myAppId", "myAppSecret", restTemplate);
  }

  @Test
  public void shouldReturnFbDataIfCallSuccessful() {
    String userAccessToken = "myUserAccessToken";
    String userId = "myUserId";
    String url =
        "https://graph.facebook.com/debug_token?input_token={token}&access_token={appToken}";

    FBTokenData fbTokenData = new FBTokenData(appId, userId, false);
    when(restTemplate.getForEntity(
            url,
            FBTokenData.class,
            Map.of("token", userAccessToken, "appToken", String.format("%s|%s", appId, appSecret))))
        .thenReturn(new ResponseEntity<>(fbTokenData, HttpStatus.OK));

    FBTokenData tokenData = fbAccessTokenValidator.validate(userAccessToken);
    assertThat(tokenData.getUserId()).isEqualTo(userId);
    assertThat(tokenData.getAppId()).isEqualTo(appId);
    assertThat(tokenData.hasError()).isFalse();
  }

  @Test
  public void shouldThrowResponseExceptionIfCallUnsuccessful() {
    String userAccessToken = "myUserAccessToken";
    String userId = "myUserId";
    String url =
        "https://graph.facebook.com/debug_token?input_token={token}&access_token={appToken}";

    FBTokenData fbTokenData = new FBTokenData(appId, userId, true);
    when(restTemplate.getForEntity(
            url,
            FBTokenData.class,
            Map.of("token", userAccessToken, "appToken", String.format("%s|%s", appId, appSecret))))
        .thenReturn(new ResponseEntity<>(fbTokenData, HttpStatus.OK));

    assertThatThrownBy(() -> fbAccessTokenValidator.validate(userAccessToken))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
  }
}
