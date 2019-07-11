package org.opencds.cqf.operation.fhir.measure;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.opencds.cqf.operation.Operation;
import org.opencds.cqf.operation.OperationContext;

@Data
@AllArgsConstructor
public class EvaluateMeasure implements Operation
{
    private String periodStart;
    private String periodEnd;
    private String measure;
    private ReportType reportType;
    private String patient;
    private String practitioner;
    private String lastReceivedOn;

    enum ReportType {
        PATIENT, PATIENTLIST, POPULATION
    }



    @Override
    public Object evaluate(OperationContext context)
    {
        return null;
    }
}
