package com.nahuelgg.inventory_app.inventories.utitlities;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.nahuelgg.inventory_app.inventories.dtos.SessionDTO;
import com.nahuelgg.inventory_app.inventories.dtos.SessionDTO.AccountSession;
import com.nahuelgg.inventory_app.inventories.dtos.SessionDTO.UserSession;
import com.nahuelgg.inventory_app.inventories.enums.Permissions;

import jakarta.servlet.http.HttpSession;

@Component
public class SessionHandler {
  private final RestTemplate restTemplate;

  public SessionHandler(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public SessionDTO setSession() {
    String url = "http://api_users:8082/account/session";
    ResponseEntity<HttpSession> responseEntity = restTemplate.getForEntity(url, HttpSession.class);

    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      HttpSession response = responseEntity.getBody();

      ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
      AccountSession loggedAccount = (AccountSession) response.getAttribute("loggedAccount");
      UserSession loggedUser = (UserSession) response.getAttribute("loggedUser");
      
      HttpSession session = attr.getRequest().getSession();
      session.setAttribute("loggedAccount", loggedAccount);
      session.setAttribute("loggedUser", loggedUser);
      
      return SessionDTO.builder()
        .account(loggedAccount)
        .user(loggedUser)
      .build();
    } else return null;
  }

  public boolean checkLoggedUserHasPerms(List<Permissions> perms, boolean checkIfUserIsAdmin) {
    SessionDTO session = setSession();
    if (session != null) {
      if (checkIfUserIsAdmin) return session.getUser().getIsAdmin();

      if (session.getUser().getIsAdmin()) return true;

      return perms.stream().allMatch(
        perm -> session.getUser().getInventoryPerms().stream().anyMatch(
          invPermDTO -> invPermDTO.getPermissions().stream().anyMatch(permInv -> Permissions.valueOf(permInv).equals(perm))
        )
      );
    } else return false;
  }
}
