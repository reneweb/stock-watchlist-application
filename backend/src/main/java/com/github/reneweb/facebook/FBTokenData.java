package com.github.reneweb.facebook;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class FBTokenData {
  private String appId;

  private String userId;

  private boolean error;

  public FBTokenData() {}

  public FBTokenData(String appId, String userId, boolean error) {
    this.appId = appId;
    this.userId = userId;
    this.error = error;
  }

  @JsonProperty("data")
  private void unpackNameFromNestedObject(Map<String, Object> data) {
    if (data.containsKey("app_id")) {
      appId = data.get("app_id").toString();
    }
    if (data.containsKey("user_id")) {
      userId = data.get("user_id").toString();
    }

    error = data.containsKey("error");
  }

  public String getAppId() {
    return appId;
  }

  public String getUserId() {
    return userId;
  }

  public boolean hasError() {
    return error;
  }
}
