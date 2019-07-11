package org.opencds.cqf.operation;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Input<T>
{
    private T data;
}
