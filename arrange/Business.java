package org.onosproject.arrange;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Business {
    String [] author() default "certusnet";
    String [] modify() default "1970-01-01 00:00:00";
}
