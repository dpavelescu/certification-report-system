package com.certreport.repository;

import com.certreport.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
      @Query("SELECT e FROM Employee e WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Employee> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    
    List<Employee> findByDepartment(String department);
    
    @Query("SELECT DISTINCT e.department FROM Employee e ORDER BY e.department")
    List<String> findDistinctDepartments();
    
    // Efficient chunked queries for reporting
    @Query("SELECT e.id FROM Employee e ORDER BY e.department, e.lastName, e.firstName")
    List<String> findEmployeeIdsChunked(Pageable pageable);
    
    default List<String> findEmployeeIdsChunked(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("department", "lastName", "firstName"));
        return findEmployeeIdsChunked(pageable);
    }
}
