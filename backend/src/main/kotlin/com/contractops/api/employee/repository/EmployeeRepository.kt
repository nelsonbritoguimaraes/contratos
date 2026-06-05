package com.contractops.api.employee.repository

import com.contractops.api.employee.domain.Employee
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface EmployeeRepository : JpaRepository<Employee, UUID> {

    fun findByTenantId(tenantId: UUID): List<Employee>

    fun findByTenantIdAndStatus(tenantId: UUID, status: String): List<Employee>

    fun findByCpf(cpf: String): Employee?

    fun findByTenantIdAndMatricula(tenantId: UUID, matricula: String): Employee?

    fun findByTenantIdAndCpf(tenantId: UUID, cpf: String): Employee?
}
