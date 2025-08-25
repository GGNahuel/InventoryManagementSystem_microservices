package com.nahuelgg.inventory_app.inventories.exceptions;

import java.util.Map;

import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

@Component
public class _ExceptionHandler extends DataFetcherExceptionResolverAdapter {
  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    String message = "Ocurrió un error inesperado: " + ex.getMessage();
    Map<String, Object> extensions = Map.of("classification", "InternalServerError");

    if (ex instanceof InternalRequestException) {
      message = ex.getMessage();
      extensions = Map.of(
        "classification", "InternalRequestError",
        "internalResponse", ((InternalRequestException) ex).getInternalRequestResponse()
      );
    }

    if (ex instanceof ResourceNotFoundException) {
      message = ex.getMessage();
      extensions = Map.of(
        "classification", "ResourceNotFound"
      );
    }

    if (ex instanceof EmptyFieldException) {
      message = ex.getMessage();
      extensions = Map.of(
        "classification", "NullOrEmptyField"
      );
    }

    if (ex instanceof IllegalArgumentException) {
      message = "Argumento requerido faltante o inválido. " + ex.getMessage();
      extensions = Map.of(
        "classification", "IllegalArgument"
      );
    }
    
    return GraphqlErrorBuilder.newError()
      .message(message)
      .path(env.getExecutionStepInfo().getPath())
      .location(env.getField().getSourceLocation())
      .extensions(extensions)
    .build();
  }
}
