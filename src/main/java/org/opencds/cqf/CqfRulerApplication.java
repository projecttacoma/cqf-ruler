package org.opencds.cqf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"org.opencds.cqf.dstu3"})
public class CqfRulerApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(CqfRulerApplication.class, args);
    }
}
