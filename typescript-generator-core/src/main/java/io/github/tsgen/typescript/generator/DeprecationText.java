
package io.github.tsgen.typescript.generator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


@Retention(RetentionPolicy.RUNTIME)
public @interface DeprecationText {
    String value();
}
