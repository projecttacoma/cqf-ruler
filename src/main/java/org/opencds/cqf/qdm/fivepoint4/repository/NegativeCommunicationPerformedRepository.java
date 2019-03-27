package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.NegativeCommunicationPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface NegativeCommunicationPerformedRepository extends JpaRepository<NegativeCommunicationPerformed, String>
{
    @Nonnull
    Optional<NegativeCommunicationPerformed> findBySystemId(@Nonnull String id);
}
