package io.quarkus.langchain4j.governance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to bind GovernanceInterceptor to CDI interceptors.
 * Applied at class level to mark the interceptor binding.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GovernanceBinding {
}