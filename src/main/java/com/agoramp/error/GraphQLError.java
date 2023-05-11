package com.agoramp.error;

import com.apollographql.apollo3.api.Error;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class GraphQLError extends java.lang.Error {
    private final List<Error> errors;
}
