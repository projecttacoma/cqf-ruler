package org.opencds.cqf.cdshooks;

import ca.uhn.fhir.spring.boot.autoconfigure.FhirAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

@SpringBootApplication(exclude = { FhirAutoConfiguration.class })
public class CqfRulerCdsHooksApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(CqfRulerCdsHooksApplication.class, args);
    }

    @RestController
    public class CdsHooksController implements Serializable
    {
        @GetMapping("/cqf-ruler/cds-services")
        public @ResponseBody String discovery()
        {
            return "Hello!";
        }
    }
}
