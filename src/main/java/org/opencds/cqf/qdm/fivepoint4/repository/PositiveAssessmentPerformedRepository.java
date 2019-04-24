package org.opencds.cqf.qdm.fivepoint4.repository;

import org.opencds.cqf.qdm.fivepoint4.model.PositiveAssessmentPerformed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.Optional;

@Repository
public interface PositiveAssessmentPerformedRepository extends JpaRepository<PositiveAssessmentPerformed, String>
{
    @Nonnull
    Optional<PositiveAssessmentPerformed> findBySystemId(@Nonnull String id);
}