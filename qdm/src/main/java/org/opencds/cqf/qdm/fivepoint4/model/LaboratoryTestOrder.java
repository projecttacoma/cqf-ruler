package org.opencds.cqf.qdm.fivepoint4.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class LaboratoryTestOrder extends BaseType implements Serializable
{
    private String authorDatetime;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "reason_code")),
            @AttributeOverride(name = "display", column = @Column(name = "reason_display")),
            @AttributeOverride(name = "system", column = @Column(name = "reason_system")),
            @AttributeOverride(name = "version", column = @Column(name = "reason_version"))
    })
    private Code reason;

    @AttributeOverrides({
            @AttributeOverride(name = "code", column = @Column(name = "negation_rationale_code")),
            @AttributeOverride(name = "display", column = @Column(name = "negation_rationale_display")),
            @AttributeOverride(name = "system", column = @Column(name = "negation_rationale_system")),
            @AttributeOverride(name = "version", column = @Column(name = "negation_rationale_version"))
    })
    private Code negationRationale;

    @Override
    public void copy(BaseType other)
    {
        if (other instanceof LaboratoryTestOrder)
        {
            LaboratoryTestOrder laboratoryTestOrder = (LaboratoryTestOrder) other;
            super.copy(laboratoryTestOrder);
            setAuthorDatetime(laboratoryTestOrder.getAuthorDatetime());
            setReason(laboratoryTestOrder.getReason());
            setNegationRationale(laboratoryTestOrder.getNegationRationale());
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Cannot copy QDM types %s and %s", this.getClass().getName(), other.getClass().getName())
            );
        }
    }
}
