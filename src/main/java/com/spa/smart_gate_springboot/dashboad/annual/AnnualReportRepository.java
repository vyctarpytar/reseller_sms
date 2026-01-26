package com.spa.smart_gate_springboot.dashboad.annual;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnualReportRepository extends JpaRepository<AnnualReport, UUID>, JpaSpecificationExecutor<AnnualReport> {

    Optional<AnnualReport> findByYearAndQuarterAndAccountId(Integer year, Integer quarter, UUID accountId);

    @Query("SELECT DISTINCT ar.year FROM AnnualReport ar ORDER BY ar.year DESC")
    List<Integer> findDistinctYears();

}
