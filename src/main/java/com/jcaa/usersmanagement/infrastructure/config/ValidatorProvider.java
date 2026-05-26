package com.jcaa.usersmanagement.infrastructure.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

@UtilityClass
public final class ValidatorProvider {

  public static Validator buildValidator() {
    try (final ValidatorFactory factory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()) {
      return factory.getValidator();
    }
  }
}