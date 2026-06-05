package com.contractops.api.employee.domain

import com.contractops.api.common.domain.AuditEntity
import com.contractops.api.common.domain.TenantAware
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/**
 * Colaborador / Funcionário
 * Alinhado com SPEC v1.0 seções 8 e 25.4.
 * Entidade central para DP, Ponto, Folha, Alocação e Glosas.
 */
@Entity
@Table(name = "employees")
class Employee(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "company_id", nullable = false)
    val companyId: UUID,

    @Column(name = "branch_id")
    val branchId: UUID? = null,

    // Dados pessoais
    @Column(name = "full_name", nullable = false, length = 200)
    var fullName: String,

    @Column(name = "cpf", nullable = false, length = 14, unique = true)
    var cpf: String,

    @Column(name = "rg", length = 20)
    var rg: String? = null,

    @Column(name = "pis_nis", length = 20)
    var pisNis: String? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Column(name = "gender", length = 20)
    var gender: String? = null,

    @Column(name = "marital_status", length = 30)
    var maritalStatus: String? = null,

    // Contato e endereço
    @Column(name = "email", length = 150)
    var email: String? = null,

    @Column(name = "phone", length = 30)
    var phone: String? = null,

    @Column(name = "address", columnDefinition = "TEXT")
    var address: String? = null,

    // Dados bancários
    @Column(name = "bank_code", length = 10)
    var bankCode: String? = null,

    @Column(name = "bank_agency", length = 20)
    var bankAgency: String? = null,

    @Column(name = "bank_account", length = 30)
    var bankAccount: String? = null,

    // Dados profissionais
    @Column(name = "matricula", length = 50)
    var matricula: String? = null,

    @Column(name = "cargo", length = 100)
    var cargo: String? = null,

    @Column(name = "cbo", length = 10)
    var cbo: String? = null,

    @Column(name = "salary_base", precision = 12, scale = 2)
    var salaryBase: BigDecimal? = null,

    @Column(name = "admission_date")
    var admissionDate: LocalDate? = null,

    @Column(name = "contract_type", length = 50)
    var contractType: String? = null, // CLT, PJ, etc.

    @Column(name = "status", length = 30, nullable = false)
    var status: String = "ATIVO", // ATIVO, AFASTADO, FERIAS, DESLIGADO

    // Documentos e exames
    @Column(name = "aso_date")
    var asoDate: LocalDate? = null,

    @Column(name = "aso_valid_until")
    var asoValidUntil: LocalDate? = null,

    // Sindicato / CCT
    @Column(name = "sindicato", length = 150)
    var sindicato: String? = null,

    @Column(name = "cct_id")
    val cctId: UUID? = null

) : AuditEntity(), TenantAware {

    override fun toString(): String = "Employee(id=$id, name='$fullName', cpf='$cpf', status='$status')"
}
