package com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto;

// Clean Code - Regla 15 (inmutabilidad como preferencia de diseño):
// Al igual que UserModel, este DTO expone setters públicos que permiten modificar
// cualquier campo desde cualquier parte del código después de construirlo:
//   response.setEmail("otro@email.com"); // nadie impide esto
// Un record o @Value eliminaría los setters y haría el objeto verdaderamente inmutable.
public record UserResponse(
    String id,
    String name,
    String email,
    String role,
    String status
) {}