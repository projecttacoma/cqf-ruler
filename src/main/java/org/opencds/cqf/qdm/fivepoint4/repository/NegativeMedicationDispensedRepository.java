package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeMedicationDispensed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeMedicationDispensedRepository extends JpaRepository<NegativeMedicationDispensed, String>
{
    @Nonnull
    Optional<NegativeMedicationDispensed> findBySystemId(@Nonnull String id);
}
