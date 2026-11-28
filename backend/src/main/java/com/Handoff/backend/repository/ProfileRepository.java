package com.Handoff.backend.repository;

import com.Handoff.backend.model.Profile;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA Repository for Profile entities. */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    /**
     * Checks if a profile already exists for a given student.
     *
     * @param studentId
     *            the ID of the student
     *
     * @return true if a profile exists, false otherwise
     */
    boolean existsByStudent_Id(Long studentId);

    /**
     * Checks if a profile already exists for a given student.
     *
     * @param student
     *            the student entity
     *
     * @return true if a profile exists, false otherwise
     */
    boolean existsByStudent(com.Handoff.backend.model.Student student);

    /**
     * Retrieves the profile associated with a given student.
     *
     * @param studentId
     *            the ID of the student
     *
     * @return an Optional containing the Profile if found
     */
    Optional<Profile> findByStudent_Id(Long studentId);

    /**
     * Same school, not the requester, and a case-insensitive partial match on the profile name or the student's email.
     * The student is fetched in the same query so each result's email needs no extra select.
     */
    @Query("""
            select profile from Profile profile join fetch profile.student student
            where profile.schoolDomain = :schoolDomain and student.id <> :requesterId and
            (lower(profile.name) like lower(concat('%', :query, '%'))
            or lower(student.email) like lower(concat('%', :query, '%')))
            """)
    Page<Profile> searchSameSchool(@Param("schoolDomain") String schoolDomain, @Param("requesterId") Long requesterId,
            @Param("query") String query, Pageable pageable);

    /** Admin student list: one school, optional case-insensitive match on profile name or email. */
    @Query("""
            select profile from Profile profile join fetch profile.student student
            where profile.schoolDomain = :schoolDomain and
            (:query is null or lower(profile.name) like lower(concat('%', :query, '%'))
              or lower(student.email) like lower(concat('%', :query, '%')))
            """)
    Page<Profile> findAdminStudents(@Param("schoolDomain") String schoolDomain, @Param("query") String query,
            Pageable pageable);

    @Query("select profile from Profile profile where profile.student.id = :studentId and profile.schoolDomain = :schoolDomain")
    Optional<Profile> findByStudentIdAndSchoolDomain(@Param("studentId") Long studentId,
            @Param("schoolDomain") String schoolDomain);
}
