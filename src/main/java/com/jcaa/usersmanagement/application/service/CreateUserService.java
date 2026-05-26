package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.out.GetUserByEmailPort;
import com.jcaa.usersmanagement.application.port.out.SaveUserPort;
import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.application.service.mapper.UserApplicationMapper;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.util.Set;

@Log
@RequiredArgsConstructor
public final class CreateUserService implements CreateUserUseCase {

  private final SaveUserPort saveUserPort;
  private final GetUserByEmailPort getUserByEmailPort;
  private final EmailNotificationService emailNotificationService;
  private final Validator validator;
  private final UserApplicationMapper userApplicationMapper;

  @Override
  public UserModel execute(final CreateUserCommand command) {
    // Clean Code - Regla 9: se usa comentario para tapar un bloque poco expresivo.
    // La regla dice: antes de comentar, intenta mejorar nombres y extraer funciones.
    // validar campos del command
    validateCommandConstraints(command);

    log.info("Creando usuario con email=" + command.email() + ", nombre=" + command.name());

    // Clean Code - Regla 10: comentario redundante — el código siguiente ya dice lo mismo.
    // verificar si el email ya existe en la base de datos
    ensureEmailDoesNotExist(command.email());

    final UserModel userToSave = buildUserModel(command);

    // Clean Code - Regla 10: comentario que explica lo obvio — no aporta valor.
    // guardar el usuario en la base de datos
    final UserModel savedUser = saveUserPort.save(userToSave);

    // Clean Code - Regla 10: otro comentario redundante.
    // enviar notificacion de bienvenida al usuario creado
    emailNotificationService.notifyUserCreated(savedUser, command.password());

    // retornar el usuario guardado
    return savedUser;
  }

  private void validateCommandConstraints(final CreateUserCommand command) {
    final Set<ConstraintViolation<CreateUserCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private void ensureEmailDoesNotExist(final String emailString) {
    final UserEmail email = new UserEmail(emailString);
    if (getUserByEmailPort.getByEmail(email).isPresent()) {
      throw UserAlreadyExistsException.becauseEmailAlreadyExists(email.value());
    }
  }

  private UserModel buildUserModel(final CreateUserCommand command) {
    return userApplicationMapper.toModel(command);
  }
}