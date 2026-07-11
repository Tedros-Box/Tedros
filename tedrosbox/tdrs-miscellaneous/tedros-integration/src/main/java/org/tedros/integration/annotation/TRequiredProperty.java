package org.tedros.integration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um campo do modelo de parametros de uma {@link TServerAiFunction}
 * como obrigatorio no JSON Schema enviado ao LLM.
 * <p>
 * Equivalente server-side da annotation homonima do frontend
 * ({@code org.tedros.ai.function.TRequiredProperty}) — o backend nao pode
 * referenciar classes do modulo FE.
 *
 * @author Davis Gordon
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TRequiredProperty {

}
