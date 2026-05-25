package com.pyrosense.shared.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate command/DTO parameters to restrict which JSON fields the client can set.
 * Fields not listed will be rejected with 400 Bad Request.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedFields {
    String[] value();
}
