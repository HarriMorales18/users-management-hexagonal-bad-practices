package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Log
@RequiredArgsConstructor
public final class EmailNotificationService {

  private static final String SUBJECT_CREATED = "Tu cuenta ha sido creada — Gestión de Usuarios";
  private static final String SUBJECT_UPDATED =
      "Tu cuenta ha sido actualizada — Gestión de Usuarios";

  private static final String TOKEN_NAME     = "name";
  private static final String TOKEN_EMAIL    = "email";
  private static final String TOKEN_PASSWORD = "password";
  private static final String TOKEN_ROLE     = "role";
  private static final String TOKEN_STATUS   = "status";

  private final EmailSenderPort emailSenderPort;

  public void notifyUserCreated(final UserModel user, final String plainPassword) {
    // Clean Code - Regla 25 (claridad sobre ingenio) y Regla 26 (evitar sobrecompactación):
    // Se comprime toda la operation en una cadena de llamadas anidadas en una sola expresión.
    // Aunque "funciona", sacrifica completamente la legibilidad.
    // Clean Code - Regla 11 (evitar duplicación): la construcción de tokens del mapa
    // es idéntica a la de notifyUserUpdated — debería centralizarse.
    final String template = loadTemplate("user-created.html");
    
    final Map<String, String> tokens = Map.of(
        TOKEN_NAME, user.getName().value(),
        TOKEN_EMAIL, user.getEmail().value(),
        TOKEN_PASSWORD, plainPassword,
        TOKEN_ROLE, user.getRole().name()
    );
    
    final String htmlBody = renderTemplate(template, tokens);
    final EmailDestinationModel destination = buildDestination(user, SUBJECT_CREATED, htmlBody);
    
    sendEmail(destination);
  }

  public void notifyUserUpdated(final UserModel user) {
    // Clean Code - Regla 11 (evitar duplicación): misma estructura que notifyUserCreated —
    // loadTemplate → renderTemplate → buildDestination → sendEmail.
    // Esta lógica de orquestación debería extraerse a un método genérico privado.
    // Clean Code - Regla 25 y 26: misma sobrecompactación que arriba.
    final String template = loadTemplate("user-updated.html");
    
    final Map<String, String> tokens = Map.of(
        TOKEN_NAME, user.getName().value(),
        TOKEN_EMAIL, user.getEmail().value(),
        TOKEN_ROLE, user.getRole().name(),
        TOKEN_STATUS, user.getStatus().name()
    );
    
    final String htmlBody = renderTemplate(template, tokens);
    final EmailDestinationModel destination = buildDestination(user, SUBJECT_UPDATED, htmlBody);
    
    sendEmail(destination);
  }

  private static EmailDestinationModel buildDestination(
      final UserModel user, final String subject, final String body) {
    return new EmailDestinationModel(
        user.getEmail().value(), user.getName().value(), subject, body);
  }

  private String loadTemplate(final String templateName) {
    final String path = "/templates/" + templateName;
    try (InputStream inputStream = openResourceStream(path)) {
      if (Objects.isNull(inputStream)) {
        throw EmailSenderException.becauseSendFailed(
            new IllegalStateException("Template not found: " + path));
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException ioException) {
      throw EmailSenderException.becauseSendFailed(ioException);
    }
  }

  InputStream openResourceStream(final String path) {
    return getClass().getResourceAsStream(path);
  }

  private static String renderTemplate(String template, final Map<String, String> values) {
    String result = template;
    for (final Map.Entry<String, String> tokenEntry : values.entrySet()) {
      final String token = "{{" + tokenEntry.getKey() + "}}";
      result = result.replace(token, tokenEntry.getValue());
    }
    return result;
  }

  private void sendEmail(final EmailDestinationModel destination) {
    emailSenderPort.send(destination);
  }
}