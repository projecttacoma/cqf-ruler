package org.opencds.cqf.config;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class ThemisApplicationContext extends AnnotationConfigWebApplicationContext
{
    public ThemisApplicationContext()
    {
        register(ThemisServerConfigDstu3.class, FhirServerConfigCommon.class);
    }

}