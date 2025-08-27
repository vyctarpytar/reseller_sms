package com.spa.smart_gate_springboot.dashboad.annual;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnualReportRepository extends JpaRepository<AnnualReport, UUID> {
    
    List<AnnualReport> findByYear(Integer year);
    
    List<AnnualReport> findByYearAndQuarter(Integer year, Integer quarter);
    
    Optional<AnnualReport> findByYearAndQuarterAndAccountId(Integer year, Integer quarter, UUID accountId);
    
    List<AnnualReport> findByAccountId(UUID accountId);
    
    List<AnnualReport> findByResellerId(UUID resellerId);
    
    @Query("SELECT ar FROM AnnualReport ar WHERE ar.year = :year ORDER BY ar.quarter, ar.accountName")
    List<AnnualReport> findByYearOrderedByQuarterAndAccount(@Param("year") Integer year);
    
    @Query("SELECT ar FROM AnnualReport ar WHERE ar.status = :status")
    List<AnnualReport> findByStatus(@Param("status") String status);
    
    @Query("SELECT DISTINCT ar.year FROM AnnualReport ar ORDER BY ar.year DESC")
    List<Integer> findDistinctYears();
    
    @Query("SELECT SUM(ar.quarterTotalRevenue) FROM AnnualReport ar WHERE ar.year = :year AND ar.resellerId = :resellerId")
    Double getTotalRevenueByYearAndReseller(@Param("year") Integer year, @Param("resellerId") UUID resellerId);
}
